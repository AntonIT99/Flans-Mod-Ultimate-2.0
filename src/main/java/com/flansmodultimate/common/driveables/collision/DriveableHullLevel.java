package com.flansmodultimate.common.driveables.collision;

/** Gives every {@code Level} its registry of driveable hulls; implemented by {@code LevelDriveableHullsMixin}. */
public interface DriveableHullLevel
{
    DriveableCollisionWorld.LevelHulls flansmodultimateGetDriveableHulls();
}
