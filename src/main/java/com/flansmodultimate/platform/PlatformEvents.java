package com.flansmodultimate.platform;

import com.flansmodultimate.platform.event.FlanCancellableEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import java.util.List;

/** Loader boundary for posting events to the game event bus. */
public final class PlatformEvents
{
    private PlatformEvents() {}

    public static void post(Event event)
    {
        NeoForge.EVENT_BUS.post(event);
    }

    /** Posts a cancellable event and returns whether a listener cancelled it. */
    public static boolean postCancellable(FlanCancellableEvent event)
    {
        return NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    /** Whether another listener denied using the clicked block. */
    public static boolean isBlockUseDenied(PlayerInteractEvent.RightClickBlock event)
    {
        return event.getUseBlock().isFalse();
    }

    /** Posts the explosion start event and returns whether a listener cancelled the explosion. */
    public static boolean onExplosionStart(Level level, Explosion explosion)
    {
        return EventHooks.onExplosionStart(level, explosion);
    }

    /** Posts the explosion detonate event, which may remove entities from the affected list. */
    public static void onExplosionDetonate(Level level, Explosion explosion, List<Entity> entities, double diameter)
    {
        EventHooks.onExplosionDetonate(level, explosion, entities, diameter);
    }
}
