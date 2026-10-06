/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.guns.ExternalProjectileSupport;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Projectile and weapon presentation support for addon-controlled living entities. */
@ApiStatus.Experimental
public final class FlansProjectiles
{
    private FlansProjectiles()
    {}

    /** @return whether the stack represents a Flan bullet or grenade; readable on either side */
    public static boolean isProjectile(ItemStack stack)
    {
        return ExternalProjectileSupport.isProjectile(stack);
    }

    /** @return whether the stack represents a Flan grenade; readable on either side */
    public static boolean isGrenade(ItemStack stack)
    {
        return ExternalProjectileSupport.isGrenade(stack);
    }

    /**
     * Describes which caller defaults are replaced, without preparing or spawning a projectile.
     * Readable on either side after content loading, including for an editor preview.
     *
     * @param ammunition
     *            bullet or grenade template
     * @param heldWeapon
     *            held gun with attachments, or an empty stack
     * @param weapon
     *            mounted gun, or null to use the platform, held gun or ammunition
     * @param platform
     *            optional platform and its fallback ammunition overrides
     * @param secondary
     *            selected platform bank
     * @param weaponStats
     *            whether content weapon statistics take precedence
     * @return setting sources, or empty for unsupported ammunition
     */
    public static Optional<ProjectileSources> getSources(ItemStack ammunition, ItemStack heldWeapon, @Nullable IContentType weapon, @Nullable IContentType platform,
        boolean secondary, boolean weaponStats)
    {
        return ExternalProjectileSupport.getSources(ammunition, heldWeapon, weapon, platform, secondary, weaponStats);
    }

    /**
     * Resolves one round on the server thread. Ammunition physics and effects retain their content definitions.
     *
     * @param shooter
     *            owner and damage credit; its level owns the projectile
     * @param ammunition
     *            bullet or grenade item, used as a template without consumption
     * @param heldWeapon
     *            gun with attachments, or an empty stack
     * @param weapon
     *            mounted gun definition; null uses the platform, held gun or ammunition alone
     * @param platform
     *            platform definition, including its fallback ammunition overrides, or null
     * @param secondary
     *            whether platform weapon statistics come from its secondary bank
     * @param parameters
     *            belt position, weapon-statistics switch and fallback statistics
     * @return resolved shot, or empty for unsupported ammunition, invalid values or a nonserver caller
     */
    public static Optional<ProjectileShot> prepare(LivingEntity shooter, ItemStack ammunition, ItemStack heldWeapon, @Nullable IContentType weapon, @Nullable IContentType platform,
        boolean secondary, ProjectileParameters parameters)
    {
        return ExternalProjectileSupport.prepare(shooter, ammunition, heldWeapon, weapon, platform, secondary, parameters);
    }

    /**
     * Selects the muzzles used by a static AA or driveable model, without creating a driveable entity.
     *
     * @param model
     *            model definition
     * @param secondary
     *            selected weapon bank
     * @param sequence
     *            trigger index, used to alternate points and individual barrels
     * @param alternate
     *            whether authored alternating fire is honored; false fires every barrel together
     * @return immutable selected muzzles; empty when the model has no defined weapon positions
     */
    public static List<WeaponMuzzle> getMuzzles(IContentType model, boolean secondary, int sequence, boolean alternate)
    {
        return ExternalProjectileSupport.getMuzzles(model, secondary, sequence, alternate);
    }

    /**
     * Plays the selected weapon's content-pack firing sound. Server thread only.
     *
     * @param shooter
     *            source position and dimension
     * @param weapon
     *            mounted gun or platform, or null to use the held weapon
     * @param heldWeapon
     *            gun stack whose attachments can select a silenced sound
     * @param secondary
     *            selected platform bank
     * @return whether a sound was available and sent
     */
    public static boolean playShootSound(Entity shooter, @Nullable IContentType weapon, ItemStack heldWeapon, boolean secondary)
    {
        return ExternalProjectileSupport.playShootSound(shooter, weapon, heldWeapon, secondary);
    }

    /**
     * Broadcasts authored shooting particles, with a small muzzle flame when none are declared.
     * Server thread only; clients create the particles.
     *
     * @param shooter
     *            dimension and nearby recipients
     * @param muzzle
     *            selected muzzle's particles
     * @param origin
     *            world muzzle position in blocks
     * @param bodyYaw
     *            vanilla entity yaw in degrees, rotating the model's -Z facing into world space
     * @param scale
     *            particle size and velocity multiplier
     */
    public static void shootParticles(Entity shooter, WeaponMuzzle muzzle, Vec3 origin, float bodyYaw, float scale)
    {
        ExternalProjectileSupport.shootParticles(shooter, muzzle, origin, bodyYaw, scale);
    }
}
