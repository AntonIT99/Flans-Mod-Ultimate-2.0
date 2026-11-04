package com.wolffsmod.npcs.drivers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcHullAnglingTest
{
    @Test
    void anAngledHullHoldsItsFacing()
    {
        assertEquals(0F, NpcHullAngling.course(25F, false));
        assertEquals(0F, NpcHullAngling.course(-38F, false));
        assertEquals(0F, NpcHullAngling.course(12F, false));
    }

    @Test
    void aSideOnHullTurnsItsFrontToTheThreat()
    {
        assertEquals(65F, NpcHullAngling.course(90F, false));
        assertEquals(-65F, NpcHullAngling.course(-90F, false));
        assertEquals(145F, NpcHullAngling.course(170F, false), 1.0E-3F);
    }

    @Test
    void aTurnContinuesUntilTheHullSettlesAtItsAngle()
    {
        assertEquals(10F, NpcHullAngling.course(35F, true));
        assertEquals(0F, NpcHullAngling.course(28F, true));
        assertEquals(-15F, NpcHullAngling.course(10F, true));
    }
}
