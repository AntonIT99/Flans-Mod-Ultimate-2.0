package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.drivers.WeaponAim;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.BiFunction;

/** Bomb release tactics shared by model aircraft and pilots of real driveables. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcBombing
{
    private static final double MIN_HEIGHT = 4D;
    /** Within one block, take the hit rather than waiting for a marginal improvement. */
    private static final double PRECISE_ERROR_SQUARED = 1D;
    private static final double MIN_IMPROVEMENT_SQUARED = 0.01D;

    /** A flat-ground release point to approach, the predicted target position, and whether to release this tick. */
    public record Solution(Vec3 release, Vec3 meeting, boolean drop)
    {}

    public static Solution solve(Entity carrier, Vec3 origin, double gravity, double drag, double radius, LivingEntity target, Vec3 lead)
    {
        Vec3 velocity = carrier.getDeltaMovement();
        WeaponAim.Fall flat = WeaponAim.fall(origin, velocity, gravity, drag, target.getY());
        Vec3 meeting = meeting(target.position(), lead, flat.ticks());
        // Guide the airframe, compensating for a bay that is forward of or beside its centre.
        Vec3 release = WeaponAim.releasePoint(carrier.position(), flat.impact(), meeting);
        // Most of the approach needs only the cheap flat-ground solution. Trace terrain only inside a viable drop window.
        if (!flat.landed() || origin.y < target.getY() + MIN_HEIGHT || flat.impact().distanceToSqr(meeting) > radius * radius)
            return new Solution(release, meeting, false);
        BiFunction<Vec3, Vec3, Optional<Vec3>> trace = (from, to) -> {
            HitResult hit = carrier.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, carrier));
            return hit.getType() == HitResult.Type.MISS ? Optional.empty() : Optional.of(hit.getLocation());
        };
        WeaponAim.Fall current = WeaponAim.fall(origin, velocity, gravity, drag, target.getY(), trace);
        meeting = meeting(target.position(), lead, current.ticks());
        double error = current.impact().distanceToSqr(meeting);
        if (!current.landed() || error > radius * radius)
            return new Solution(release, meeting, false);
        Vec3 nextOrigin = origin.add(velocity);
        WeaponAim.Fall next = WeaponAim.fall(nextOrigin, velocity, gravity, drag, target.getY(), trace);
        Vec3 nextMeeting = meeting(target.position(), lead, 1D + next.ticks());
        double nextError = next.landed() && nextOrigin.y >= target.getY() + MIN_HEIGHT ? next.impact().distanceToSqr(nextMeeting) : Double.POSITIVE_INFINITY;
        return new Solution(release, meeting, releaseNow(error, nextError, radius));
    }

    private static Vec3 meeting(Vec3 target, Vec3 lead, double ticks)
    {
        return target.add(lead.x * ticks, 0D, lead.z * ticks);
    }

    /** Keep the full fallback radius; defer only while a materially closer drop remains available on the next tick. */
    static boolean releaseNow(double errorSquared, double nextErrorSquared, double radius)
    {
        return errorSquared <= radius * radius && (errorSquared <= PRECISE_ERROR_SQUARED || nextErrorSquared > radius * radius
            || nextErrorSquared >= errorSquared - MIN_IMPROVEMENT_SQUARED);
    }
}
