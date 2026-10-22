package com.wolffsmod.npcs.diag;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Server benchmark duration, optional warm-up/label, status and stop, for operators and the console. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcBenchmarkCommands
{
    private static final String SECONDS_ARGUMENT = "seconds";
    private static final String WARM_UP_ARGUMENT = "warmup";
    private static final String LABEL_ARGUMENT = "label";
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands
            .literal("flansnpcbenchmark").requires(source -> source.hasPermission(2)).executes(
                context -> start(context, ServerBenchmark.DEFAULT_SECONDS, ServerBenchmark.DEFAULT_WARM_UP_SECONDS, ""))
            .then(
                Commands.argument(SECONDS_ARGUMENT, IntegerArgumentType.integer(ServerBenchmark.MIN_SECONDS, ServerBenchmark.MAX_SECONDS))
                    .executes(context -> start(context, IntegerArgumentType.getInteger(context, SECONDS_ARGUMENT), ServerBenchmark.DEFAULT_WARM_UP_SECONDS, "")).then(
                        Commands.argument(WARM_UP_ARGUMENT, IntegerArgumentType.integer(0, BenchmarkOptions.MAX_WARM_UP_SECONDS))
                            .executes(context -> start(context, IntegerArgumentType.getInteger(context, SECONDS_ARGUMENT), IntegerArgumentType.getInteger(context, WARM_UP_ARGUMENT), ""))
                            .then(Commands.argument(LABEL_ARGUMENT, StringArgumentType.greedyString()).executes(context -> start(context, IntegerArgumentType.getInteger(context, SECONDS_ARGUMENT),
                                IntegerArgumentType.getInteger(context, WARM_UP_ARGUMENT), StringArgumentType.getString(context, LABEL_ARGUMENT))))))
            .then(Commands.literal("status").executes(context ->
            {
                if (ServerBenchmark.status(context.getSource()))
                    return 1;
                context.getSource().sendFailure(Component.translatable("commands.wolffsmodnpcs.benchmark.idle"));
                return 0;
            })).then(Commands.literal("stop").executes(context ->
            {
                if (ServerBenchmark.stop(context.getSource().getServer(), "stopped by command"))
                    return 1;
                context.getSource().sendFailure(Component.translatable("commands.wolffsmodnpcs.benchmark.idle"));
                return 0;
            })));
    }

    private static int start(CommandContext<CommandSourceStack> context, int seconds, int warmUp, String label)
    {
        if (ServerBenchmark.start(context.getSource(), new BenchmarkOptions(seconds, warmUp, label)))
            return 1;
        context.getSource().sendFailure(Component.translatable("commands.wolffsmodnpcs.benchmark.busy"));
        return 0;
    }
}
