package com.wolffsmod.npcs.platform.client;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.client.ClientBenchmark;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Version boundary for the module's client game events: hands Forge's events to loader-neutral handlers. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = NpcsMod.MOD_ID, value = Dist.CLIENT)
public final class NpcsClientGameEvents
{
    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            ClientBenchmark.frame();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event)
    {
        ClientBenchmark.registerCommands(event.getDispatcher());
    }
}
