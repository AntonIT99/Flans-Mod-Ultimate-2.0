package com.flansmodultimate.hooks.client;

import com.flansmodultimate.hooks.IClientTooltipHooks;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class ClientTooltipHooksImpl implements IClientTooltipHooks
{
    /** Above zero while a search index reads tooltips on the render thread; render thread only. */
    private static int indexing;

    /**
     * Runs the task as if no key were held, so that tooltips read for a search index are the ones
     * shown at rest, whatever key the player holds while the index is built.
     */
    public static <T> T withoutHeldKeys(Supplier<T> task)
    {
        boolean renderThread = RenderSystem.isOnRenderThread();
        if (renderThread)
            indexing++;
        try
        {
            return task.get();
        }
        finally
        {
            if (renderThread)
                indexing--;
        }
    }

    /**
     * Held keys only count for a tooltip being shown. Search indexes, built on their first search
     * or, with eager search trees, on background threads, must index the tooltip shown at rest, not
     * the detailed statistics shown while Shift is held; the keyboard is not polled off the render
     * thread either.
     */
    @Override
    public boolean isShiftDown()
    {
        return RenderSystem.isOnRenderThread() && indexing == 0 && Screen.hasShiftDown();
    }

    @Override
    public Component getShiftKeyName()
    {
        return Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
    }
}
