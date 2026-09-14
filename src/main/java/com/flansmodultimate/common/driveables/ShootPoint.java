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
    /**
     * Set on a point installed by the shoot-point debug command rather than read
     * from a type file. Only diagnostics look at this: it colours the in-world
     * marker so an overridden muzzle cannot be mistaken for an authored one.
     */
    private final boolean debugOverride;

    public ShootPoint(@NotNull DriveablePosition rootPos, @NotNull Vector3f offPos)
    {
        this(rootPos, offPos, false);
    }

    public ShootPoint(@NotNull DriveablePosition rootPos, @NotNull Vector3f offPos, boolean debugOverride)
    {
        this.rootPos = rootPos;
        this.offPos = new Vector3f(offPos.x, offPos.y, offPos.z);
        this.debugOverride = debugOverride;
    }
}
