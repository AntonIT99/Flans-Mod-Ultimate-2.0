/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.guns.EquipmentSupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Equipment inspection and native Flan weapon actions for addon-controlled living entities. */
@ApiStatus.Experimental
public final class FlansEquipment
{
    private FlansEquipment()
    {}

    /** @return stack-aware Flan weapon properties, using loaded ammunition first and the supplied preview ammunition otherwise; either side */
    public static Optional<EquippedWeaponProperties> getWeaponProperties(ItemStack weapon, ItemStack ammunition)
    {
        return EquipmentSupport.weapon(weapon, ammunition);
    }

    /** @return additional Flan armor properties, or empty for another armor item; either side */
    public static Optional<EquippedArmorProperties> getArmorProperties(ItemStack armor)
    {
        return EquipmentSupport.armor(armor);
    }

    /** @return whether the gun has a usable loaded round; does not consume ammunition; either side */
    public static boolean hasLoadedRound(ItemStack weapon)
    {
        return EquipmentSupport.hasLoadedRound(weapon);
    }

    /** @return a detached copy of the first usable loaded magazine, or empty; either side, without consumption */
    public static Optional<ItemStack> getLoadedAmmunition(ItemStack weapon)
    {
        return EquipmentSupport.loadedAmmunition(weapon);
    }

    /** @return the next round's index in a loaded belt, or zero when empty; either side, without consumption */
    public static int getLoadedRoundIndex(ItemStack weapon)
    {
        return EquipmentSupport.loadedRoundIndex(weapon);
    }

    /** Plays the native empty/secondary reload sound on the logical server thread. */
    public static void playReloadSound(LivingEntity holder, ItemStack weapon)
    {
        EquipmentSupport.reloadSound(holder, weapon);
    }

    /**
     * Moves compatible spare magazines into empty/spent gun slots and drops authored spent-magazine items. The supplied spare stack is consumed.
     * Call on the server thread after the caller's reload timer expires; incompatible ammunition is unchanged.
     *
     * @return whether a magazine was loaded
     */
    public static boolean loadMagazine(LivingEntity holder, ItemStack weapon, ItemStack spare)
    {
        return EquipmentSupport.loadMagazine(holder, weapon, spare);
    }

    /**
     * Fires one loaded round using native ammunition, attachments and belt position, or throws a throwable gun.
     * Server thread only. Successful shots consume ammunition; throws remove one held weapon and retain it in the projectile.
     *
     * @param origin
     *            world launch position in blocks
     * @param direction
     *            aim vector, normalized internally
     * @param sounds
     *            whether to broadcast firing sounds
     * @param particles
     *            whether to broadcast muzzle particles
     * @return whether an attack launched; dry or disabled weapons return false
     */
    public static boolean fire(LivingEntity holder, InteractionHand hand, Vec3 origin, Vec3 direction, boolean sounds, boolean particles)
    {
        return EquipmentSupport.fire(holder, hand, origin, direction, sounds, particles);
    }
}
