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
 * @param turretOffset
 *            the seat's rotated offset, added to where the occupant sits and turned with the main turret whether or not
 *            the seat is on it, in the same frame as {@code position}; a seat on the barrel also elevates it with the gun
 * @param originOffset
 *            the turret origin offset of a seat on the turret, added to where the occupant sits and turned with the
 *            turret but not elevated with the gun, in the same frame as {@code position}; zero for a seat on the hull
 * @param part
 *            short name of the part the seat sits in, such as {@code core}, {@code turret} or {@code turret3}: the
 *            occupant is thrown out while it is destroyed and dies with it if it is a turret
 */
@ApiStatus.Experimental
public record WeaponMount(int seat, boolean driver, boolean onTurret, MountAim minimum, MountAim maximum, float yawSpeed, float pitchSpeed, Vec3 position, Vec3 pivot, Optional<IContentType> gun,
    Vec3 turretOffset, Vec3 originOffset, String part)
{
    /** Part a mount sits in when it names none: the main turret for one that turns with it, else the hull. */
    private static final String HULL = "core";
    private static final String MAIN_TURRET = "turret";
    /** Part whose seats elevate with the main gun as well as turning with the turret. */
    private static final String BARREL = "barrel";

    public WeaponMount
    {
        if (part == null || part.isBlank())
            part = onTurret ? MAIN_TURRET : HULL;
    }

    public WeaponMount(int seat, boolean driver, boolean onTurret, MountAim minimum, MountAim maximum, float yawSpeed, float pitchSpeed, Vec3 position, Vec3 pivot, Optional<IContentType> gun)
    {
        this(seat, driver, onTurret, minimum, maximum, yawSpeed, pitchSpeed, position, pivot, gun, Vec3.ZERO);
    }

    public WeaponMount(int seat, boolean driver, boolean onTurret, MountAim minimum, MountAim maximum, float yawSpeed, float pitchSpeed, Vec3 position, Vec3 pivot, Optional<IContentType> gun,
        Vec3 turretOffset)
    {
        this(seat, driver, onTurret, minimum, maximum, yawSpeed, pitchSpeed, position, pivot, gun, turretOffset, Vec3.ZERO, null);
    }

    /**
     * @param turretYaw
     *            yaw of the main turret relative to the hull, in degrees, positive to the right
     * @return where the occupant sits with the turret turned that far and its gun level, in the same frame as
     *         {@code position}
     */
    public Vec3 positionAt(float turretYaw)
    {
        return positionAt(turretYaw, 0F);
    }

    /**
     * Places the occupant as a driveable seats its rider: a seat on the turret is carried round the turret's pivot, and one
     * on the barrel is also elevated with the gun round that pivot. The seat's own gun does not move the occupant.
     *
     * @param turretYaw
     *            yaw of the main turret relative to the hull, in degrees, positive to the right
     * @param turretPitch
     *            pitch of the main gun, in degrees, positive downwards as {@link MountAim} counts it
     * @return where the occupant sits with the turret turned and the gun elevated that far, in the same frame as
     *         {@code position}
     */
    public Vec3 positionAt(float turretYaw, float turretPitch)
    {
        float pitch = BARREL.equals(part) ? turretPitch : 0F;
        Vec3 offset = turn(elevate(turretOffset, pitch), turretYaw);
        if (!onTurret)
            return position.add(offset);
        return pivot.add(turn(elevate(position.subtract(pivot), pitch).add(originOffset), turretYaw)).add(offset);
    }

    private static Vec3 turn(Vec3 vector, float yaw)
    {
        if (yaw == 0F)
            return vector;
        double radians = Math.toRadians(yaw);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new Vec3(vector.x * cosine - vector.z * sine, vector.y, vector.x * sine + vector.z * cosine);
    }

    /** Pitches a vector about the width, a positive pitch carrying what lies ahead, along -Z, downwards. */
    private static Vec3 elevate(Vec3 vector, float pitch)
    {
        if (pitch == 0F)
            return vector;
        double radians = Math.toRadians(pitch);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new Vec3(vector.x, vector.y * cosine + vector.z * sine, vector.z * cosine - vector.y * sine);
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
