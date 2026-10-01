package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.entity.Plane;
import org.joml.Quaternionf;

import net.minecraft.util.Mth;

/** Aircraft attitude around a rider's feet, in the same order as the plane renderer. */
public final class PlaneRiderRotation
{
    private PlaneRiderRotation() {}

    public static Quaternionf at(Plane plane, float partialTick, float bodyYaw)
    {
        float planeYaw = Mth.rotLerp(partialTick, plane.getPrevYaw(), plane.getYaw());
        float pitch = Mth.rotLerp(partialTick, plane.getPrevPitch(), plane.getPitch());
        float roll = Mth.rotLerp(partialTick, plane.getPrevRoll(), plane.getRoll());
        return forAngles(bodyYaw, plane.getEntityFacingYaw(planeYaw), pitch, roll);
    }

    static Quaternionf forAngles(float bodyYaw, float forwardYaw, float pitch, float roll)
    {
        float bodyOffset = Mth.wrapDegrees(bodyYaw - forwardYaw);
        return new Quaternionf()
            .rotateY((bodyOffset - 90F) * Mth.DEG_TO_RAD)
            .rotateZ(pitch * Mth.DEG_TO_RAD)
            .rotateX(roll * Mth.DEG_TO_RAD)
            .rotateY((90F - bodyOffset) * Mth.DEG_TO_RAD);
    }
}
