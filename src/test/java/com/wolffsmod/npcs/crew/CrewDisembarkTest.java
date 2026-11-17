package com.wolffsmod.npcs.crew;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.sim.NpcFlight;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class CrewDisembarkTest
{
    private static final int TICKS_PER_SECOND = 20;
    /** Blocks from its target a vehicle has closed with it at. */
    private static final double CLOSE = 20D;
    /** Blocks from its target a vehicle firing a machine gun from its own preferred distance stands at. */
    private static final double STANDOFF = 60D;
    /** Blocks per tick a standing vehicle moves. */
    private static final double STOPPED = 0D;
    /** Blocks per tick a vehicle under way moves. */
    private static final double DRIVING = 0.4D;
    /** The delay as an unedited NPC has it, in ticks. */
    private static final int DELAY = Value.DISEMBARK_DELAY.fallback() * TICKS_PER_SECOND;

    @Test
    void anAircraftShotToPiecesIsAbandonedBeforeItGoesDown() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        when(vehicle.getMaxHealth()).thenReturn(100F);
        try (var flight = mockStatic(NpcFlight.class))
        {
            flight.when(() -> NpcFlight.type(vehicle)).thenReturn(NpcTuning.Flight.PLANE);
            when(vehicle.getHealth()).thenReturn(50F);
            assertFalse(CrewDisembark.failing(vehicle), "damage it can still fly home with");
            when(vehicle.getHealth()).thenReturn(10F);
            assertTrue(CrewDisembark.failing(vehicle), "too far gone to be brought home");
            flight.when(() -> NpcFlight.type(vehicle)).thenReturn(NpcTuning.Flight.NORMAL);
            assertFalse(CrewDisembark.failing(vehicle), "a crippled ground vehicle is no reason to jump");
        }
    }

    @Test
    void aVehicleThatHasClosedWithItsTargetLetsItsCrewOutAtOnce()
    {
        assertTrue(CrewDisembark.deploys(CLOSE, 0, STOPPED, DELAY));
        // Up close it waits neither for the delay nor to be stopped, as it did before contact was counted at all.
        assertTrue(CrewDisembark.deploys(CLOSE, 0, DRIVING, DELAY));
    }

    @Test
    void aVehicleFiringFromItsOwnStandoffDistanceStillPutsItsCrewDown()
    {
        // The case this rule exists for: a halftrack fights with its machine gun from further off than it would ever close to.
        assertFalse(CrewDisembark.deploys(STANDOFF, 0, STOPPED, DELAY), "not the moment it finds a target");
        assertTrue(CrewDisembark.deploys(STANDOFF, DELAY, STOPPED, DELAY), "once it has been in action for the delay");
    }

    @Test
    void crewDoNotGetOutOfAVehicleUnderWay()
    {
        assertFalse(CrewDisembark.deploys(STANDOFF, DELAY, DRIVING, DELAY));
        // It puts them down as soon as it pulls up.
        assertTrue(CrewDisembark.deploys(STANDOFF, DELAY, STOPPED, DELAY));
    }

    @Test
    void aBriefExchangeAtADistanceDoesNotEmptyTheTroopBay()
    {
        assertFalse(CrewDisembark.deploys(STANDOFF, DELAY - 1, STOPPED, DELAY));
    }

    @Test
    void anEditedDelayDecidesHowLongTheVehicleFightsFirst()
    {
        int longer = 40 * TICKS_PER_SECOND;
        assertFalse(CrewDisembark.deploys(STANDOFF, DELAY, STOPPED, longer), "a longer delay keeps them aboard");
        assertTrue(CrewDisembark.deploys(STANDOFF, longer, STOPPED, longer));
    }

    @Test
    void noDelayPutsTheCrewDownAsSoonAsThereIsATarget()
    {
        assertTrue(CrewDisembark.deploys(STANDOFF, 0, STOPPED, 0));
        // Standing still is still required, so that nobody is thrown out of a vehicle on the move.
        assertFalse(CrewDisembark.deploys(STANDOFF, 0, DRIVING, 0));
    }

    @Test
    void paratroopersJumpTogetherInWaves()
    {
        List<String> stick = List.of("a", "b", "c", "d", "e", "f");
        assertEquals(CrewDisembark.WAVE_SIZE, CrewDisembark.wave(stick, member -> true).size(), "a full stick jumps a wave at a time");
        assertEquals(List.of("a", "b", "c", "d"), CrewDisembark.wave(stick, member -> true), "in seat order");
        assertEquals(List.of("b", "d"), CrewDisembark.wave(stick, member -> member.equals("b") || member.equals("d")), "only those carrying a parachute");
        assertTrue(CrewDisembark.wave(stick, member -> false).isEmpty());
    }
}
