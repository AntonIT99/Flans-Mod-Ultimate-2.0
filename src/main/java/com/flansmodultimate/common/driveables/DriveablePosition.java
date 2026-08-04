package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** A local driveable position and the damageable part it is attached to. */
@Getter
public class DriveablePosition
{
    private final Vector3f position;
    private final EnumDriveablePart part;

    public DriveablePosition(@NotNull Vector3f position, EnumDriveablePart part)
    {
        this.position = new Vector3f(position.x, position.y, position.z);
        this.part = part == null ? EnumDriveablePart.CORE : part;
    }

    public static DriveablePosition fromModelCoordinates(float x, float y, float z, EnumDriveablePart part)
    {
        return new DriveablePosition(new Vector3f(x / 16F, y / 16F, z / 16F), part);
    }
}
