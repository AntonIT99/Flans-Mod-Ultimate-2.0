package com.flansmodultimate.platform.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Base of the mod's cancellable game events. Forge reads {@link Cancelable} from the superclass chain,
 * so subclasses need no annotation; NeoForge marks the same events with {@code ICancellableEvent}.
 * Post them through {@code PlatformEvents.postCancellable}.
 */
@Cancelable
public abstract class FlanCancellableEvent extends Event
{
}
