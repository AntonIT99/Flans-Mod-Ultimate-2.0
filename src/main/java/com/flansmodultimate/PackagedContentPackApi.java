package com.flansmodultimate;

import com.flansmodultimate.content.PackagedContentLoader;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Map;
import java.util.Set;

/**
 * Public API for normal Forge mods that package immutable Flan content packs.
 * Registration must happen during the packaging mod's constructor, before Flan's Mod Ultimate
 * scans the user {@code flan} folder. This entry point keeps its original package for existing pack mods.
 */
public final class PackagedContentPackApi
{
    private PackagedContentPackApi() {}

    /** Registers the module's content packs during its mod constructor. */
    public static synchronized void register(FMLJavaModLoadingContext context, String modId, String contentRoot, String modelsRoot)
    {
        PackagedContentLoader.register(context, modId, contentRoot, modelsRoot);
    }

    /** Registers content packs with a set of packs that must remain enabled. */
    public static synchronized void register(FMLJavaModLoadingContext context, String modId, String contentRoot, String modelsRoot, Set<String> enforcedPackIds)
    {
        PackagedContentLoader.register(context, modId, contentRoot, modelsRoot, enforcedPackIds);
    }

    /** Registers content packs with enforced pack IDs and display-name overrides. */
    public static synchronized void register(FMLJavaModLoadingContext context, String modId, String contentRoot, String modelsRoot, Set<String> enforcedPackIds, Map<String, String> displayNameOverrides)
    {
        PackagedContentLoader.register(context, modId, contentRoot, modelsRoot, enforcedPackIds, displayNameOverrides);
    }
}
