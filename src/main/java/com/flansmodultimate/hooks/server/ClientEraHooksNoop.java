package com.flansmodultimate.hooks.server;

import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.hooks.IClientEraHooks;

import net.minecraft.world.entity.Entity;

public final class ClientEraHooksNoop implements IClientEraHooks
{
    @Override
    public boolean permits(Entity holder, InfoType type)
    {
        return true;
    }

    @Override
    public boolean isHidden(InfoType type)
    {
        return false;
    }
}
