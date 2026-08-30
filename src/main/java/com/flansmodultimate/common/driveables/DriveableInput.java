package com.flansmodultimate.common.driveables;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Compact input flags shared by driveable controllers and the server runtime.
 *
 * <p>The values deliberately describe intent rather than movement. The server
 * remains responsible for applying acceleration, rotation, weapon delays and
 * all other state changes.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableInput
{
    public static final int FORWARD = 1;
    public static final int BACKWARD = 1 << 1;
    public static final int LEFT = 1 << 2;
    public static final int RIGHT = 1 << 3;
    public static final int ASCEND = 1 << 4;
    public static final int DESCEND = 1 << 5;
    public static final int PRIMARY_FIRE = 1 << 6;
    public static final int SECONDARY_FIRE = 1 << 7;
    public static final int ROLL_LEFT = 1 << 8;
    public static final int ROLL_RIGHT = 1 << 9;
    public static final int TOGGLE_GEAR = 1 << 10;
    public static final int TOGGLE_DOOR = 1 << 11;
    public static final int TOGGLE_MODE = 1 << 12;
    public static final int FLARE = 1 << 13;
    public static final int EXIT = 1 << 14;
    public static final int MENU = 1 << 15;
    public static final int TRIM = 1 << 16;
    public static final int BRAKE = 1 << 17;
    /** Client camera/controller scheme toggle. Synced for compatibility only. */
    public static final int CONTROL_MODE = 1 << 18;
    /** Persistent ground-vehicle throttle lever, independent of the forward pedal. */
    public static final int THROTTLE_INCREASE = 1 << 19;
    /** Persistent ground-vehicle throttle lever, independent of the reverse pedal. */
    public static final int THROTTLE_DECREASE = 1 << 20;
    /** Cycle to the next free, intact seat. The server selects the destination. */
    public static final int CHANGE_SEAT = 1 << 21;

    public static final int VALID_MASK = (1 << 22) - 1;
    public static final int CONTINUOUS_MASK = FORWARD | BACKWARD | LEFT | RIGHT | ASCEND | DESCEND
        | PRIMARY_FIRE | SECONDARY_FIRE | ROLL_LEFT | ROLL_RIGHT | BRAKE
        | THROTTLE_INCREASE | THROTTLE_DECREASE;
    public static final int EDGE_TRIGGERED_MASK = VALID_MASK & ~CONTINUOUS_MASK;

    public static int sanitize(int mask)
    {
        return mask & VALID_MASK;
    }

    public static boolean isDown(int mask, int input)
    {
        return (mask & input) != 0;
    }

    /** Maps the legacy controller key IDs retained by {@code IControllable}. */
    public static int forLegacyKey(int key)
    {
        return switch (key)
        {
            case 0 -> FORWARD;
            case 1 -> BACKWARD;
            case 2 -> LEFT;
            case 3 -> RIGHT;
            case 4 -> ASCEND;
            case 5 -> DESCEND;
            case 6 -> EXIT;
            case 7 -> MENU;
            case 8 -> SECONDARY_FIRE;
            case 9 -> PRIMARY_FIRE;
            case 10 -> CONTROL_MODE;
            case 11 -> ROLL_LEFT;
            case 12 -> ROLL_RIGHT;
            case 13 -> TOGGLE_GEAR;
            case 14 -> TOGGLE_DOOR;
            case 15 -> TOGGLE_MODE;
            case 16 -> TRIM;
            case 18 -> FLARE;
            default -> 0;
        };
    }
}
