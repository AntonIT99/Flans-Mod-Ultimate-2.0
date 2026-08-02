package com.flansmodultimate.common.teams;

import lombok.Getter;

import java.util.Locale;

@Getter
public enum LoadoutSlot
{
    PRIMARY("Primary"),
    SECONDARY("Secondary"),
    SPECIAL("Special"),
    MELEE("Melee"),
    ARMOUR("Armour");

    private final String displayName;

    LoadoutSlot(String displayName) { this.displayName = displayName; }

    public static LoadoutSlot fromConfigKey(String key)
    {
        return valueOf(key.substring(3).toUpperCase(Locale.ROOT));
    }
}
