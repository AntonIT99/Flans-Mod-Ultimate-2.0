package com.flansmodultimate.hooks.client;

import com.flansmodultimate.hooks.IClientTooltipHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClientTooltipHooksImpl implements IClientTooltipHooks
{
    @Override
    public boolean isShiftDown()
    {
        return Screen.hasShiftDown();
    }

    @Override
    public Component getShiftKeyName()
    {
        return Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
    }
}
