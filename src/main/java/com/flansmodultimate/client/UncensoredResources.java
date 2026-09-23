package com.flansmodultimate.client;

import net.minecraft.client.Minecraft;

/** Client-only resource reload bridge for the uncensored resource toggle. */
public final class UncensoredResources
{
    private UncensoredResources()
    {
    }

    public static void reload()
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(minecraft::reloadResourcePacks);
    }
}
