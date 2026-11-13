package com.flansmodultimate.hooks.client;

import com.flansmodultimate.client.eras.EraClientState;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.hooks.IClientEraHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ClientEraHooksImpl implements IClientEraHooks
{
    @Override
    public boolean permits(Entity holder, InfoType type)
    {
        return holder != Minecraft.getInstance().player || EraClientState.check(type).allowed();
    }

    @Override
    public boolean isHidden(InfoType type)
    {
        return !EraClientState.check(type).allowed();
    }
}
