package com.flansmodultimate.common.guns;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The swing a custom melee attack takes. {@link #FORWARD} is the default {@code AddNode} path; the others are the
 * optional directional swings chosen by holding a movement key while attacking, as in the Labjac Edition.
 */
@RequiredArgsConstructor
public enum EnumMeleeDirection
{
    FORWARD(""), LEFT("Left"), RIGHT("Right"), UP("Up");

    /** The word used in the definition keys of this swing, such as {@code MeleeLeftTime} or {@code AddLeftNode} */
    @Getter
    private final String configName;

    /**
     * Picks the swing for the movement keys held, with the Labjac Edition priority: strafe left, strafe right, then forward.
     */
    public static EnumMeleeDirection fromMovement(boolean left, boolean right, boolean forward)
    {
        if (left)
            return LEFT;
        if (right)
            return RIGHT;
        if (forward)
            return UP;
        return FORWARD;
    }

    /** @return the direction with this ordinal, or {@link #FORWARD} for an unknown one */
    public static EnumMeleeDirection byOrdinal(int ordinal)
    {
        EnumMeleeDirection[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FORWARD;
    }
}
