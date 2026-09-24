package com.flansmodultimate.platform;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/** Loader boundary for posting events to the game event bus. */
public final class PlatformEvents
{
    private PlatformEvents() {}

    public static void post(Event event)
    {
        MinecraftForge.EVENT_BUS.post(event);
    }

    /** Posts a cancellable event and returns whether a listener cancelled it. */
    public static boolean postCancellable(Event event)
    {
        return MinecraftForge.EVENT_BUS.post(event);
    }

    /** Whether another listener denied using the clicked block. */
    public static boolean isBlockUseDenied(PlayerInteractEvent.RightClickBlock event)
    {
        return event.getUseBlock() == Event.Result.DENY;
    }
}
