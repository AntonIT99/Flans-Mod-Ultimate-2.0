package com.flansmodultimate.platform;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Loader boundary for posting events to the game event bus. */
public final class PlatformEvents
{
    private PlatformEvents() {}

    public static void post(Event event)
    {
        NeoForge.EVENT_BUS.post(event);
    }

    /** Posts a cancellable event and returns whether a listener cancelled it. */
    public static <T extends Event & ICancellableEvent> boolean postCancellable(T event)
    {
        return NeoForge.EVENT_BUS.post(event).isCanceled();
    }
}
