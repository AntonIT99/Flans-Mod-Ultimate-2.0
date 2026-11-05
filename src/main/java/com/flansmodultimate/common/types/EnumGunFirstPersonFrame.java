package com.flansmodultimate.common.types;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Legacy RenderGun view frames and authored-model alignment categories. */
public enum EnumGunFirstPersonFrame
{
    /** Ultimate 1.7.10: vanilla and Forge equipped-item frames, then the 1.7.10 aiming movement. */
    FLANS_1710("1.7.10", 0.0009249F),
    /** Flan's Mod 1.12.2: a smaller, further placement without Forge's equipped-item frame. */
    FLANS_1122("1.12.2", -0.005F),
    /** Tyrants and Plebeians (Labjac fork): the 1.7.10 frame with retuned resting and aiming positions. */
    FLANS_TAP("Tap", 0.0009249F);

    private final String displayName;
    /** Sideways (model z) position of the screen centre at full aim, in frame units before the model scale. */
    private final float aimCentreZ;

    EnumGunFirstPersonFrame(String displayName, float aimCentreZ)
    {
        this.displayName = displayName;
        this.aimCentreZ = aimCentreZ;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    /** Z shift that keeps a model authored for {@code authored} centred at full aim when it is rendered with this frame. */
    public float aimZCorrectionFrom(EnumGunFirstPersonFrame authored)
    {
        return aimCentreZ - authored.aimCentreZ;
    }

    /** Parses an authored frame; old Tap values normalize to 1.7.10. Returns null when unknown. */
    @Nullable
    public static EnumGunFirstPersonFrame parse(@Nullable String value)
    {
        if (value == null || value.isBlank())
            return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (EnumGunFirstPersonFrame frame : values())
        {
            if (frame.displayName.toLowerCase(Locale.ROOT).equals(normalized) || frame.name().toLowerCase(Locale.ROOT).equals(normalized))
                return frame == FLANS_TAP ? FLANS_1710 : frame;
        }
        return switch (normalized)
        {
            case "1710", "1.7", "legacy" -> FLANS_1710;
            case "1122", "1.12" -> FLANS_1122;
            case "labjac" -> FLANS_1710;
            default -> null;
        };
    }
}
