package com.wolffsmod.npcs.diag;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.wolffsmod.npcs.NpcsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /flansnpcbenchmark [seconds]} and {@code /flansnpcbenchmark stop}, for operators and the server console. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = NpcsMod.MOD_ID)
public final class NpcBenchmarkCommands
{
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event)
    {
        event.getDispatcher()
            .register(Commands.literal("flansnpcbenchmark").requires(source -> source.hasPermission(2)).executes(context -> start(context, ServerBenchmark.DEFAULT_SECONDS))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(ServerBenchmark.MIN_SECONDS, ServerBenchmark.MAX_SECONDS))
                    .executes(context -> start(context, IntegerArgumentType.getInteger(context, "seconds"))))
                .then(Commands.literal("stop").executes(context ->
                {
                    if (ServerBenchmark.stop(context.getSource().getServer(), "stopped by command"))
                        return 1;
                    context.getSource().sendFailure(Component.translatable("commands.wolffsmodnpcs.benchmark.idle"));
                    return 0;
                })));
    }

    private static int start(CommandContext<CommandSourceStack> context, int seconds)
    {
        if (ServerBenchmark.start(context.getSource(), seconds))
            return 1;
        context.getSource().sendFailure(Component.translatable("commands.wolffsmodnpcs.benchmark.busy"));
        return 0;
    }
}
