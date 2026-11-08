package com.wolffsmod.npcs.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcContactTest
{
    /** Ticks between the checks that record contact, as the crew rule samples it. */
    private static final int INTERVAL = 10;

    @Test
    void anNpcThatHasSeenNothingHoldsNoContact()
    {
        NpcContact contact = new NpcContact();
        assertEquals(0, contact.heldFor(0));
        // A tick without a target leaves it with none to count.
        contact.tick(0, false);
        assertEquals(0, contact.heldFor(0));
    }

    @Test
    void contactIsCountedFromTheTickItBegan()
    {
        NpcContact contact = new NpcContact();
        contact.tick(100, true);
        assertEquals(0, contact.heldFor(100));
        assertEquals(40, contact.heldFor(140), "the count runs on between the checks that record it");
        contact.tick(140, true);
        assertEquals(60, contact.heldFor(160));
    }

    @Test
    void aTargetOutOfSightForAMomentDoesNotStartTheCountAgain()
    {
        NpcContact contact = new NpcContact();
        contact.tick(0, true);
        // Checks without a target, inside the grace, keep the count running.
        for (int now = INTERVAL; now <= NpcContact.GRACE_TICKS; now += INTERVAL)
            contact.tick(now, false);
        assertEquals(NpcContact.GRACE_TICKS, contact.heldFor(NpcContact.GRACE_TICKS));
        contact.tick(NpcContact.GRACE_TICKS + INTERVAL, true);
        assertEquals(NpcContact.GRACE_TICKS + INTERVAL, contact.heldFor(NpcContact.GRACE_TICKS + INTERVAL), "contact it held throughout still counts");
    }

    @Test
    void aTargetLostForLongerGivesUpTheContact()
    {
        NpcContact contact = new NpcContact();
        contact.tick(0, true);
        contact.tick(NpcContact.GRACE_TICKS + 1, false);
        assertEquals(0, contact.heldFor(NpcContact.GRACE_TICKS + 1));
        // The next target it finds is counted from scratch.
        contact.tick(200, true);
        assertEquals(0, contact.heldFor(200));
        assertEquals(100, contact.heldFor(300), "and runs on from there");
    }

    @Test
    void contactIsNotGivenUpWhileTheTargetIsStillThere()
    {
        NpcContact contact = new NpcContact();
        for (int now = 0; now <= 400; now += INTERVAL)
            contact.tick(now, true);
        assertEquals(400, contact.heldFor(400));
    }
}
