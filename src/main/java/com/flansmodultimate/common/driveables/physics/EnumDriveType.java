package com.flansmodultimate.common.driveables.physics;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Explicit drive layout for ground vehicles, read from the optional
 * {@code DriveType} content pack key.
 *
 * <p>This is deliberately not inferred from the legacy {@code Tank} and
 * {@code FourWheelDrive} booleans at parse time. The resolver records whether a
 * value was authored explicitly so that legacy traction selection, which keys
 * only off {@code FourWheelDrive}, stays byte-identical for packs that never
 * declare a drive type.
 */
public enum EnumDriveType
{
    /** Rear wheel drive. Only the front wheel pair is treated as undriven. */
    RWD(false, 0.85F, 0.80F),
    /** Front wheel drive. */
    FWD(false, 0.80F, 0.85F),
    /** All wheel drive; the legacy {@code FourWheelDrive true} equivalent. */
    AWD(true, 1F, 1.05F),
    /** Continuous tracks; the legacy {@code Tank true} equivalent. */
    TRACKED(true, 1.1F, 1.25F);

    private final boolean drivesAllWheels;
    private final float tractionFactor;
    private final float slopeTractionFactor;

    EnumDriveType(boolean drivesAllWheels, float tractionFactor, float slopeTractionFactor)
    {
        this.drivesAllWheels = drivesAllWheels;
        this.tractionFactor = tractionFactor;
        this.slopeTractionFactor = slopeTractionFactor;
    }

    /** Whether every configured wheel contributes propulsion, as legacy {@code FourWheelDrive} did. */
    public boolean drivesAllWheels()
    {
        return drivesAllWheels;
    }

    /**
     * Multiplier on the launch-limited tractive force. Only consumed by the new
     * ground propulsion model; it never affects legacy propulsion.
     */
    public float tractionFactor()
    {
        return tractionFactor;
    }

    /** Gameplay traction contribution used by the derived uphill response. */
    public float slopeTractionFactor()
    {
        return slopeTractionFactor;
    }

    /**
     * Case-insensitive parse accepting the four canonical names plus the
     * spellings long-lived content packs already use for the same concepts.
     * Returns {@code null} for anything unrecognised so the caller can warn and
     * fall back rather than crashing content loading.
     */
    @Nullable
    public static EnumDriveType parse(@Nullable String raw)
    {
        if (raw == null)
            return null;
        String normalized = raw.trim().replace("-", "").replace("_", "").replace(" ", "").toUpperCase(Locale.ROOT);
        return switch (normalized)
        {
            case "RWD", "REARWHEELDRIVE", "2WD", "REAR" -> RWD;
            case "FWD", "FRONTWHEELDRIVE", "FRONT" -> FWD;
            case "AWD", "4WD", "ALLWHEELDRIVE", "FOURWHEELDRIVE" -> AWD;
            case "TRACKED", "TRACK", "TRACKS", "TANK", "CONTINUOUSTRACK" -> TRACKED;
            default -> null;
        };
    }

    /**
     * Legacy inference used when no {@code DriveType} is authored. Tracks win
     * over four wheel drive because a legacy {@code Tank} is always tracked.
     */
    public static EnumDriveType infer(boolean tank, boolean fourWheelDrive)
    {
        if (tank)
            return TRACKED;
        return fourWheelDrive ? AWD : RWD;
    }
}
