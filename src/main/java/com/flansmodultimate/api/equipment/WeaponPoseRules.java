/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Per-hand rules, selected in priority order USING, FIRING, TARGETING, MOVING, IDLE. DEFAULT delegates to normal weapon
 * behavior in that state. Shot hold time is in ticks, bounded to 0..200. Immutable, readable on either side.
 */
@ApiStatus.Experimental
public record WeaponPoseRules(ArmPoseRule idle, ArmPoseRule moving, ArmPoseRule targeting, ArmPoseRule firing, ArmPoseRule using, int shotHoldTicks)
{
    /** Normal server policy, with the usual one-second firing hold. */
    public static final WeaponPoseRules DEFAULT = new WeaponPoseRules(ArmPoseRule.DEFAULT, ArmPoseRule.DEFAULT, ArmPoseRule.DEFAULT, ArmPoseRule.DEFAULT, ArmPoseRule.DEFAULT, 20);

    /** Conditions whose rule can be edited independently; either side. */
    @ApiStatus.Experimental
    public enum State
    {
        IDLE, MOVING, TARGETING, FIRING, USING
    }

    /** Requires all five rules and bounds the firing hold time. */
    public WeaponPoseRules
    {
        Objects.requireNonNull(idle);
        Objects.requireNonNull(moving);
        Objects.requireNonNull(targeting);
        Objects.requireNonNull(firing);
        Objects.requireNonNull(using);
        shotHoldTicks = Math.max(0, Math.min(200, shotHoldTicks));
    }

    /** Returns the rule for one state; either side. */
    public ArmPoseRule get(State state)
    {
        return switch (state)
        {
            case IDLE -> idle;
            case MOVING -> moving;
            case TARGETING -> targeting;
            case FIRING -> firing;
            case USING -> using;
        };
    }

    /** Returns a copy with one state's rule replaced; either side. */
    public WeaponPoseRules with(State state, ArmPoseRule rule)
    {
        return new WeaponPoseRules(state == State.IDLE ? rule : idle, state == State.MOVING ? rule : moving, state == State.TARGETING ? rule : targeting, state == State.FIRING ? rule : firing,
            state == State.USING ? rule : using, shotHoldTicks);
    }
}
