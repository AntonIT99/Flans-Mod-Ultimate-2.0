package com.flansmodultimate.platform.world;

import net.minecraft.world.level.saveddata.SavedData;

/**
 * Saved data that is written with registry context. Minecraft 1.21 already passes the registries to
 * {@code save}; the 1.20.1 counterpart adapts its one-argument {@code save} here.
 */
public abstract class FlanSavedData extends SavedData
{
}
