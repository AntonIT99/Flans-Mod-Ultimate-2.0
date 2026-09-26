/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * A projectile entity fired by a Flan's weapon. Check a damage source's direct entity against it
 * to tell Flan's shots apart from other projectiles.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IBullet
{
    /**
     * @return who fired the shot: usually a player, otherwise a mob or the rider of a driveable
     */
    Optional<LivingEntity> getShooter();

    /**
     * @return the ammunition definition of this projectile
     */
    IContentType getBulletType();

    /**
     * @return the definition of the weapon that fired it, such as a gun, an AA gun or a driveable, when known
     */
    Optional<IContentType> getWeaponType();
}
