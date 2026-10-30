/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import com.flansmodultimate.common.entity.ProneSupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

/**
 * Body postures that change how a living entity handles its guns. A prone entity fires with its gun's prone spread, recovers
 * from recoil faster than a sneaking one, takes recoil and knockback as a sneaking one does, and holds its gun forward along
 * its lying body. Players lie prone with the Go Prone key; any entity in the vanilla crawling pose on land counts as prone too.
 * Addons whose entities lie down another way, such as through their own animation, register a rule here.
 */
@ApiStatus.Experimental
public final class FlansPostures
{
    private FlansPostures()
    {}

    /**
     * Registers a rule that calls entities prone. Register it on both sides, during common setup: the server uses it for
     * accuracy, the client to pose the arms of an entity holding a gun. An entity a rule calls prone is drawn as if its
     * renderer laid it flat on its front, head forward. Registering the same rule twice has no effect.
     *
     * @param rule
     *            tested on the server and render threads; must be cheap and must not change the entity
     */
    public static void addProneRule(Predicate<LivingEntity> rule)
    {
        ProneSupport.addRule(rule);
    }

    public static void removeProneRule(Predicate<LivingEntity> rule)
    {
        ProneSupport.removeRule(rule);
    }

    /** @return whether the entity lies prone, in the crawling pose on land or by a registered rule; either side */
    public static boolean isProne(LivingEntity entity)
    {
        return ProneSupport.isProne(entity);
    }
}
