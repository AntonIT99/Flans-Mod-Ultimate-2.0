package com.flansmodultimate.common.driveables.damage;

/** Explosion settings applied when a configured part is destroyed. */
public record DriveableExplosion(float fireRadius, float explosionRadius, boolean breaksBlocks, float damageVsLiving, float damageVsPlayer, float damageVsPlane, float damageVsVehicle)
{
    public DriveableExplosion(float fireRadius, float explosionRadius, boolean breaksBlocks)
    {
        this(fireRadius, explosionRadius, breaksBlocks, 1F, 1F, 1F, 1F);
    }
}
