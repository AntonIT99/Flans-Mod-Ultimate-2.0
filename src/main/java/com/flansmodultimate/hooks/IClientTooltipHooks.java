package com.flansmodultimate.hooks;

import net.minecraft.network.chat.Component;

public interface IClientTooltipHooks
{
    boolean isShiftDown();

    Component getShiftKeyName();
}
