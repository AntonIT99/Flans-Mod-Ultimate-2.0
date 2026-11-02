/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.projectiles;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Receives actual projectile armour tests on real driveables and addon vehicle bodies, on the server thread. */
@ApiStatus.Experimental
@FunctionalInterface
public interface IArmorImpactListener
{
    /**
     * Called once per part impact with a known living attacker, including hitscan. A penetration does not guarantee damage.
     *
     * @param attacker
     *            living entity credited with the round
     * @param target
     *            entity whose armour was tested
     * @param part
     *            legacy part name, such as core or turret
     * @param outwardNormal
     *            struck face in the part's semantic frame: front -Z, right +X, roof +Y
     * @param ammunition
     *            performance of the actual round, including weapon and platform overrides
     * @param blocked
     *            whether armour stopped the round
     */
    void onImpact(LivingEntity attacker, Entity target, String part, Vec3 outwardNormal, AmmunitionArmor ammunition, boolean blocked);
}
