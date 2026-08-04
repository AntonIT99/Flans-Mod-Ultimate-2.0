package com.flansmodultimate.common.driveables;

import java.util.List;
import java.util.Locale;

public enum EnumMechaItemType
{
    UPGRADE,
    TOOL,
    ARM_UPGRADE,
    LEG_UPGRADE,
    HEAD_UPGRADE,
    SHOULDER_UPGRADE,
    FEET_UPGRADE,
    HIPS_UPGRADE,
    NOTHING;

    public static EnumMechaItemType parse(String value)
    {
        if (value == null)
            return NOTHING;
        String normalized = value.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        for (EnumMechaItemType type : values())
        {
            if (type.name().replace("_", "").toLowerCase(Locale.ROOT).equals(normalized))
                return type;
        }
        return NOTHING;
    }

    public List<EnumMechaSlotType> getValidSlots()
    {
        return switch (this)
        {
            case UPGRADE -> List.of(EnumMechaSlotType.U1, EnumMechaSlotType.U2, EnumMechaSlotType.U3, EnumMechaSlotType.U4, EnumMechaSlotType.U5);
            case TOOL -> List.of(EnumMechaSlotType.LEFT_TOOL, EnumMechaSlotType.RIGHT_TOOL);
            case ARM_UPGRADE -> List.of(EnumMechaSlotType.LEFT_ARM, EnumMechaSlotType.RIGHT_ARM);
            case LEG_UPGRADE -> List.of(EnumMechaSlotType.LEGS);
            case HEAD_UPGRADE -> List.of(EnumMechaSlotType.HEAD);
            case SHOULDER_UPGRADE -> List.of(EnumMechaSlotType.LEFT_SHOULDER, EnumMechaSlotType.RIGHT_SHOULDER);
            case FEET_UPGRADE -> List.of(EnumMechaSlotType.FEET);
            case HIPS_UPGRADE -> List.of(EnumMechaSlotType.HIPS);
            case NOTHING -> List.of();
        };
    }
}
