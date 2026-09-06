package com.flansmodultimate.common.command;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketParticles;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class FMParticleCommand
{
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.particle.failed"));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN = new DynamicCommandExceptionType(
        name -> Component.translatable("commands.flansmodultimate.fmparticle.unknown", name));
    /** Height above the caller used by the short form. */
    private static final double SHORT_FORM_HEIGHT = 1.0D;
    private static final int SHORT_FORM_COUNT = 1;
    private static final float DEFAULT_SCALE = 1F;
    private static final double RANGE = 512.0D;
    private static final String NAME = "name";
    private static final String POS = "pos";
    private static final String DELTA = "delta";
    private static final String SPEED = "speed";
    private static final String COUNT = "count";
    private static final String SCALE = "scale";
    private static final String VIEWERS = "viewers";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("fmparticle")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument(NAME, StringArgumentType.string())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(FlanParticles.suggestions(), builder))
                // Short form: a single particle one block above the caller
                .executes(context -> sendParticles(context, context.getSource().getPosition().add(0D, SHORT_FORM_HEIGHT, 0D),
                    Vec3.ZERO, 0.0F, SHORT_FORM_COUNT, DEFAULT_SCALE, allPlayers(context)))
                .then(Commands.argument(POS, Vec3Argument.vec3())
                    .executes(context -> sendParticles(context, Vec3Argument.getVec3(context, POS), Vec3.ZERO, 0.0F, 0, DEFAULT_SCALE, allPlayers(context)))
                    .then(Commands.argument(DELTA, Vec3Argument.vec3(false))
                        .then(Commands.argument(SPEED, FloatArgumentType.floatArg(0.0F))
                            .then(Commands.argument(COUNT, IntegerArgumentType.integer(0))
                                .executes(context -> sendParticles(context, Vec3Argument.getVec3(context, POS), Vec3Argument.getVec3(context, DELTA), FloatArgumentType.getFloat(context, SPEED), IntegerArgumentType.getInteger(context, COUNT), DEFAULT_SCALE, allPlayers(context)))
                                .then(Commands.argument(SCALE, FloatArgumentType.floatArg(0.0F))
                                    .executes(context -> sendParticles(context, Vec3Argument.getVec3(context, POS), Vec3Argument.getVec3(context, DELTA), FloatArgumentType.getFloat(context, SPEED), IntegerArgumentType.getInteger(context, COUNT), FloatArgumentType.getFloat(context, SCALE), allPlayers(context)))
                                    .then(Commands.argument(VIEWERS, EntityArgument.players())
                                        .executes(context -> sendParticles(context, Vec3Argument.getVec3(context, POS), Vec3Argument.getVec3(context, DELTA), FloatArgumentType.getFloat(context, SPEED), IntegerArgumentType.getInteger(context, COUNT), FloatArgumentType.getFloat(context, SCALE), EntityArgument.getPlayers(context, VIEWERS)))
                                    )
                                )
                                .then(Commands.argument(VIEWERS, EntityArgument.players())
                                    .executes(context -> sendParticles(context, Vec3Argument.getVec3(context, POS), Vec3Argument.getVec3(context, DELTA), FloatArgumentType.getFloat(context, SPEED), IntegerArgumentType.getInteger(context, COUNT), DEFAULT_SCALE, EntityArgument.getPlayers(context, VIEWERS)))
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    private static Collection<ServerPlayer> allPlayers(CommandContext<CommandSourceStack> context)
    {
        return context.getSource().getServer().getPlayerList().getPlayers();
    }

    private static int sendParticles(CommandContext<CommandSourceStack> context, Vec3 pos, Vec3 delta, float speed, int count, float scale, Collection<ServerPlayer> viewers) throws CommandSyntaxException
    {
        String requested = StringArgumentType.getString(context, NAME);
        // Reject unknown names here instead of letting every client silently log a warning
        String name = FlanParticles.resolve(requested).orElseThrow(() -> ERROR_UNKNOWN.create(requested));
        CommandSourceStack source = context.getSource();
        int sent = 0;

        for (ServerPlayer viewer : viewers)
        {
            if (sendParticles(source, viewer, name, pos, delta, speed, count, scale))
                sent++;
        }

        if (sent == 0)
            throw ERROR_FAILED.create();

        source.sendSuccess(() -> Component.translatable("commands.particle.success", name), true);
        return sent;
    }

    private static boolean sendParticles(CommandSourceStack source, ServerPlayer viewer, String name, Vec3 pos, Vec3 delta, float speed, int count, float scale)
    {
        if (viewer.level() != source.getLevel())
            return false;

        if (!viewer.blockPosition().closerToCenterThan(pos, RANGE))
            return false;

        PacketHandler.sendTo(new PacketParticles(name, pos, delta, speed, count, scale), viewer);
        return true;
    }
}
