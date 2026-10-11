package com.flansmodultimate.common.driveables.visual;

import com.flansmodultimate.common.driveables.EnumPlaneMode;
import com.flansmodultimate.common.driveables.ValkyrieAnimation;
import org.jetbrains.annotations.Nullable;

/** Propeller, rotor and control-surface motion of an aircraft. */
public interface PlaneVisual extends DriveableVisual
{
    EnumPlaneMode getPlaneMode();

    float getPropellerAngle();

    float getPrevPropellerAngle();

    /**
     * @param speedRatio
     *            speed of the rotor relative to the main one
     * @return rotor angle in degrees at the given partial tick
     */
    float getRotorRenderAngle(float partialTick, float speedRatio);

    float getFlapYaw();

    float getPrevFlapYaw();

    float getFlapPitchLeft();

    float getPrevFlapPitchLeft();

    float getFlapPitchRight();

    float getPrevFlapPitchRight();

    /** @return the transforming-fighter animation, or {@code null} for an ordinary aircraft */
    @Nullable
    ValkyrieAnimation getValkyrieAnimation();
}
