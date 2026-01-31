package com.flansmodultimate.hooks.client;

import com.flansmodultimate.hooks.IClientTooltipHooks;

import net.minecraft.client.KeyMapping;
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
        KeyMapping shiftKey = Minecraft.getInstance().options.keyShift;
        return shiftKey.getTranslatedKeyMessage();
    }
}
