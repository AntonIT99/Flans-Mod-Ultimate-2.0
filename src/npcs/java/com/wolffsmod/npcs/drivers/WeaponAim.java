package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.equipment.WeaponBallistics;
import com.wolffsmod.npcs.combat.NpcSkill;
import com.wolffsmod.npcs.combat.ProjectileAim;
import com.wolffsmod.npcs.sim.TargetMotion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Aims a driveable's weapons: the direction to launch a round along to meet a target, and where a dropped bomb comes
 * down. Bombs follow Flan's collision trace, then drag and gravity, then movement each tick. No world access.
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
     * @param share
     *            share, 0 to 1, of the target's run the NPC's human limits let it allow for
     * @return the unit direction to launch along to meet the target
     */
    static Vec3 launch(Vec3 displacement, TargetMotion motion, WeaponBallistics ballistics, boolean lead, boolean accelerating, boolean arc, float share)
    {
        double gravity = arc ? ballistics.gravity() : 0D;
        double drag = arc ? ballistics.drag() : 1D;
        Vec3 still = ProjectileAim.direction(displacement, Vec3.ZERO, ballistics.speed(), gravity, drag, false);
        if (!lead)
            return still;
        Vec3 led = accelerating
            ? ProjectileAim.solve(displacement, motion, ballistics.speed(), gravity, drag, false).direction()
            : ProjectileAim.direction(displacement, motion.velocity(), ballistics.speed(), gravity, drag, false);
        return NpcSkill.partialLead(share, still, led);
    }

    /**
     * Where a dropped bomb comes down, and when.
     *
     * @param impact
     *            where it reaches the height followed down to, or where it is after the longest fall followed
     * @param ticks
     *            ticks it falls for, including the fraction of the final collision trace
     * @param landed
     *            whether it reaches the target height before the prediction limit
     */
    public record Fall(Vec3 impact, double ticks, boolean landed)
    {}

    /** @return where a bomb dropped from a point at a velocity comes down to a height, or where it is after the longest fall followed */
    static Vec3 impact(Vec3 origin, Vec3 velocity, double gravity, double drag, double floor)
    {
        return fall(origin, velocity, gravity, drag, floor).impact();
    }

    /** @return where and after how many ticks a bomb dropped from a point at a velocity comes down to a height */
    public static Fall fall(Vec3 origin, Vec3 velocity, double gravity, double drag, double floor)
    {
        return fall(origin, velocity, gravity, drag, floor, (from, to) -> Optional.empty());
    }

    /** Follows the same incoming collision traces as the projectile, stopping at terrain before the target's height. */
    public static Fall fall(Vec3 origin, Vec3 velocity, double gravity, double drag, double floor, BiFunction<Vec3, Vec3, Optional<Vec3>> trace)
    {
        Vec3 position = origin;
        Vec3 motion = velocity;
        int tick = 0;
        while (tick < MAX_FALL_TICKS)
        {
            Optional<Vec3> collision = trace.apply(position, position.add(motion));
            if (collision.isPresent())
            {
                double fraction = motion.lengthSqr() > 1.0E-12D ? position.distanceTo(collision.get()) / motion.length() : 0D;
                return new Fall(collision.get(), tick + fraction, true);
            }
            // Bullet traces the incoming velocity before applying forces. Moving first would accumulate an extra
            // tick of horizontal travel and use the wrong fall time, especially with drag or a climbing carrier.
            if (position.y <= floor)
                return new Fall(position, tick, true);
            if (motion.y < 0D && position.y + motion.y <= floor)
            {
                double fraction = (position.y - floor) / -motion.y;
                return new Fall(position.add(motion.scale(fraction)), tick + fraction, true);
            }
            motion = motion.scale(drag).subtract(0D, gravity, 0D);
            position = position.add(motion);
            tick++;
        }
        return new Fall(position, tick, false);
    }

    /** The release point allowing for the distance a bomb carries forward after leaving its bay. */
    public static Vec3 releasePoint(Vec3 origin, Vec3 impact, Vec3 meeting)
    {
        return new Vec3(meeting.x - (impact.x - origin.x), meeting.y, meeting.z - (impact.z - origin.z));
    }

    /** A nearby release point is flown through, so crossing it does not command a sudden turn back toward the bay's drop point. */
    public static Vec3 bombApproach(Vec3 position, Vec3 velocity, Vec3 release)
    {
        Vec3 horizontal = velocity.multiply(1D, 0D, 1D);
        double lookahead = Math.max(16D, horizontal.length() * 10D);
        return release.subtract(position).horizontalDistance() <= lookahead * 2D ? release.add(horizontal.normalize().scale(lookahead)) : release;
    }
}
