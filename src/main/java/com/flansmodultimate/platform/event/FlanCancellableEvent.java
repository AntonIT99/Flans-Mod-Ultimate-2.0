package com.flansmodultimate.platform.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Base of the mod's cancellable game events. NeoForge marks them with {@link ICancellableEvent}; Forge
 * uses the {@code Cancelable} annotation, which its event bus reads from the superclass chain.
 * Post them through {@code PlatformEvents.postCancellable}.
 */
public abstract class FlanCancellableEvent extends Event implements ICancellableEvent
{}
