package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.DriveableCollisionWorld;
import com.flansmodultimate.common.driveables.DriveableHullLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.level.Level;

/**
 * Stores the driveable hulls of a level on the level itself, so that the
 * per-move lookup is a field read and the registry is released with the level.
 */
@Mixin(Level.class)
public abstract class LevelDriveableHullsMixin implements DriveableHullLevel
{
    @Unique
    private final DriveableCollisionWorld.LevelHulls flansmodultimate$driveableHulls =
        new DriveableCollisionWorld.LevelHulls();

    @Override
    public DriveableCollisionWorld.LevelHulls flansmodultimate$getDriveableHulls()
    {
        return flansmodultimate$driveableHulls;
    }
}
