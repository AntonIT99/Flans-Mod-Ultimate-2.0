package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.AircraftRole;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.*;

class NpcAirCombatTest
{
    @Test
    void fightersInterceptThreatsBeforeSeekingAirSuperiorityAndGroundSupport()
    {
        int interception = NpcAirCombat.priority(AircraftRole.FIGHTER, true, true);
        int airSuperiority = NpcAirCombat.priority(AircraftRole.FIGHTER, true, false);
        int support = NpcAirCombat.priority(AircraftRole.FIGHTER, false, false);
        assertTrue(interception < airSuperiority);
        assertTrue(airSuperiority < support);
    }

    @Test
    void bombersKeepTheirGroundMissionEvenWhenAnotherAircraftAttacks()
    {
        assertTrue(NpcAirCombat.priority(AircraftRole.BOMBER, false, false) < NpcAirCombat.priority(AircraftRole.BOMBER, true, true));
    }

    @Test
    void groundAttackAircraftSupportTheGroundButStillTurnOnAircraftThreateningTheirSide()
    {
        int support = NpcAirCombat.priority(AircraftRole.ATTACKER, false, false);
        int threat = NpcAirCombat.priority(AircraftRole.ATTACKER, true, true);
        int otherAircraft = NpcAirCombat.priority(AircraftRole.ATTACKER, true, false);
        assertTrue(support < threat);
        assertTrue(threat < otherAircraft);
    }

    @Test
    void aFighterLeavesAnUnreachableAircraftForAGroundTargetItCouldSupportNow()
    {
        int ground = NpcAirCombat.rank(AircraftRole.FIGHTER, false, false, true);
        assertTrue(NpcAirCombat.rank(AircraftRole.FIGHTER, true, false, true) < ground);
        assertTrue(NpcAirCombat.rank(AircraftRole.FIGHTER, true, false, false) > ground);
    }

    @Test
    void anAircraftGoingForAnAllyIsWorthChasingHoweverFarOff()
    {
        assertEquals(NpcAirCombat.rank(AircraftRole.FIGHTER, true, true, true), NpcAirCombat.rank(AircraftRole.FIGHTER, true, true, false));
    }

    @Test
    void theOtherRolesDoNotGiveUpDistantAircraft()
    {
        assertEquals(NpcAirCombat.rank(AircraftRole.BOMBER, true, false, true), NpcAirCombat.rank(AircraftRole.BOMBER, true, false, false));
        assertEquals(NpcAirCombat.rank(AircraftRole.ATTACKER, true, false, true), NpcAirCombat.rank(AircraftRole.ATTACKER, true, false, false));
    }

    @Test
    void automaticRoleDistinguishesBombOnlyAircraftAndMultiroleFighters()
    {
        assertEquals(AircraftRole.BOMBER, NpcAirCombat.automatic(false, true, false));
        assertEquals(AircraftRole.FIGHTER, NpcAirCombat.automatic(true, true, false));
        assertEquals(AircraftRole.FIGHTER, NpcAirCombat.automatic(true, false, false));
    }

    @Test
    void aGunArmedRotorcraftSupportsTheGroundAsAnAttackerWhileABombOnlyOneIsStillABomber()
    {
        assertEquals(AircraftRole.ATTACKER, NpcAirCombat.automatic(true, false, true));
        assertEquals(AircraftRole.ATTACKER, NpcAirCombat.automatic(true, true, true));
        assertEquals(AircraftRole.BOMBER, NpcAirCombat.automatic(false, true, true));
    }

    @Test
    void aGunshipTurnsOnAnAircraftGoingForItsOwnSideWhereABomberWouldNot()
    {
        AircraftRole gunship = NpcAirCombat.automatic(true, false, true);
        assertTrue(NpcAirCombat.priority(gunship, true, true) < NpcAirCombat.priority(gunship, true, false));
        assertEquals(NpcAirCombat.priority(AircraftRole.BOMBER, true, true), NpcAirCombat.priority(AircraftRole.BOMBER, true, false));
        // Supporting the ground still comes first, as it does for a ground-attack plane.
        assertTrue(NpcAirCombat.priority(gunship, false, false) < NpcAirCombat.priority(gunship, true, true));
    }

    @Test
    void missionsChooseGunsForInterceptionAndBombsForGroundRunsWithAnArmedFallback()
    {
        assertTrue(NpcAirCombat.bankScore(true, false, true, false) > NpcAirCombat.bankScore(false, true, true, false));
        assertTrue(NpcAirCombat.bankScore(false, true, false, true) > NpcAirCombat.bankScore(true, false, false, true));
        assertTrue(NpcAirCombat.bankScore(true, false, false, false) > NpcAirCombat.bankScore(false, true, false, false));
        assertTrue(NpcAirCombat.bankScore(false, true, false, false) > NpcAirCombat.bankScore(false, false, false, false));
    }

    @Test
    void explicitRolesAndSearchRangesSurviveSavingWhileOldNpcsUseAutomaticRoles()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.load(new CompoundTag());
        assertEquals(AircraftRole.AUTO, tuning.aircraftRole());
        assertEquals(192, tuning.get(NpcTuning.Value.AIR_SEARCH_RANGE));
        tuning.set(NpcTuning.Value.AIRCRAFT_ROLE, AircraftRole.BOMBER.ordinal());
        tuning.set(NpcTuning.Value.AIR_SEARCH_RANGE, 256);
        CompoundTag saved = new CompoundTag();
        tuning.save(saved);
        NpcTuning copy = new NpcTuning();
        copy.load(saved);
        assertEquals(AircraftRole.BOMBER, copy.aircraftRole());
        assertEquals(256, copy.get(NpcTuning.Value.AIR_SEARCH_RANGE));
    }
}
