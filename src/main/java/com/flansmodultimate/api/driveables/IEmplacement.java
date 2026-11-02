/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import com.flansmodultimate.api.content.IContentType;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Supplier;

/** A placed deployable or manually operated AA gun. Mutations run on the server thread. */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IEmplacement
{
    /** @return the weapon definition, or empty while unavailable */
    Optional<IContentType> getEmplacementType();

    /** @return whether the mount currently holds any live ammunition; either side */
    boolean hasEmplacementAmmunition();

    /** @return the muzzle position in world blocks */
    Vec3 getEmplacementOrigin();

    /** @return the unit firing direction in world coordinates */
    Vec3 getEmplacementDirection();

    /** @return a supported operator position in world blocks, or empty if the mount cannot be manned */
    Optional<Vec3> getEmplacementOperatorPosition(LivingEntity operator);

    /** @return whether a world position lies within the weapon's mechanical firing arc */
    boolean canEmplacementAimAt(Vec3 point);

    /** Aims the mounted operator at a world position; the mount retains its native traverse limits and speed. */
    void aimEmplacement(LivingEntity operator, Vec3 point);

    /** Sets non-player firing intent. Renew each server tick; stale intent expires automatically. */
    void setEmplacementFiring(boolean firing);

    /**
     * Reloads an empty mount with compatible ammunition supplied on demand, retaining native reload timing.
     * The supplier transfers ownership of each returned stack; it must remove finite supplies from the owner's inventory.
     * Virtual supplies simulate magazine use and reloads but are neither saved nor dropped, and vanish on dismount.
     * Only the current living operator may reload. Server thread only.
     */
    void reloadEmplacement(LivingEntity operator, Supplier<ItemStack> ammunition, boolean virtual);
}
