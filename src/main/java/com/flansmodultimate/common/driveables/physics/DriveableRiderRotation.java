package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.api.driveables.IFlanTiltingMount;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Driveable or tilting-mount pitch and roll around a rider's feet, in the same order as the mount's renderer. */
public final class DriveableRiderRotation
{
    private DriveableRiderRotation()
    {}

    public static Quaternionf at(Driveable driveable, float partialTick, float bodyYaw)
    {
        float yaw = Mth.rotLerp(partialTick, driveable.getPrevYaw(), driveable.getYaw());
        float pitch = Mth.rotLerp(partialTick, driveable.getPrevPitch(), driveable.getPitch());
        float roll = Mth.rotLerp(partialTick, driveable.getPrevRoll(), driveable.getRoll());
        return forAngles(bodyYaw, yaw, pitch, roll);
    }

    /** The rider's tilt on any other mount, or null when it has none. */
    @Nullable
    public static Quaternionf at(@Nullable Entity mount, float partialTick, float bodyYaw)
    {
        if (mount instanceof Seat seat && seat.getDriveable() != null)
            return at(seat.getDriveable(), partialTick, bodyYaw);
        if (mount instanceof IFlanTiltingMount tilting && mount instanceof LivingEntity living)
        {
            Quaternionf tilt = tilting.flansmodultimateRiderTilt(partialTick);
            return tilt == null ? null : inBodyFrame(tilt, bodyYaw, Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot));
        }
        return null;
    }

    /** The renderer turns every driveable model by {@code 180 - yaw}; tilt in the model's frame. */
    static Quaternionf forAngles(float bodyYaw, float driveableYaw, float pitch, float roll)
    {
        return inBodyFrame(new Quaternionf().rotateZ(pitch * Mth.DEG_TO_RAD).rotateX(roll * Mth.DEG_TO_RAD), bodyYaw, driveableYaw);
    }

    /** Re-expresses a tilt of a body turned by {@code 180 - mountYaw} in a rider body turned by {@code 180 - bodyYaw}. */
    static Quaternionf inBodyFrame(Quaternionf tilt, float bodyYaw, float mountYaw)
    {
        float offset = Mth.wrapDegrees(bodyYaw - mountYaw) * Mth.DEG_TO_RAD;
        return new Quaternionf().rotateY(offset).mul(tilt).rotateY(-offset);
    }
}
