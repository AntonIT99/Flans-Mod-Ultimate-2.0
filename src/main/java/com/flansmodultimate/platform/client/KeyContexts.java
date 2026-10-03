package com.flansmodultimate.platform.client;

import com.flansmodultimate.FlansMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

import net.minecraft.client.KeyMapping;

/**
 * Loader boundary for key-binding conflict contexts, whose types moved from Forge to NeoForge packages.
 * Shared code passes these values and builds its mappings here, so it never names the loader types.
 */
public final class KeyContexts
{
    /** Live whenever no screen is open. */
    public static final IKeyConflictContext IN_GAME = KeyConflictContext.IN_GAME;
    /** Live everywhere; every vanilla mapping uses it, so it conflicts with every other context. */
    public static final IKeyConflictContext UNIVERSAL = KeyConflictContext.UNIVERSAL;

    private KeyContexts() {}

    /** A conflict context of this mod; {@link #conflictsWith} receives the loader's other context. */
    public interface ModContext extends IKeyConflictContext
    {
        boolean conflictsWith(Object other);

        @Override
        default boolean conflicts(IKeyConflictContext other)
        {
            return conflictsWith(other);
        }
    }

    /** A keyboard mapping named {@code key.flansmodultimate.<name>}. */
    public static KeyMapping key(String name, int keyCode, IKeyConflictContext context, String category)
    {
        return mapping("key." + FlansMod.MOD_ID + "." + name, context, keyCode, category);
    }

    /** A mouse-button mapping named {@code key.flansmodultimate.<name>}. */
    public static KeyMapping mouseKey(String name, int button, IKeyConflictContext context, String category)
    {
        return new KeyMapping("key." + FlansMod.MOD_ID + "." + name, context, InputConstants.Type.MOUSE, button, category);
    }

    /** A keyboard mapping with the full translation key as its name. */
    public static KeyMapping mapping(String name, IKeyConflictContext context, int keyCode, String category)
    {
        return new KeyMapping(name, context, InputConstants.Type.KEYSYM, keyCode, category);
    }
}
