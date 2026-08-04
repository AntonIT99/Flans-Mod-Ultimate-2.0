package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Muzzle definition relative to a driveable part or pilot gun. */
@Getter
public final class ShootPoint
{
    private final DriveablePosition rootPos;
    private final Vector3f offPos;

    public ShootPoint(@NotNull DriveablePosition rootPos, @NotNull Vector3f offPos)
    {
        this.rootPos = rootPos;
        this.offPos = new Vector3f(offPos.x, offPos.y, offPos.z);
    }
}
