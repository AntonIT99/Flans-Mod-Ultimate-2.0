package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Labjac-style ammunition rack cook-off.
 *
 * <p>
 * A destroyed hull weak spot ({@code weakSpot}, {@code weakSpot2}, {@code weakSpot3}) burns the core down, and a destroyed
 * turret weak spot ({@code turretWeak}, {@code turretWeak2}) burns the turret down while it still stands. Every
 * {@link #PULSE_TICKS} ticks the burning part loses {@link #damagePerPulse} hit points, so it is gone after about twice
 * {@code WeakspotCookTime} seconds, as in the fork.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmmoRackCookOff
{
    /** Ticks between two cook-off pulses: the fork's five-tick cycle. */
    public static final int PULSE_TICKS = 5;
    /** The fork doubles the authored cook time. */
    private static final float COOK_TIME_MULTIPLIER = 2F;
    private static final float MIN_COOK_TIME = 0.25F;

    private static final List<EnumDriveablePart> HULL_WEAK_SPOTS = List.of(EnumDriveablePart.WEAK_SPOT, EnumDriveablePart.WEAK_SPOT_2, EnumDriveablePart.WEAK_SPOT_3);
    private static final List<EnumDriveablePart> TURRET_WEAK_SPOTS = List.of(EnumDriveablePart.TURRET_WEAK, EnumDriveablePart.TURRET_WEAK_2);

    /** @return the hit points the burning part loses per pulse: a quarter of its health per doubled cook-time second */
    public static float damagePerPulse(float maxHealth, float cookTimeSeconds)
    {
        float cookTime = Float.isFinite(cookTimeSeconds) ? Math.max(MIN_COOK_TIME, cookTimeSeconds) : MIN_COOK_TIME;
        return Math.max(1F, (float) Math.ceil(maxHealth / (4F * cookTime * COOK_TIME_MULTIPLIER)));
    }

    /**
     * @return the part burning down because one of its ammunition racks is destroyed: the core before the turret, or null
     *     when no rack has gone up or its part is already gone
     */
    @Nullable
    public static EnumDriveablePart burningPart(@Nullable Map<EnumDriveablePart, DriveablePart> parts)
    {
        if (parts == null)
            return null;
        if (anyRackDestroyed(parts, HULL_WEAK_SPOTS) && isStanding(parts.get(EnumDriveablePart.CORE)))
            return EnumDriveablePart.CORE;
        if (anyRackDestroyed(parts, TURRET_WEAK_SPOTS) && isStanding(parts.get(EnumDriveablePart.TURRET)))
            return EnumDriveablePart.TURRET;
        return null;
    }

    private static boolean anyRackDestroyed(Map<EnumDriveablePart, DriveablePart> parts, List<EnumDriveablePart> racks)
    {
        for (EnumDriveablePart rack : racks)
        {
            DriveablePart part = parts.get(rack);
            if (part != null && part.getMaxHealth() > 0F && part.isDestroyed())
                return true;
        }
        return false;
    }

    private static boolean isStanding(@Nullable DriveablePart part)
    {
        return part != null && part.getMaxHealth() > 0F && !part.isDestroyed();
    }
}
