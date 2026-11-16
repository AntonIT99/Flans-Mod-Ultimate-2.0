package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.entity.Driveable;
import org.joml.Quaternionf;

import net.minecraft.util.Mth;

/** Driveable pitch and roll around a seated rider's feet, in the same order as the driveable renderer. */
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

    /** The renderer turns every driveable model by {@code 180 - yaw} and a rider body by {@code 180 - bodyYaw}; tilt in the model's frame. */
    static Quaternionf forAngles(float bodyYaw, float driveableYaw, float pitch, float roll)
    {
        float modelOffset = Mth.wrapDegrees(bodyYaw - driveableYaw);
        return new Quaternionf().rotateY(modelOffset * Mth.DEG_TO_RAD).rotateZ(pitch * Mth.DEG_TO_RAD).rotateX(roll * Mth.DEG_TO_RAD).rotateY(-modelOffset * Mth.DEG_TO_RAD);
    }
}
