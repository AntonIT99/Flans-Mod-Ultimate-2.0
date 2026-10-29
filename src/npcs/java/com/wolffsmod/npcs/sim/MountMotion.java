package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Angle arithmetic of turrets and hulls, in degrees and Minecraft's conventions. No world access. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MountMotion
{
    /** Misalignment up to which a vehicle keeps its full speed. */
    public static final float FULL_SPEED_ANGLE = 20F;
    /** Misalignment from which a vehicle is down to its slowest. */
    public static final float SLOWEST_ANGLE = 90F;

    /**
     * Turns an aim toward a wanted one by at most the given steps. A mount with a limited traverse never swings through
     * the arc it cannot reach, even when that would be the shorter way round.
     *
     * @param mount
     *            the limits to respect, or null for a mount that turns and elevates freely
     */
    public static MountAim approach(MountAim current, MountAim wanted, @Nullable WeaponMount mount, float yawStep, float pitchStep)
    {
        MountAim goal = mount == null ? wanted : mount.clamp(wanted);
        float yaw;
        if (mount == null || mount.turnsFreely())
            yaw = Mth.wrapDegrees(current.yaw() + Mth.clamp(Mth.wrapDegrees(goal.yaw() - current.yaw()), -yawStep, yawStep));
        else
        {
            float centre = mount.restYaw();
            float from = Mth.wrapDegrees(current.yaw() - centre);
            float to = Mth.wrapDegrees(goal.yaw() - centre);
            yaw = centre + from + Mth.clamp(to - from, -yawStep, yawStep);
        }
        return new MountAim(yaw, current.pitch() + Mth.clamp(goal.pitch() - current.pitch(), -pitchStep, pitchStep));
    }

    /** @return how far apart two aims are: the larger of their yaw and pitch differences */
    public static float error(MountAim current, MountAim wanted)
    {
        return Math.max(Math.abs(Mth.wrapDegrees(wanted.yaw() - current.yaw())), Math.abs(wanted.pitch() - current.pitch()));
    }

    /** @return the yaw reached by turning from one yaw toward another by at most the given step */
    public static float turn(float current, float wanted, float step)
    {
        return Mth.wrapDegrees(current + Mth.clamp(Mth.wrapDegrees(wanted - current), -step, step));
    }

    /**
     * @param misalignment
     *            degrees between where the hull points and where the vehicle wants to go
     * @param floor
     *            share of its speed the vehicle keeps at its slowest, from 0 to 1
     * @return the share of its speed the vehicle keeps: all of it while roughly aligned, falling to the floor
     */
    public static float throttle(float misalignment, float floor)
    {
        float angle = Math.abs(Mth.wrapDegrees(misalignment));
        float lowest = Mth.clamp(floor, 0F, 1F);
        if (angle <= FULL_SPEED_ANGLE)
            return 1F;
        if (angle >= SLOWEST_ANGLE)
            return lowest;
        return Mth.lerp((angle - FULL_SPEED_ANGLE) / (SLOWEST_ANGLE - FULL_SPEED_ANGLE), 1F, lowest);
    }

    /** @return the Minecraft yaw a horizontal direction points at; 0 for a vertical one */
    public static float yaw(Vec3 direction)
    {
        return direction.x * direction.x + direction.z * direction.z < 1.0E-12D ? 0F : (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    /** @return the aim, relative to a hull facing the given yaw, that points along a world direction */
    public static MountAim relative(Vec3 direction, float hullYaw)
    {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
        return new MountAim(Mth.wrapDegrees(yaw(direction) - hullYaw), pitch);
    }

    /** @return the unit world direction of an aim relative to a hull facing the given yaw */
    public static Vec3 direction(MountAim aim, float hullYaw)
    {
        return Vec3.directionFromRotation(aim.pitch(), hullYaw + aim.yaw());
    }
}
