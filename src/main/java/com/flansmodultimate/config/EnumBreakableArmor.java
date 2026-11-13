package com.flansmodultimate.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Armor durability policy. Keep this order compatible with legacy numeric config values and the wire format. */
public enum EnumBreakableArmor
{
    UNBREAKABLE,
    ALL_BREAKABLE,
    CONTENT_PACK;

    /** @return the policy's lower-case name, as commands and Teams rule presets spell it */
    public String word()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<EnumBreakableArmor> fromWord(String word)
    {
        return Arrays.stream(values()).filter(mode -> mode.word().equals(word)).findFirst();
    }
}
