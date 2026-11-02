package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotIntelligenceTest
{
    @Test
    void hiddenContactsKeepTheirLastReportedPositionAndExpireWithoutFreshReports()
    {
        BotIntelligence intelligence = new BotIntelligence();
        UUID enemy = UUID.randomUUID();
        Vec3 lastSeen = new Vec3(20D, 64D, 10D);
        intelligence.report(0, enemy, lastSeen, true, 100L);
        assertTrue(intelligence.contacts(1, 101L).isEmpty(), "other sides do not receive this report");
        assertEquals(lastSeen, intelligence.contacts(0, 699L).get(0).position());
        assertTrue(intelligence.contacts(0, 700L).isEmpty(), "reading a stale contact cannot renew its memory");
    }

    @Test
    void olderReportsCannotOverwriteANewerSightingAndNewSightingsRefreshThePosition()
    {
        BotIntelligence intelligence = new BotIntelligence();
        UUID enemy = UUID.randomUUID();
        intelligence.report(0, enemy, Vec3.ZERO, false, 10L);
        intelligence.report(0, enemy, new Vec3(5D, 0D, 0D), true, 20L);
        intelligence.report(0, enemy, Vec3.ZERO, false, 15L);
        BotIntelligence.Contact contact = intelligence.contacts(0, 25L).get(0);
        assertEquals(5D, contact.position().x);
        assertTrue(contact.armoured());
        assertEquals(20L, contact.observedAt());
        intelligence.clear();
        assertTrue(intelligence.contacts(0, 25L).isEmpty());
    }

    @Test
    void contactMemoryIsBoundedAndEvictsTheLeastRecentlyReportedEnemy()
    {
        BotIntelligence intelligence = new BotIntelligence();
        UUID oldest = UUID.randomUUID();
        UUID refreshed = UUID.randomUUID();
        intelligence.report(0, oldest, Vec3.ZERO, false, 0L);
        intelligence.report(0, refreshed, Vec3.ZERO, false, 1L);
        for (int index = 2; index < BotIntelligence.MAX_CONTACTS_PER_SIDE; index++)
            intelligence.report(0, UUID.randomUUID(), Vec3.ZERO, false, index);
        intelligence.report(0, refreshed, Vec3.ZERO, false, 200L);
        intelligence.report(0, UUID.randomUUID(), Vec3.ZERO, false, 201L);
        var contacts = intelligence.contacts(0, 202L);
        assertEquals(BotIntelligence.MAX_CONTACTS_PER_SIDE, contacts.size());
        assertFalse(contacts.stream().anyMatch(contact -> contact.enemy().equals(oldest)));
        assertTrue(contacts.stream().anyMatch(contact -> contact.enemy().equals(refreshed)));
    }
}
