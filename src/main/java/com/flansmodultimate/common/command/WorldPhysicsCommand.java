package com.flansmodultimate.common.command;

import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Operator controls for live, persisted dimension-specific Flan's physics. */
public final class WorldPhysicsCommand
{
    private WorldPhysicsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansphysics")
            .requires(source -> source.hasPermission(2))
            .executes(context -> report(context.getSource(), context.getSource().getLevel()))
            .then(factorCommand("gravity", true))
            .then(factorCommand("drag", false)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> factorCommand(
        String name, boolean gravity)
    {
        return Commands.literal(name)
            .executes(context -> show(context.getSource(), context.getSource().getLevel(), gravity))
            .then(Commands.argument("dimension", DimensionArgument.dimension())
                .executes(context -> show(context.getSource(), dimension(context), gravity))
                .then(Commands.argument("factor", DoubleArgumentType.doubleArg(0D, 10D))
                    .executes(context -> set(context.getSource(), dimension(context), gravity,
                        DoubleArgumentType.getDouble(context, "factor"))))
                .then(Commands.literal("reset")
                    .executes(context -> clear(context.getSource(), dimension(context), gravity))));
    }

    private static ServerLevel dimension(CommandContext<CommandSourceStack> context)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        return DimensionArgument.getDimension(context, "dimension");
    }

    private static int report(CommandSourceStack source, ServerLevel level)
    {
        source.sendSuccess(() -> Component.literal("Flan's physics in " + level.dimension().location()
            + ": gravity=" + ModCommonConfig.gravityFactor(level)
            + ", drag=" + ModCommonConfig.dragFactor(level)), false);
        return 1;
    }

    private static int show(CommandSourceStack source, ServerLevel level, boolean gravity)
    {
        double factor = gravity ? ModCommonConfig.gravityFactor(level) : ModCommonConfig.dragFactor(level);
        source.sendSuccess(() -> Component.literal((gravity ? "Gravity" : "Drag") + " in "
            + level.dimension().location() + ": " + factor), false);
        return 1;
    }

    private static int set(CommandSourceStack source, ServerLevel level, boolean gravity, double factor)
    {
        ModCommonConfig.setDimensionFactor(level.dimension().location(), gravity, factor);
        return show(source, level, gravity);
    }

    private static int clear(CommandSourceStack source, ServerLevel level, boolean gravity)
    {
        ModCommonConfig.clearDimensionFactor(level.dimension().location(), gravity);
        return show(source, level, gravity);
    }
}
