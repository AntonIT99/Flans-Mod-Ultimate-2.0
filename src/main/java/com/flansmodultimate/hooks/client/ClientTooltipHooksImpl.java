package com.flansmodultimate.hooks.client;

import com.flansmodultimate.hooks.IClientTooltipHooks;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClientTooltipHooksImpl implements IClientTooltipHooks
{
    /**
     * Held keys only count for a tooltip being shown. The game builds its creative and recipe
     * search indexes on background threads, and those must index the tooltip shown at rest, not the
     * detailed statistics shown while Shift is held; the keyboard is not polled off the render
     * thread either.
     */
    @Override
    public boolean isShiftDown()
    {
        return RenderSystem.isOnRenderThread() && Screen.hasShiftDown();
    }

    @Override
    public Component getShiftKeyName()
    {
        return Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
    }
}
