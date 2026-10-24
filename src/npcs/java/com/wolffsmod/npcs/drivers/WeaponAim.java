package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.equipment.WeaponBallistics;
import com.wolffsmod.npcs.combat.ProjectileAim;
import com.wolffsmod.npcs.sim.TargetMotion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

/**
 * Aims a driveable's weapons: the direction to launch a round along to meet a target, and where a dropped bomb comes
 * down. Rounds move as Flan's moves them, a tick at a time, then lose speed to drag and fall by their gravity. No world
 * access.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeaponAim
{
    /** Most ticks a dropped bomb is followed down. */
    private static final int MAX_FALL_TICKS = 400;

    /**
     * @param displacement
     *            from the muzzle to the point of the target aimed at
     * @param lead
     *            whether to lead the target by its motion
     * @param accelerating
     *            whether that motion includes how the target speeds up and turns, rather than its velocity alone
     * @param arc
     *            whether to allow for the round's gravity and drag
     * @return the unit direction to launch along to meet the target
     */
    static Vec3 launch(Vec3 displacement, TargetMotion motion, WeaponBallistics ballistics, boolean lead, boolean accelerating, boolean arc)
    {
        double gravity = arc ? ballistics.gravity() : 0D;
        double drag = arc ? ballistics.drag() : 1D;
        if (!lead)
            return ProjectileAim.direction(displacement, Vec3.ZERO, ballistics.speed(), gravity, drag, false);
        if (!accelerating)
            return ProjectileAim.direction(displacement, motion.velocity(), ballistics.speed(), gravity, drag, false);
        return ProjectileAim.solve(displacement, motion, ballistics.speed(), gravity, drag, false).direction();
    }

    /**
     * Where a dropped bomb comes down, and when.
     *
     * @param impact
     *            where it reaches the height followed down to, or where it is after the longest fall followed
     * @param ticks
     *            ticks it falls for
     */
    public record Fall(Vec3 impact, int ticks)
    {}

    /** @return where a bomb dropped from a point at a velocity comes down to a height, or where it is after the longest fall followed */
    static Vec3 impact(Vec3 origin, Vec3 velocity, double gravity, double drag, double floor)
    {
        return fall(origin, velocity, gravity, drag, floor).impact();
    }

    /** @return where and after how many ticks a bomb dropped from a point at a velocity comes down to a height */
    public static Fall fall(Vec3 origin, Vec3 velocity, double gravity, double drag, double floor)
    {
        Vec3 position = origin;
        Vec3 motion = velocity;
        int tick = 0;
        while (tick < MAX_FALL_TICKS && position.y > floor)
        {
            position = position.add(motion);
            motion = motion.scale(drag).subtract(0D, gravity, 0D);
            tick++;
        }
        return new Fall(position, tick);
    }
}
