package com.wolffsmod.npcs.teams;

import net.minecraft.world.phys.Vec3;

/**
 * What a commander told a squad to do: go somewhere and act there.
 *
 * @param target
 *            where the squad goes, in world coordinates
 * @param radius
 *            blocks around the target the squad spreads over once there
 */
public record SquadTask(Kind kind, Vec3 target, double radius)
{
    /** What a squad does at its target. */
    public enum Kind
    {
        /** Take an objective the side does not hold. */
        ATTACK,
        /** Hold an objective the side holds against an enemy closing in. */
        DEFEND,
        /** Guard an objective nobody threatens. */
        GUARD,
        /** Seek out the enemy where they were last seen. */
        HUNT,
        /**
         * Cover an objective from a distance instead of driving onto it, as armour does while the infantry go in: its guns
         * reach the objective from open ground, where the anti-tank weapons waiting on it do not reach the armour.
         */
        OVERWATCH,
        /** Circle over a place, as aircraft and ships do. */
        PATROL,
        /** Stay where it is, as AA guns do. */
        HOLD
    }

    public SquadTask
    {
        radius = Math.max(2D, radius);
    }
}
