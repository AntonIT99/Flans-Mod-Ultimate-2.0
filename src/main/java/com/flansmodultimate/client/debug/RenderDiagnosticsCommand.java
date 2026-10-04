package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.render.gpu.RenderDiagnostics;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Client-local measurements; available without server permissions. */
public final class RenderDiagnosticsCommand
{
    private RenderDiagnosticsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansrenderstats")
            .executes(context -> {
                context.getSource().sendSuccess(() -> Component.literal(RenderDiagnostics.report()), false);
                return 1;
            })
            .then(Commands.literal("start").executes(context -> {
                RenderDiagnostics.start();
                context.getSource().sendSuccess(() -> Component.literal("Flan render counters reset and recording."), false);
                return 1;
            }))
            .then(Commands.literal("stop").executes(context -> {
                RenderDiagnostics.stop();
                context.getSource().sendSuccess(() -> Component.literal(RenderDiagnostics.report()), false);
                return 1;
            }))
            .then(Commands.literal("reset").executes(context -> {
                RenderDiagnostics.reset();
                context.getSource().sendSuccess(() -> Component.literal("Flan render counters reset."), false);
                return 1;
            }))
            // Size-culled indices drawn anyway between visible ranges, for comparing frame times from one spot.
            .then(Commands.literal("bridge")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Flan GPU batches bridge size-culling gaps of up to "
                        + RenderDiagnostics.bridgedGap() + " indices."), false);
                    return 1;
                })
                .then(Commands.argument("indices", IntegerArgumentType.integer(0, 1 << 20)).executes(context -> {
                    RenderDiagnostics.setBridgedGap(IntegerArgumentType.getInteger(context, "indices"));
                    context.getSource().sendSuccess(() -> Component.literal("Flan GPU batches now bridge size-culling gaps of up to "
                        + RenderDiagnostics.bridgedGap() + " indices (0 turns bridging off; the default is 600)."), false);
                    return 1;
                }))));
    }
}
