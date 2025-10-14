package com.wolffsarmormod.common.guns;

public enum EnumSpreadPattern
{
    CIRCLE,
    CUBE,
    TRIANGLE,
    HORIZONTAL,
    VERTICAL;

    public static EnumSpreadPattern get(String s)
    {
        s = s.toLowerCase();
        return switch (s)
        {
            case "circle" -> CIRCLE;
            case "cube" -> CUBE;
            case "triangle" -> TRIANGLE;
            case "horizontal" -> HORIZONTAL;
            case "vertical" -> VERTICAL;
            default -> null;
        };
    }
}
