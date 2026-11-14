package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class NpcBankCadenceTest
{
    @Test
    void eachGroupKeepsItsOwnWaitAcrossCalls()
    {
        NpcBankCadence cadence = new NpcBankCadence();
        IContentType tank = mock(IContentType.class);
        cadence.group(tank, false).holdFor(100L, 120D);
        cadence.group(tank, true).delay(100L, 2D);
        assertEquals(60D, cadence.group(tank, false).remaining(160L));
        assertEquals(0D, cadence.group(tank, true).remaining(160L));
        assertSame(cadence.group(tank, false), cadence.group(tank, false));
    }

    @Test
    void delaysAddUpWhileHoldingNeverShortensTheWait()
    {
        NpcBankCadence.Group group = new NpcBankCadence.Group();
        group.delay(0L, 2D);
        group.delay(0L, 2D);
        assertEquals(4D, group.remaining(0L));
        group.holdFor(0L, 1D);
        assertEquals(4D, group.remaining(0L));
    }

    @Test
    void anotherModelStartsAfresh()
    {
        NpcBankCadence cadence = new NpcBankCadence();
        IContentType tank = mock(IContentType.class);
        cadence.group(tank, false).holdFor(0L, 120D);
        cadence.group(tank, false).setFired(3);
        NpcBankCadence.Group other = cadence.group(mock(IContentType.class), false);
        assertEquals(0D, other.remaining(0L));
        assertEquals(0, other.getFired());
    }
}
