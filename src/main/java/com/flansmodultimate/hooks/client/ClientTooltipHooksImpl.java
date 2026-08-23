package com.flansmodultimate.hooks.client;

import com.flansmodultimate.hooks.IClientTooltipHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;

public class ClientTooltipHooksImpl implements IClientTooltipHooks
{
    @Override
    public boolean isShiftDown()
    {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public Component getShiftKeyName()
    {
        return Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
    }
}
