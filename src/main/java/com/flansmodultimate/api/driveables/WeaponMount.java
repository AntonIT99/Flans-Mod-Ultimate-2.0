/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import com.flansmodultimate.api.content.IContentType;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A seat of a driveable definition, or the gun of an AA gun definition, as something that can be aimed: how far it
 * turns, how fast, and where its occupant sits. Read from the definition alone, on either side after content loading.
 *
 * @param seat
 *            seat index, 0 for the driver, whose aim turns the main turret
 * @param driver
 *            whether this is the driver's seat
 * @param onTurret
 *            whether the seat and its gun turn with the main turret
 * @param minimum
 *            leftmost and highest aim; pitch is positive downwards, so this is the smaller pitch
 * @param maximum
 *            rightmost and lowest aim. A yaw span of 360 degrees or more means the mount turns freely
 * @param yawSpeed
 *            traverse speed in degrees per tick
 * @param pitchSpeed
 *            elevation speed in degrees per tick
 * @param position
 *            where the occupant sits with the turret at rest, in the static model preview frame: blocks, Y up,
 *            forward -Z, relative to the model origin
 * @param pivot
 *            the point a seat on the turret is carried round as the turret turns, in the same frame as
 *            {@code position}; the position itself for a seat on the hull
 * @param gun
 *            the gun definition the seat mans, if any
 */
@ApiStatus.Experimental
public record WeaponMount(int seat, boolean driver, boolean onTurret, MountAim minimum, MountAim maximum, float yawSpeed, float pitchSpeed, Vec3 position, Vec3 pivot, Optional<IContentType> gun)
{

    /**
     * @param turretYaw
     *            yaw of the main turret relative to the hull, in degrees, positive to the right
     * @return where the occupant sits with the turret turned that far, in the same frame as {@code position}
     */
    public Vec3 positionAt(float turretYaw)
    {
        if (!onTurret || turretYaw == 0F)
            return position;
        double radians = Math.toRadians(turretYaw);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        Vec3 arm = position.subtract(pivot);
        return pivot.add(arm.x * cosine - arm.z * sine, arm.y, arm.x * sine + arm.z * cosine);
    }

    /** @return whether the mount can turn all the way round */
    public boolean turnsFreely()
    {
        return maximum.yaw() - minimum.yaw() >= 360F;
    }

    /** @return the yaw the mount rests at: the middle of a limited traverse, or straight ahead */
    public float restYaw()
    {
        return turnsFreely() ? 0F : (minimum.yaw() + maximum.yaw()) * 0.5F;
    }

    /**
     * Brings an aim within the mount's limits. A yaw outside a limited traverse goes to the nearer end.
     *
     * @param aim
     *            wanted aim relative to the hull
     * @return the nearest aim the mount can reach, with its yaw wrapped to the half-open range from -180 to 180 when
     *         the mount turns freely
     */
    public MountAim clamp(MountAim aim)
    {
        float pitch = Math.max(minimum.pitch(), Math.min(maximum.pitch(), aim.pitch()));
        if (turnsFreely())
            return new MountAim(wrap(aim.yaw()), pitch);
        float centre = restYaw();
        float halfSpan = (maximum.yaw() - minimum.yaw()) * 0.5F;
        return new MountAim(centre + Math.max(-halfSpan, Math.min(halfSpan, wrap(aim.yaw() - centre))), pitch);
    }

    private static float wrap(float degrees)
    {
        float wrapped = degrees % 360F;
        if (wrapped >= 180F)
            wrapped -= 360F;
        if (wrapped < -180F)
            wrapped += 360F;
        return wrapped;
    }
}
