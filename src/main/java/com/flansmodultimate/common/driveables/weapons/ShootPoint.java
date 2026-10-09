package com.flansmodultimate.common.driveables.weapons;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.DriveablePosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    /**
     * Where each barrel of a twin or quad mount ends, as offsets in blocks from
     * the point's muzzle, measured off the model while the content loads. The
     * point fires from them in turn. Empty for a point with one barrel.
     */
    private final List<Vector3f> barrels;

    public ShootPoint(@NotNull DriveablePosition rootPos, @NotNull Vector3f offPos)
    {
        this(rootPos, offPos, false);
    }

    public ShootPoint(@NotNull DriveablePosition rootPos, @NotNull Vector3f offPos, boolean debugOverride)
    {
        this(rootPos, offPos, debugOverride, List.of());
    }

    public ShootPoint(@NotNull DriveablePosition rootPos, @NotNull Vector3f offPos, boolean debugOverride, @NotNull List<Vector3f> barrels)
    {
        this.rootPos = rootPos;
        this.offPos = new Vector3f(offPos.x, offPos.y, offPos.z);
        this.debugOverride = debugOverride;
        this.barrels = copyBarrels(barrels);
    }

    /** This point with the given barrel offsets, in blocks from its muzzle. */
    public ShootPoint withBarrels(@NotNull List<Vector3f> offsets)
    {
        return new ShootPoint(rootPos, offPos, debugOverride, offsets);
    }

    /** How many barrels this point fires from in turn, at least one. */
    public int getBarrelCount()
    {
        return Math.max(1, barrels.size());
    }

    /** The offset from the root of one barrel's muzzle: {@link #offPos} for a single barrel. */
    public Vector3f getBarrelOffPos(int barrel)
    {
        return plusBarrel(offPos, barrels, barrel);
    }

    /** Defensive copies, since {@link Vector3f} is mutable; a single barrel is stored as none. */
    public static List<Vector3f> copyBarrels(List<Vector3f> offsets)
    {
        if (offsets.size() < 2)
            return List.of();
        return offsets.stream().map(offset -> new Vector3f(offset.x, offset.y, offset.z)).toList();
    }

    /** {@code base} moved by one barrel's offset, wrapping the index round the barrels. */
    public static Vector3f plusBarrel(Vector3f base, List<Vector3f> barrels, int barrel)
    {
        if (barrels.isEmpty())
            return new Vector3f(base.x, base.y, base.z);
        Vector3f offset = barrels.get(Math.floorMod(barrel, barrels.size()));
        return new Vector3f(base.x + offset.x, base.y + offset.y, base.z + offset.z);
    }
}
