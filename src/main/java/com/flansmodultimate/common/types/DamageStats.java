package com.flansmodultimate.common.types;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Holds and normalizes damage values for different target categories
 * (generic, living entities, players, vehicles, planes).
 * <p>
 * The idea:
 * <ul>
 *     <li>{@code damage} is the generic/base damage value.</li>
 *     <li>{@code damageVsLiving}, {@code damageVsPlayer},
 *         {@code damageVsVehicles}, {@code damageVsPlanes} are optional
 *         overrides for specific target types.</li>
 *     <li>The {@code readXxx} flags indicate which values were explicitly
 *         provided (e.g. parsed from config) and which should be derived
 *         from other values.</li>
 * </ul>
 *
 * Typical usage:
 * <ol>
 *     <li>Set any combination of {@code damage}, {@code damageVs*} and
 *         corresponding {@code readDamage*} flags from a config file.</li>
 *     <li>Call {@link #calculate()} once after loading the config.</li>
 *     <li>After that, all {@code damageVs*} fields are guaranteed to be
 *         populated with consistent values (no {@code read*} flags needed
 *         at runtime).</li>
 * </ol>
 *
 * Inheritance rules enforced by {@link #calculate()}:
 * <ul>
 *     <li>If {@code damage} was not explicitly read ({@code readDamage == false}),
 *         it will be derived from the most specific available override in the
 *         following priority order:
 *         <ol>
 *             <li>{@code damageVsLiving}</li>
 *             <li>{@code damageVsVehicles}</li>
 *             <li>{@code damageVsPlayer}</li>
 *             <li>{@code damageVsPlanes}</li>
 *         </ol>
 *     </li>
 *     <li>If {@code damageVsLiving} was not explicitly read, it falls back to {@code damage}.</li>
 *     <li>If {@code damageVsPlayer} was not explicitly read, it falls back to {@code damageVsLiving}.</li>
 *     <li>If {@code damageVsVehicles} was not explicitly read, it falls back to {@code damage}.</li>
 *     <li>If {@code damageVsPlanes} was not explicitly read, it falls back to {@code damageVsVehicles}.</li>
 * </ul>
 *
 * This chaining allows very compact config definitions while still supporting
 * fine-grained per-target overrides when needed.
 */
@Getter @Setter
@NoArgsConstructor
public final class DamageStats
{
    private float damage = 1.0F;
    private float damageVsLiving = 1.0F;
    private float damageVsPlayer = 1.0F;
    private float damageVsVehicles = 1.0F;
    private float damageVsPlanes = 1.0F;
    private boolean readDamage;
    private boolean readDamageVsLiving;
    private boolean readDamageVsPlayer;
    private boolean readDamageVsVehicles;
    private boolean readDamageVsPlanes;

    /**
     * Normalizes all damage fields according to the read flags and
     * inheritance rules described in the class-level documentation.
     * <p>
     * This method should be called once after all values (and {@code read*}
     * flags) have been loaded from configuration. After calling it, all
     * damage fields will be self-contained and suitable for direct use
     * in gameplay logic without further fallback checks.
     */
    public void calculate()
    {
        // Determine base damage if it was not explicitly provided.
        if (!readDamage)
        {
            if (readDamageVsLiving)
                damage = damageVsLiving;
            else if (readDamageVsVehicles)
                damage = damageVsVehicles;
            else if (readDamageVsPlayer)
                damage = damageVsPlayer;
            else if (readDamageVsPlanes)
                damage = damageVsPlanes;
        }

        // Propagate base damage into more specific fields where no explicit value was given.
        if (!readDamageVsLiving)
            damageVsLiving = damage;
        if (!readDamageVsPlayer)
            damageVsPlayer = damageVsLiving;
        if (!readDamageVsVehicles)
            damageVsVehicles = damage;
        if (!readDamageVsPlanes)
            damageVsPlanes = damageVsVehicles;
    }

    public float getDamageAgainstEntity(Entity entity)
    {
        if (entity instanceof Player)
            return damageVsPlayer;
        else if (entity instanceof Plane)
            return damageVsPlanes;
        else if (entity instanceof Driveable)
            return damageVsVehicles;
        else if (entity instanceof LivingEntity)
            return damageVsLiving;
        else
            return damage;
    }
}
