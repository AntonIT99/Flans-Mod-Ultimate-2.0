package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.sim.NpcTuning.AircraftRole;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotAirMissionsTest
{
    @Test
    void fightersInvestigateReportedInterceptionsBeforeCloserGroundContacts()
    {
        var ground = contact(new Vec3(10D, 0D, 0D), false, false);
        var threat = contact(new Vec3(500D, 80D, 0D), true, true);
        var mission = BotAirMissions.choose(List.of(ground, threat), AircraftRole.FIGHTER, Vec3.ZERO, Vec3.ZERO);
        assertEquals(threat.position(), mission.position());
        assertEquals(0, mission.priority());
    }

    @Test
    void bombersSupportTheFrontAndIgnoreUnrelatedDistantGroundFights()
    {
        var closeToFront = contact(new Vec3(20D, 0D, 0D), false, false);
        var unrelated = contact(new Vec3(700D, 0D, 0D), false, false);
        var aircraft = contact(new Vec3(5D, 80D, 0D), true, true);
        assertEquals(closeToFront.position(), BotAirMissions.choose(List.of(unrelated, aircraft, closeToFront), AircraftRole.BOMBER, Vec3.ZERO, Vec3.ZERO).position());
        var patrol = BotAirMissions.choose(List.of(unrelated), AircraftRole.BOMBER, Vec3.ZERO, Vec3.ZERO);
        assertEquals(Vec3.ZERO, patrol.position());
        assertEquals(4, patrol.priority());
    }

    @Test
    void missionsUseOnlyLastReportedPositionsAndReturnToPatrolAfterContactExpiry()
    {
        BotIntelligence intelligence = new BotIntelligence();
        Vec3 reported = new Vec3(200D, 80D, 0D);
        intelligence.report(0, UUID.randomUUID(), reported, false, 10L, true, true);
        assertEquals(reported, BotAirMissions.choose(intelligence.contacts(0, 20L), AircraftRole.FIGHTER, Vec3.ZERO, Vec3.ZERO).position());
        assertEquals(Vec3.ZERO, BotAirMissions.choose(intelligence.contacts(0, 10L + BotIntelligence.MEMORY_TICKS), AircraftRole.FIGHTER, Vec3.ZERO, Vec3.ZERO).position());
    }

    private static BotIntelligence.Contact contact(Vec3 position, boolean airborne, boolean intercept)
    {
        return new BotIntelligence.Contact(UUID.randomUUID(), position, false, 0L, airborne, intercept);
    }

    @Test
    void routineSightingsPreserveAnAttackWarningButCannotExtendItOrTrackItsMovement()
    {
        BotIntelligence intelligence = new BotIntelligence();
        UUID enemy = UUID.randomUUID();
        Vec3 attack = new Vec3(40D, 30D, 0D);
        intelligence.report(0, enemy, attack, false, 10L, true, true);
        Vec3 sighting = new Vec3(20D, 30D, 0D);
        intelligence.report(0, enemy, sighting, false, 20L, true, false);
        assertTrue(intelligence.contacts(0, 50L).get(0).intercept());
        assertEquals(sighting, intelligence.contacts(0, 50L).get(0).position());
        assertFalse(intelligence.contacts(0, 110L).get(0).intercept());
        assertEquals(sighting, intelligence.contacts(0, 110L).get(0).position());
        intelligence.report(0, enemy, attack, false, 111L, true, true);
        assertTrue(intelligence.contacts(0, 112L).get(0).intercept());
        intelligence.report(0, enemy, attack, false, 113L, false, false);
        assertFalse(intelligence.contacts(0, 114L).get(0).intercept(), "landing clears the interception request");
        assertTrue(intelligence.contacts(1, 114L).isEmpty());
    }
}
