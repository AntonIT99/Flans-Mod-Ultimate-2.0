package com.flansmodultimate.common.driveables;

import com.flansmodultimate.api.DriveControl;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class DriveableInputTest
{
    @Test
    void everyDriveControlHoldsAContinuousInputOfItsOwnAndNoTrigger()
    {
        int seen = 0;
        for (DriveControl control : DriveControl.values())
        {
            int mask = DriveableInput.forControls(EnumSet.of(control));
            assertEquals(1, Integer.bitCount(mask), control.name());
            assertEquals(0, seen & mask, control.name());
            assertEquals(mask, mask & DriveableInput.CONTINUOUS_MASK, control.name());
            seen |= mask;
        }
        assertEquals(DriveableInput.DRIVE_CONTROL_MASK, seen);
        assertEquals(0, DriveableInput.DRIVE_CONTROL_MASK & DriveableInput.TRIGGER_MASK);
        assertEquals(0, DriveableInput.forControls(EnumSet.noneOf(DriveControl.class)));
    }
}
