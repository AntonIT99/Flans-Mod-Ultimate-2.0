package com.flansmodultimate.common.guns;

/**
 * Functional interface to add effects when a bullet is fired. This is used to reduce the ammo, apply knockback, drop empty shells, etc.
 */
public interface ShootingHandler
{
    void onShoot();
}
