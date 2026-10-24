/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.flansmodultimate.common.driveables.weapons.DriveableAmmoLoader;
import com.flansmodultimate.common.entity.Parachute;
import com.flansmodultimate.common.guns.EquipmentSupport;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalDouble;

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

    /**
     * @param ammunition
     *            ammunition to measure the magazine with when the gun holds none; may be empty
     * @return the sight magnification and magazine capacity of a Flan gun, or empty for any other item; either side
     */
    public static Optional<WeaponTraits> getWeaponTraits(ItemStack weapon, ItemStack ammunition)
    {
        return EquipmentSupport.traits(weapon, ammunition);
    }

    /** @return the shield of a held Flan weapon, or empty for an item that is not a Flan shield; either side */
    public static Optional<EquippedShieldProperties> getShieldProperties(ItemStack weapon)
    {
        return EquipmentSupport.shield(weapon);
    }

    /** @return additional Flan armor properties, or empty for another armor item; either side */
    public static Optional<EquippedArmorProperties> getArmorProperties(ItemStack armor)
    {
        return EquipmentSupport.armor(armor);
    }

    /**
     * Tells whether a gun is a deployable one, such as a tripod machine gun: it is placed in the world and fired from
     * there, never from the hand. Either side.
     *
     * @return whether the stack is such a gun; false for any other item
     */
    public static boolean isDeployable(ItemStack weapon)
    {
        return EquipmentSupport.deployable(weapon);
    }

    /** @return whether the gun has a usable loaded round; does not consume ammunition; either side */
    public static boolean hasLoadedRound(ItemStack weapon)
    {
        return EquipmentSupport.hasLoadedRound(weapon);
    }

    /**
     * Tells whether a gun can be reloaded with a stack: ammunition the gun takes, in its current fire mode, that still
     * holds rounds. Nothing is consumed. Either side.
     *
     * @param weapon
     *            the gun; false for any other item
     * @param ammunition
     *            the stack offered to it
     * @return whether {@link #loadMagazine} would accept the stack for an empty slot of the gun
     */
    public static boolean acceptsAmmunition(ItemStack weapon, ItemStack ammunition)
    {
        return EquipmentSupport.acceptsAmmunition(weapon, ammunition);
    }

    /**
     * @param gun
     *            a gun definition, such as one a driveable mounts
     * @return a new full stack of the ammunition the gun is issued with by default, or empty for another definition or a
     *         gun with none; either side after content loading
     */
    public static Optional<ItemStack> createDefaultAmmunition(IContentType gun)
    {
        if (!(gun instanceof GunType gunType))
            return Optional.empty();
        return gunType.getDefaultAmmo().flatMap(ModUtils::getItemStack).map(stack -> stack.copyWithCount(stack.getMaxStackSize()));
    }

    /**
     * Creates ammunition for the selected driver bank without changing an inventory. Either side after content loading.
     * Uses the first mounted gun's default or the bank's declared ordnance, with the fallback used by default-ammo loading.
     *
     * @param vehicle
     *            the driveable definition
     * @param secondary
     *            whether to select the secondary bank rather than the primary bank
     * @return a new full stack, or empty for an unsupported definition or a bank without ammunition
     */
    public static Optional<ItemStack> createDefaultAmmunition(IContentType vehicle, boolean secondary)
    {
        if (!(vehicle instanceof DriveableType type))
            return Optional.empty();
        return DriveableAmmoLoader.defaultBankAmmo(type, secondary).flatMap(ModUtils::getItemStack).map(stack -> stack.copyWithCount(stack.getMaxStackSize()));
    }

    /**
     * Opens a parachute over a living entity, as a player's parachute tool does, taking the first parachute tool of the
     * loaded content packs by short name. The entity drifts down on it until it lands. Server thread.
     *
     * @return whether the parachute opened; false without a parachute tool, on a client, or without room above the entity
     */
    public static boolean deployParachute(LivingEntity rider)
    {
        if (rider.level().isClientSide)
            return false;
        Optional<ToolType> tool = InfoType.getInfoTypes().values().stream().filter(ToolType.class::isInstance).map(ToolType.class::cast).filter(ToolType::isParachute)
            .min(Comparator.comparing(ToolType::getShortName));
        return tool.map(type -> Parachute.spawnAndMount(rider.level(), rider, type) != null).orElse(false);
    }

    /**
     * Ammunition for a gun someone holds, as a supply bag would hand it out.
     *
     * @param weapon
     *            a Flan gun stack
     * @return a new full stack of the gun's default ammunition, or empty for another item or a gun with none; either side
     */
    public static Optional<ItemStack> createDefaultAmmunition(ItemStack weapon)
    {
        return weapon.getItem() instanceof com.flansmodultimate.common.item.GunItem gun ? createDefaultAmmunition(gun.getConfigType()) : Optional.empty();
    }

    /** @return what a Flan tool does when used, or empty for another item; either side */
    public static Optional<ToolProperties> getToolProperties(ItemStack tool)
    {
        if (!(tool.getItem() instanceof com.flansmodultimate.common.item.ToolItem item))
            return Optional.empty();
        ToolType type = item.getConfigType();
        return Optional.of(new ToolProperties(type.isHealPlayers(), type.isHealDriveables(), type.getHealAmount(), type.isParachute()));
    }

    /**
     * How much armour a gun's round goes through, the measure anti-tank weapons are told apart by.
     *
     * @param ammunition
     *            ammunition to measure with when the gun holds none; may be empty
     * @return the round's penetration in millimetres of armour at 100 metres, 0 for a round without a stated
     *         penetration, or empty for an item that is no Flan gun; either side
     */
    public static OptionalDouble getArmorPenetration(ItemStack weapon, ItemStack ammunition)
    {
        return EquipmentSupport.armorPenetration(weapon, ammunition);
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
     * Raises the held gun's aim pose and plays its shot animation and muzzle flash model for clients that see the holder,
     * as {@link #fire} does. For a caller that launches its own projectile on behalf of the held gun. Server thread only;
     * honors {@link IEquipmentPolicy#flansGunShotAnimations()}.
     *
     * @param holder
     *            entity holding the gun
     * @param hand
     *            hand holding the gun; nothing happens when it holds no Flan gun
     */
    public static void animateShot(LivingEntity holder, InteractionHand hand)
    {
        EquipmentSupport.animateShot(holder, hand);
    }

    /**
     * Plays the held gun's reload animation for clients that see the holder. Players animate their own reloads, so a player
     * holder is ignored. Server thread only.
     *
     * @param holder
     *            non-player entity holding the gun
     * @param hand
     *            hand holding the gun; nothing happens when it holds no Flan gun
     * @param reloadTicks
     *            duration of the animation in ticks; nothing happens when not positive
     */
    public static void animateReload(LivingEntity holder, InteractionHand hand, float reloadTicks)
    {
        EquipmentSupport.animateReload(holder, hand, reloadTicks);
    }

    /**
     * Plays the held gun's melee animation for clients that see the holder. Players animate their own melee, so a player
     * holder is ignored. Server thread only.
     *
     * @param holder
     *            non-player entity holding the gun
     * @param hand
     *            hand holding the gun; nothing happens when it holds no Flan gun
     */
    public static void animateMelee(LivingEntity holder, InteractionHand hand)
    {
        EquipmentSupport.animateMelee(holder, hand);
    }

    /**
     * Plays the held gun's melee sound with its authored range. Server thread only.
     *
     * @param holder
     *            entity holding the gun
     * @param hand
     *            hand holding the gun
     * @return whether the gun declares a melee sound and it was sent
     */
    public static boolean playMeleeSound(LivingEntity holder, InteractionHand hand)
    {
        return EquipmentSupport.meleeSound(holder, hand);
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
        return EquipmentSupport.fire(holder, hand, origin, direction, sounds, particles, WeaponOverrides.NONE);
    }

    /**
     * Fires one loaded round like {@link #fire(LivingEntity, InteractionHand, Vec3, Vec3, boolean, boolean)}, with some of the
     * gun's own statistics replaced by the caller's. Throwable guns ignore the overrides. Server thread only.
     *
     * @param overrides
     *            statistics that replace the gun's own; {@link WeaponOverrides#NONE} fires it unchanged
     * @return whether an attack launched; dry or disabled weapons return false
     */
    public static boolean fire(LivingEntity holder, InteractionHand hand, Vec3 origin, Vec3 direction, boolean sounds, boolean particles, WeaponOverrides overrides)
    {
        return EquipmentSupport.fire(holder, hand, origin, direction, sounds, particles, overrides);
    }

    /**
     * Prepares a throwable Flan gun from any inventory slot, using its native damage, velocity, spread and recoverable stack.
     * Server thread only. The returned shot neither consumes nor equips the item; the caller must consume one after a successful launch.
     *
     * @param holder
     *            thrower and damage owner
     * @param weapon
     *            throwable gun stack, copied as a single weapon for the projectile
     * @return resolved throw with its flight physics, or empty for an unusable weapon or a nonserver caller
     */
    public static Optional<ProjectileShot> prepareThrow(LivingEntity holder, ItemStack weapon)
    {
        return EquipmentSupport.prepareThrow(holder, weapon);
    }
}
