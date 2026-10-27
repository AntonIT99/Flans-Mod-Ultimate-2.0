package com.flansmodultimate.common.driveables;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Labjac repair rates, with bounded crew fractions and safe defaults for uncrewed parts. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableRepairRules
{
    public static float rebuildHealth(EnumDriveablePart type, float maximum)
    {
        return switch (type)
        {
            case ERA, ERA_2, ERA_3, APS, ADS -> maximum;
            default -> Math.min(maximum, Math.max(1F, (int) (1F + maximum / 115F)));
        };
    }

    public static float timedAmount(EnumDriveablePart type, float maximum, boolean ship)
    {
        boolean fast = type.name().contains("TRACK") || type.name().contains("WHEEL") || type.name().startsWith("ENGINE");
        return 1F + (int) (maximum * (ship || fast ? 0.025F : 0.005F));
    }

    public static int shipInterval(float localCrew, float totalCrew)
    {
        return Math.max(20, (int) (566F - 137F * fraction(localCrew) - 409F * fraction(totalCrew))) + 1;
    }

    public static int buoyancyInterval(float totalCrew)
    {
        return Math.max(20, (int) (400F - 380F * fraction(totalCrew)));
    }

    private static float fraction(float value)
    {
        return Float.isFinite(value) ? Math.max(0F, Math.min(1F, value)) : 0F;
    }
}
