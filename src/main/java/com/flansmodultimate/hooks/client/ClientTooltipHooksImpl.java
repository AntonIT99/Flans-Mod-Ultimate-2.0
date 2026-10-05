package com.flansmodultimate.hooks.client;

import com.flansmodultimate.hooks.IClientTooltipHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class ClientTooltipHooksImpl implements IClientTooltipHooks
{
    /** Above zero while a search index reads tooltips; render thread only. */
    private static int indexing;

    /**
     * Runs the task as if no key were held, so that tooltips read for a search index are the ones
     * shown at rest, whatever key the player holds while the index is built.
     */
    public static <T> T withoutHeldKeys(Supplier<T> task)
    {
        indexing++;
        try
        {
            return task.get();
        }
        finally
        {
            indexing--;
        }
    }

    @Override
    public boolean isShiftDown()
    {
        return indexing == 0 && Screen.hasShiftDown();
    }

    @Override
    public Component getShiftKeyName()
    {
        return Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
    }
}
