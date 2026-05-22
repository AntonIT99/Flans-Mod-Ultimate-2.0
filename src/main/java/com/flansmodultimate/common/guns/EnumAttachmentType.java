package com.flansmodultimate.common.guns;

public enum EnumAttachmentType
{
    BARREL,
    SIGHTS,
    STOCK,
    GRIP,
    GADGET,
    SLIDE,
    PUMP,
    ACCESSORY,
    GENERIC;

    public static EnumAttachmentType get(String s)
    {
        for (EnumAttachmentType type : values())
        {
            if (type.toString().equalsIgnoreCase(s))
                return type;
        }
        return GENERIC;
    }
}
