package com.flansmodultimate.common.driveables.visual;

import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.types.DriveableType;
import org.jetbrains.annotations.Nullable;

/**
 * Everything a driveable model reads from the thing it draws. A live driveable entity is one such source; a stand-in
 * animated by other code, such as a model worn by another entity, is another. Angles are in degrees.
 */
public interface DriveableVisual
{
    @Nullable
    DriveableType getConfigType();

    boolean isPartIntact(@Nullable EnumDriveablePart part);

    /** @return the seat's aim, or {@code null} when the seat is not there: its registered gun is then not drawn */
    @Nullable
    SeatVisual getSeat(int index);

    /** @return the loaded inventory, or {@code null} to draw every ammunition part and no mecha tools */
    @Nullable
    DriveableData getDriveableData();

    float getTurretYaw();

    float getPrevTurretYaw();

    float getTurretPitch();

    float getPrevTurretPitch();

    /** @return progress of the main gun's recoil stroke, from 0 to 1 */
    float getRecoilProgress();

    float getThrottle();

    int getInputMask();

    int getDriveableMode();

    boolean isGearDeployed();

    boolean isDoorOpen();

    boolean isWingFolded();

    boolean isVarFlare();

    /** @return a counter that advances once per game tick, used to step animations */
    int getVisualTick();

    // IT-1 missile tank launcher

    default boolean isReloadingDrakon()
    {
        return false;
    }

    default boolean isCanFireIT1()
    {
        return true;
    }

    default float getIT1DoorAngle()
    {
        return 0F;
    }

    default float getPrevIT1DoorAngle()
    {
        return 0F;
    }

    default float getIT1ArmAngle()
    {
        return 0F;
    }

    default float getPrevIT1ArmAngle()
    {
        return 0F;
    }

    default float getIT1RailAngle()
    {
        return 0F;
    }

    default float getPrevIT1RailAngle()
    {
        return 0F;
    }
}
