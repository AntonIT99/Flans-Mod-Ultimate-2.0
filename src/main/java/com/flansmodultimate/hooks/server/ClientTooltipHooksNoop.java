package com.flansmodultimate.hooks.server;

import com.flansmodultimate.hooks.IClientTooltipHooks;

import net.minecraft.network.chat.Component;

public final class ClientTooltipHooksNoop implements IClientTooltipHooks
{
    @Override
    public boolean isShiftDown()
    {
        return false;
    }

    @Override
    public Component getShiftKeyName()
    {
        return Component.empty();
    }
}