package com.flansmodultimate.platform.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.gui.screens.Screen;

/**
 * Failed-login screens. Forge 1.20.1 dereferences mod metadata for every mismatched namespace, so legacy registry
 * namespaces such as {@code flansmod} need a replacement screen there. NeoForge's registry failure reason is shortened
 * by ConnectionMixin before its ordinary disconnect screen is constructed, so its screens need no replacement.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentMismatchScreens
{
    /** The screen to open instead; the Forge platform replaces its mismatch screen here. */
    public static Screen safeScreen(Screen screen)
    {
        return screen;
    }
}
