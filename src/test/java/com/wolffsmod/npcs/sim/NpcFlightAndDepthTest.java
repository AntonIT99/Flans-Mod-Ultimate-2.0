package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcTuning.Flight;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcFlightAndDepthTest
{
    @Test
    void anAircraftClimbsFirmlyWhenTooLowAndDescendsOnlyWhenFarTooHigh()
    {
        // Ten blocks too low: a plane climbs, harder the lower it is, up to its limit.
        assertEquals(0.24D, NpcFlight.climb(0D, 10D, Flight.PLANE, false), 1.0E-9D);
        assertEquals(0.13D, NpcFlight.climb(0D, 2D, Flight.PLANE, false), 1.0E-9D);
        assertEquals(0.30D, NpcFlight.climb(0D, 10D, Flight.HELICOPTER, false), 1.0E-9D);
        // A climb already faster than that is left alone.
        assertEquals(0.5D, NpcFlight.climb(0.5D, 2D, Flight.PLANE, false), 1.0E-9D);
        // A little too high is fine; far too high comes down gently.
        assertEquals(0.02D, NpcFlight.climb(0.02D, -3D, Flight.PLANE, false), 1.0E-9D);
        assertEquals(-0.12D, NpcFlight.climb(0D, -20D, Flight.PLANE, false), 1.0E-9D);
        assertEquals(-0.15D, NpcFlight.climb(0D, -6D, Flight.HELICOPTER, false), 1.0E-9D);
        // A helicopter with nowhere to go settles into a hover; a plane does not.
        assertEquals(0.055D, NpcFlight.climb(0.1D, 0D, Flight.HELICOPTER, true), 1.0E-9D);
        assertEquals(0.1D, NpcFlight.climb(0.1D, 0D, Flight.HELICOPTER, false), 1.0E-9D);
        assertEquals(0.1D, NpcFlight.climb(0.1D, 0D, Flight.PLANE, true), 1.0E-9D);
    }

    @Test
    void anOrbitingPlaneTurnsInForAGunPassNowAndThenAndCarriesOnBackToItsCircle()
    {
        NpcFlight flight = new NpcFlight();
        double radius = 40D;
        for (int tick = 1; tick < NpcFlight.ORBIT_PASS_INTERVAL; tick++)
            assertFalse(flight.orbitPass(40D, radius));
        assertTrue(flight.orbitPass(40D, radius), "turns in");
        assertTrue(flight.orbitPass(20D, radius), "closes in");
        assertTrue(flight.orbitPass(5D, radius), "has passed over the target and carries on");
        assertTrue(flight.orbitPass(30D, radius), "still short of its circle");
        assertFalse(flight.orbitPass(41D, radius), "circles again");
        assertFalse(flight.orbitPass(41D, radius), "and counts afresh");
    }

    @Test
    void anOrbitingPlaneTooCloseToLineUpWaitsAndAPassThatLastsTooLongIsBrokenOff()
    {
        NpcFlight flight = new NpcFlight();
        for (int tick = 1; tick < NpcFlight.ORBIT_PASS_INTERVAL; tick++)
            flight.orbitPass(40D, 40D);
        assertFalse(flight.orbitPass(20D, 40D), "too close to line up");
        assertTrue(flight.orbitPass(40D, 40D));
        for (int tick = 0; tick < NpcFlight.MAX_PASS_TICKS; tick++)
            assertTrue(flight.orbitPass(50D, 40D));
        assertFalse(flight.orbitPass(50D, 40D), "broken off beyond its circle");
    }

    @Test
    void aPlaneNeitherStallsNorSpinsInPlace()
    {
        assertEquals(NpcFlight.STALL_SPEED, NpcFlight.planeSpeed(0, 0D));
        assertEquals(NpcFlight.TOP_SPEED, NpcFlight.planeSpeed(0, 3D));
        assertEquals(0.3D, NpcFlight.planeSpeed(0, 0.3D));
        assertEquals(0.25D, NpcFlight.planeSpeed(50, 0D), 1.0E-9D);
        // Flying a circle of 16 blocks at 0.3 blocks per tick takes about one degree per tick, and it can turn a quarter more.
        assertEquals(1.074F * 1.25F, NpcFlight.planeTurn(0.3D, 16D), 1.0E-3F);
        assertEquals(2F, NpcFlight.planeTurn(0.42D, 2D));
        assertEquals(0.25F, NpcFlight.planeTurn(0.13D, 400D));
    }

    @Test
    void aPlaneCirclesItsHomeAtItsWalkingRangeOrWideWithoutOne()
    {
        assertEquals(NpcFlight.HOME_CIRCLE_RADIUS, NpcFlight.loiterRadius(0));
        assertEquals(16D, NpcFlight.loiterRadius(5));
        assertEquals(30D, NpcFlight.loiterRadius(30));
        assertEquals(100D, NpcFlight.loiterRadius(100));
    }

    @Test
    void anAircraftThatDroppedBelowItsAttackAltitudeSettlesHighEnoughToFireAgain()
    {
        // It settles no lower than its margin less the band it no longer climbs in, which has to clear the height it fires again at.
        assertTrue(NpcFlight.HELICOPTER_MARGIN - NpcFlight.CLIMB_DEADBAND > NpcFlight.ATTACK_HYSTERESIS);
        assertTrue(NpcFlight.PLANE_MARGIN - NpcFlight.CLIMB_DEADBAND > NpcFlight.ATTACK_HYSTERESIS);
        // Within that band it no longer climbs.
        assertEquals(0D, NpcFlight.climb(0D, NpcFlight.CLIMB_DEADBAND, Flight.HELICOPTER, false));
        assertTrue(NpcFlight.climb(0D, NpcFlight.CLIMB_DEADBAND + 0.01D, Flight.HELICOPTER, false) > 0D);
    }

    @Test
    void aHelicopterBacksOffFromATargetSteeperThanItsGunsReach()
    {
        NpcVehicleState.ShotPitch tipped = new NpcVehicleState.ShotPitch(-45F, 45F);
        assertFalse(NpcFlight.tooSteep(30F, tipped));
        assertTrue(NpcFlight.tooSteep(41F, tipped), "too close to the steepest to keep the shot");
        assertTrue(NpcFlight.tooSteep(80F, tipped), "right over its target");
        assertTrue(NpcFlight.tooSteep(-60F, tipped), "steeply under an aircraft above it");
        assertFalse(NpcFlight.tooSteep(80F, new NpcVehicleState.ShotPitch(-90F, 90F)));
        // It backs off across the ground away from its target, and against its heading when right over it.
        Vec3 away = NpcFlight.away(new Vec3(10D, 90D, 0D), new Vec3(0D, 64D, 0D), 0F);
        assertEquals(1D, away.x, 1.0E-9D);
        assertEquals(0D, away.y);
        assertEquals(-1D, NpcFlight.away(new Vec3(0D, 90D, 0D), new Vec3(0D, 64D, 0D), 0F).z, 1.0E-6D);
        // Flying in, it turns round to at least its backing-off speed; flying out faster, it keeps its speed; sideways motion stays.
        Vec3 backing = NpcFlight.backOff(new Vec3(-0.3D, 0.1D, 0.2D), away, NpcFlight.BACK_OFF_SPEED);
        assertEquals(NpcFlight.BACK_OFF_SPEED, backing.x, 1.0E-9D);
        assertEquals(0.2D, backing.z, 1.0E-9D);
        assertEquals(0.1D, backing.y, 1.0E-9D);
        Vec3 fast = new Vec3(0.4D, 0D, 0D);
        assertEquals(fast, NpcFlight.backOff(fast, away, NpcFlight.BACK_OFF_SPEED));
    }

    @Test
    void aRoamingPlaneMovesItsCircleOnRoughlyAlongItsHeading()
    {
        // Heading 0 flies toward positive z.
        Vec3 next = NpcFlight.nextRoamCentre(new Vec3(10D, 80D, 20D), 0F, 128D, 0F);
        assertEquals(10D, next.x, 1.0E-2D);
        assertEquals(148D, next.z, 1.0E-2D);
        assertEquals(80D, next.y);
        // Turned off its heading, the next point lies as far away.
        Vec3 turned = NpcFlight.nextRoamCentre(Vec3.ZERO, 0F, 128D, NpcFlight.ROAM_SPREAD);
        assertEquals(128D, turned.horizontalDistance(), 1.0E-2D);
        assertTrue(turned.x < 0D && turned.z > 0D);
    }

    @Test
    void aPlaneIsNeverToldToCircleTighterThanItTurns()
    {
        // Two degrees a tick at one block a tick flies a circle of about 28.6 blocks.
        assertEquals(28.648D, NpcFlight.turnRadius(1D, 2F), 1.0E-3D);
        assertEquals(NpcFlight.turnRadius(1D, NpcFlight.MIN_PLANE_TURN), NpcFlight.turnRadius(1D, 0F));
        // Without a turn rate or a model it turns two degrees a tick at most, a model as its bank allows, a set rate as set.
        assertEquals(NpcFlight.MAX_PLANE_TURN, NpcFlight.maxTurn(0, 1D, false));
        assertEquals(NpcFlight.coordinatedTurn(2D), NpcFlight.maxTurn(0, 2D, true));
        assertEquals(1.5F, NpcFlight.maxTurn(30, 2D, true));
        // The tightest circle it is told to fly leaves it a quarter more turn than the circle takes, at most what it turns.
        double tightest = NpcFlight.turnRadius(0.42D, NpcFlight.MAX_PLANE_TURN) * NpcFlight.CIRCLE_MARGIN;
        assertEquals(NpcFlight.MAX_PLANE_TURN, NpcFlight.planeTurn(0.42D, tightest), 1.0E-4F);
        // A fast model plane circles far wider than its sixteen-block home circle.
        assertTrue(NpcFlight.turnRadius(2D, NpcFlight.maxTurn(0, 2D, true)) > 40D);
    }

    @Test
    void aLoiteringPlaneFliesTheTangentAndCorrectsTowardItsCircle()
    {
        Vec3 centre = new Vec3(0D, 80D, 0D);
        // East of the centre on the circle: clockwise heads south, the other way north.
        assertEquals(0F, Mth.wrapDegrees(NpcFlight.loiterYaw(new Vec3(32D, 80D, 0D), centre, 32D, true, 45F)), 1.0E-3F);
        assertEquals(180F, Math.abs(Mth.wrapDegrees(NpcFlight.loiterYaw(new Vec3(32D, 80D, 0D), centre, 32D, false, 45F))), 1.0E-3F);
        // Outside the circle it leans in, toward the west; inside it leans out.
        float outside = Mth.wrapDegrees(NpcFlight.loiterYaw(new Vec3(64D, 80D, 0D), centre, 32D, true, 45F));
        float inside = Mth.wrapDegrees(NpcFlight.loiterYaw(new Vec3(16D, 80D, 0D), centre, 32D, true, 45F));
        assertTrue(outside > 0F && outside < 90F, "leans west: " + outside);
        assertTrue(inside < 0F && inside > -90F, "leans east: " + inside);
        // Right over the centre there is no tangent; it keeps its heading.
        assertEquals(45F, NpcFlight.loiterYaw(new Vec3(0.2D, 80D, 0D), centre, 32D, true, 45F));
    }

    @Test
    void aSubmarineHoldsItsDepthAndRidesHigherOverShallows()
    {
        assertEquals(4, NpcSubmarine.effectiveDepth(4, 20));
        assertEquals(2, NpcSubmarine.effectiveDepth(4, 3));
        assertEquals(1, NpcSubmarine.effectiveDepth(4, 1));
        // Three blocks above its depth it sinks, three below it rises, and near it the motion dies away.
        assertEquals(-0.18D, NpcSubmarine.dive(0D, -3D), 1.0E-9D);
        assertEquals(-0.08D, NpcSubmarine.dive(0D, -1D), 1.0E-9D);
        assertEquals(0.16D, NpcSubmarine.dive(0D, 3D), 1.0E-9D);
        assertEquals(0.045D, NpcSubmarine.dive(0.1D, 0.2D), 1.0E-9D);
    }

    @Test
    void aShotThroughCoverStartsJustBeyondTheLastBlockItCrosses()
    {
        Vec3 muzzle = new Vec3(0.5D, 65.5D, 0.5D);
        Vec3 target = new Vec3(10.5D, 65.5D, 0.5D);
        Vec3 exit = NpcSight.exit(new BlockPos(3, 65, 0), muzzle, target);
        assertEquals(4.1D, exit.x, 1.0E-9D);
        assertEquals(65.5D, exit.y, 1.0E-9D);
        assertEquals(0.5D, exit.z, 1.0E-9D);
        // A block the line does not cross moves nothing.
        assertEquals(muzzle, NpcSight.exit(new BlockPos(3, 70, 0), muzzle, target));
    }

    @Test
    void longPathsAreDrawnWithEvenlySpacedMarkers()
    {
        assertEquals(1, NpcPathDebug.stride(0));
        assertEquals(1, NpcPathDebug.stride(NpcPathDebug.MAX_MARKERS));
        assertEquals(2, NpcPathDebug.stride(NpcPathDebug.MAX_MARKERS + 1));
        assertEquals(5, NpcPathDebug.stride(200));
        assertTrue(200 / NpcPathDebug.stride(200) <= NpcPathDebug.MAX_MARKERS);
    }

    @Test
    void aSubmarinePassesAPathNodeFromBelow()
    {
        // Level with the node or just under it, Custom NPCs counts the node itself.
        assertFalse(NpcSubmarine.passesBelow(0D, 0D, 0D, 2F));
        assertFalse(NpcSubmarine.passesBelow(0D, -0.65D, 0D, 2F));
        assertTrue(NpcSubmarine.passesBelow(0.9D, -3D, -0.9D, 2F));
        assertTrue(NpcSubmarine.passesBelow(0D, -40D, 0D, 0.6F));
        // Beside the node is not under it.
        assertFalse(NpcSubmarine.passesBelow(1.1D, -3D, 0D, 2F));
        assertFalse(NpcSubmarine.passesBelow(0D, -3D, 0.5D, 0.6F));
    }

    @Test
    void anNpcWithoutItsOwnSearchIntervalFollowsTheServer()
    {
        assertEquals(25, NpcTargetSearch.interval(0, 25));
        assertEquals(7, NpcTargetSearch.interval(7, 25));
        assertEquals(1, NpcTargetSearch.interval(0, 0));
    }
}
