/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.projectiles;

import com.flansmodultimate.common.entity.GrenadeDeviceSupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Placement, ownership and use of grenade-based mines, remote charges and supply bags. */
@ApiStatus.Experimental
public final class FlansGrenadeDevices
{
    private FlansGrenadeDevices()
    {}

    /** @return deployment properties of a carried device, or empty for other items; either side */
    public static Optional<GrenadeDeviceProperties> properties(ItemStack stack)
    {
        return GrenadeDeviceSupport.properties(stack);
    }

    /** @return deployment properties of a live device, or empty for other entities; level thread */
    public static Optional<GrenadeDeviceProperties> properties(Entity entity)
    {
        return GrenadeDeviceSupport.properties(entity);
    }

    /** @return loaded grenade devices intersecting the world-space box; level thread, without loading chunks */
    public static List<Entity> find(Level level, AABB area)
    {
        return GrenadeDeviceSupport.find(level, area);
    }

    /** @return the saved owner's UUID matches this entity, including before owner resolution after loading; level thread */
    public static boolean ownedBy(Entity device, LivingEntity owner)
    {
        return GrenadeDeviceSupport.ownedBy(device, owner);
    }

    /** @return the resolved living owner, or empty when unavailable; level thread */
    public static Optional<LivingEntity> owner(Entity device)
    {
        return GrenadeDeviceSupport.owner(device);
    }

    /** @return the device's blast, fragment and fire reach, or empty for another entity; level thread */
    public static Optional<DetonationRadii> radii(Entity device)
    {
        return GrenadeDeviceSupport.radii(device);
    }

    /**
     * Places a supported device on the top of a solid block within 4.5 blocks of the operator.
     * Validates loaded terrain, free space and finite coordinates. Facing is a world-space direction.
     * Does not consume the stack; the caller consumes one item after success. Server thread only.
     *
     * @return the placed entity, or empty when placement is refused
     */
    public static Optional<Entity> place(LivingEntity operator, ItemStack stack, BlockPos support, Vec3 facing, boolean sound)
    {
        return GrenadeDeviceSupport.place(operator, stack, support, facing, sound);
    }

    /** @return whether the owner's remote charge is armed and still available; level thread */
    public static boolean canDetonate(Entity device, LivingEntity operator)
    {
        return GrenadeDeviceSupport.canDetonate(device, operator);
    }

    /** Detonates only an armed remote charge belonging to the operator. Returns whether it detonated; server thread only. */
    public static boolean detonate(Entity device, LivingEntity operator)
    {
        return GrenadeDeviceSupport.detonate(device, operator);
    }

    /**
     * Tests useful healing/effects or ammunition offered by a bag, without using it. The ammunition flag means the caller
     * needs ammunition and has inventory space; weapon is the gun to rearm. Level thread.
     *
     * @return whether at least one benefit is useful
     */
    public static boolean needsSupply(Entity bag, LivingEntity recipient, ItemStack weapon, boolean ammunition)
    {
        return GrenadeDeviceSupport.needsSupply(bag, recipient, weapon, ammunition);
    }

    /** @return whether deploying a carried bag would offer this recipient a useful benefit; level thread */
    public static boolean needsSupply(ItemStack bag, LivingEntity recipient, ItemStack weapon, boolean ammunition)
    {
        return GrenadeDeviceSupport.needsSupply(bag, recipient, weapon, ammunition);
    }

    /**
     * Uses a bag within three blocks (six for players, whose normal interaction validates reach), consuming one use only if healing, effects or ammunition are actually delivered.
     * The receiver accepts fresh ammunition stacks and returns true only after storing the whole stack.
     * Supply effects and ammunition amounts follow the bag definition and the gun's rearm permission. Server thread only.
     *
     * @return whether any benefit was delivered
     */
    public static boolean useSupply(Entity bag, LivingEntity recipient, ItemStack weapon, Predicate<ItemStack> receiver)
    {
        return GrenadeDeviceSupport.useSupply(bag, recipient, weapon, receiver);
    }
}
