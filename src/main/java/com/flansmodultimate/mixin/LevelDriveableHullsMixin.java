package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.collision.DriveableCollisionWorld;
import com.flansmodultimate.common.driveables.collision.DriveableHullLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.level.Level;

/**
 * Stores the driveable hulls of a level on the level itself, so that the
 * per-move lookup is a field read and the registry is released with the level.
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(Level.class)
public abstract class LevelDriveableHullsMixin implements DriveableHullLevel
{
    @Unique
    private final DriveableCollisionWorld.LevelHulls flansmodultimateDriveableHulls = new DriveableCollisionWorld.LevelHulls();

    @Override
    public DriveableCollisionWorld.LevelHulls flansmodultimateGetDriveableHulls()
    {
        return flansmodultimateDriveableHulls;
    }
}
