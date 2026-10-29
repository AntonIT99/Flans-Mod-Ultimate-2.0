package com.flansmodultimate.common.driveables;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** HP-scaled field repairs and bounded crew penalties. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableRepairRules
{
    public static float toolAmount(float maximum, int configured)
    {
        return Math.min(configured, Math.max(1F, maximum * 0.05F));
    }

    public static float rebuildHealth(EnumDriveablePart type, float maximum)
    {
        return switch (type)
        {
            case ERA, ERA_2, ERA_3, APS, ADS -> maximum;
            default -> Math.min(maximum, Math.max(1F, maximum * 0.1F));
        };
    }

    public static float timedAmount(EnumDriveablePart type, float maximum, boolean ship)
    {
        boolean fast = type.name().contains("TRACK") || type.name().contains("WHEEL") || type.name().startsWith("ENGINE");
        return Math.max(0.25F, maximum * (ship || fast ? 0.025F : 0.015F));
    }

    public static int shipInterval(float localCrew, float totalCrew)
    {
        return Math.round(60F - 40F * (0.25F * fraction(localCrew) + 0.75F * fraction(totalCrew)));
    }

    public static float carrierAmount(float maximum)
    {
        return Math.max(0.25F, maximum * 0.015F);
    }

    public static float buoyancyAmount(float maximum)
    {
        return Math.max(0.25F, maximum / 60F);
    }

    public static int buoyancyInterval(float totalCrew)
    {
        return Math.round(60F - 40F * fraction(totalCrew));
    }

    private static float fraction(float value)
    {
        return Float.isFinite(value) ? Math.max(0F, Math.min(1F, value)) : 0F;
    }
}
