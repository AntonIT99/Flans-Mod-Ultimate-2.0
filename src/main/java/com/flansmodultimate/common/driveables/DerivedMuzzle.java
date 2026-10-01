package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A muzzle position measured from a loaded model instead of read from a type file.
 *
 * <p>The position is in type-file units and convention: model pixels, Y up, and
 * the lateral axis already mirrored, so it can be compared with an authored
 * {@code ShootPointPrimary} or {@code GunOrigin} line and pasted into one
 * without further conversion.</p>
 *
 * @param seatIndex the passenger seat this muzzle belongs to, or {@code -1} for the
 *                  driveable's own barrel
 * @param position  the muzzle with the gun in its rest pose
 * @param pivot     the point a seat gun turns round, in the same terms as {@code position};
 *                  {@code null} for the barrel or when the model gives none
 */
public record DerivedMuzzle(int seatIndex, @NotNull String label, @NotNull Vector3f position,
                            @Nullable Vector3f pivot)
{
    public DerivedMuzzle(int seatIndex, @NotNull String label, @NotNull Vector3f position)
    {
        this(seatIndex, label, position, null);
    }

    public boolean isBarrel()
    {
        return seatIndex < 0;
    }
}
