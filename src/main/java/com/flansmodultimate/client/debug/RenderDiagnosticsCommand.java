package com.flansmodultimate.client.debug;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.render.gpu.RenderDiagnostics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Client-local measurements; available without server permissions. */
@EventBusSubscriber(modid = FlansMod.MOD_ID, value = Dist.CLIENT)
public final class RenderDiagnosticsCommand
{
    private RenderDiagnosticsCommand() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("flansrenderstats")
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
            })));
    }
}
