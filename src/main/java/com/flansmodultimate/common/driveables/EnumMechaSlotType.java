package com.flansmodultimate.common.driveables;

public enum EnumMechaSlotType
{
    LEFT_TOOL,
    RIGHT_TOOL,
    LEFT_ARM,
    RIGHT_ARM,
    HEAD,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    HIPS,
    LEGS,
    FEET,
    U1,
    U2,
    U3,
    U4,
    U5;

    public boolean accepts(EnumMechaItemType type)
    {
        return type != null && type.getValidSlots().contains(this);
    }
}
