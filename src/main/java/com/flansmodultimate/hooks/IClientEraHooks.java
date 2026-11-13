package com.flansmodultimate.hooks;

import com.flansmodultimate.common.types.InfoType;

import net.minecraft.world.entity.Entity;

/** The client's view of era restrictions, which only predicts and displays; the server decides. */
public interface IClientEraHooks
{
    /** Whether the holder may use the type as far as this client knows; only the local player is ever predicted blocked. */
    boolean permits(Entity holder, InfoType type);

    /** Whether the local player's creative tabs leave the type out. */
    boolean isHidden(InfoType type);
}
