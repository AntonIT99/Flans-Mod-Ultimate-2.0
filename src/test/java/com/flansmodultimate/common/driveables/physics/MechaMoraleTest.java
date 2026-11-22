package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MechaMoraleTest
{
    @Test
    void aMountStartsWithFullMoraleAndShrugsOffLightDamage()
    {
        MechaMorale morale = new MechaMorale();
        assertEquals(MechaMorale.Step.NONE, morale.step(50, 15, 400F));
        assertEquals(50, morale.getMorale());
        assertEquals(MechaMorale.Step.NONE, morale.step(50, 15, 390F));
        assertEquals(40, morale.getMorale());
        assertFalse(morale.isPanicking());
        morale.step(50, 15, 390F);
        assertEquals(41, morale.getMorale(), "morale recovers a point a second");
    }

    @Test
    void aMountPanicsTheSecondAfterItsMoraleRunsOut()
    {
        MechaMorale morale = new MechaMorale();
        morale.step(50, 15, 400F);
        assertEquals(MechaMorale.Step.NONE, morale.step(50, 15, 340F));
        assertFalse(morale.isPanicking());
        assertEquals(MechaMorale.Step.PANIC_STARTED, morale.step(50, 15, 340F));
        assertTrue(morale.isPanicking());
        assertEquals(15F, morale.getPanicTimer());
    }

    @Test
    void thePanicRunsDownTwoASecondAndDamageProlongsIt()
    {
        MechaMorale morale = new MechaMorale();
        morale.step(50, 15, 400F);
        morale.step(50, 15, 340F);
        morale.step(50, 15, 340F);
        assertEquals(MechaMorale.Step.PANICKING, morale.step(50, 15, 300F));
        assertEquals(13F + 10F, morale.getPanicTimer(), 1.0E-6F);
        int seconds = 0;
        while (morale.isPanicking() && seconds < 100)
        {
            morale.step(50, 15, 300F);
            seconds++;
        }
        assertEquals(12, seconds);
        assertFalse(morale.isPanicking());
    }
}
