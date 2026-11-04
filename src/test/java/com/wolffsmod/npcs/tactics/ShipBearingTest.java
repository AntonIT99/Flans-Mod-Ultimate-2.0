package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.tactics.ShipBearing.Course;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class ShipBearingTest
{
    private static final double HOLD = 40D;
    /** The target lies due south of the ship, which Minecraft counts as yaw 0. */
    private static final Vec3 SHIP = Vec3.ZERO;
    private static final Vec3 TARGET = new Vec3(0D, 0D, 40D);

    @Test
    void aShipWithSomethingFixedToItsHullRunsInUntilItIsAsCloseAsItWants()
    {
        assertEquals(Course.RUN_IN, ShipBearing.course(true, 120D, HOLD, null));
        assertEquals(Course.RUN_IN, ShipBearing.course(true, HOLD + 1D, HOLD, Course.RUN_IN));
        assertEquals(Course.TURN_AWAY, ShipBearing.course(true, HOLD - 1D, HOLD, Course.RUN_IN));
    }

    @Test
    void aShipThatHasTurnedAwayOpensWellOutBeforeItRunsInAgain()
    {
        assertEquals(Course.TURN_AWAY, ShipBearing.course(true, HOLD + 1D, HOLD, Course.TURN_AWAY), "still short of the range it comes round at");
        assertEquals(Course.RUN_IN, ShipBearing.course(true, HOLD * ShipBearing.REOPEN_SHARE, HOLD, Course.TURN_AWAY));
    }

    @Test
    void aShipWithNothingFixedLeftToFireCirclesItsTargetSoItsTurretsBear()
    {
        assertEquals(Course.CIRCLE, ShipBearing.course(false, 120D, HOLD, Course.RUN_IN));
        assertEquals(Course.CIRCLE, ShipBearing.course(false, 5D, HOLD, Course.TURN_AWAY));
    }

    @Test
    void aShipHoldsNoLessThanItsLeastDistanceHoweverShortItsRangeIs()
    {
        assertEquals(ShipBearing.MIN_HOLD, ShipBearing.hold(4D));
        assertEquals(HOLD, ShipBearing.hold(HOLD));
        // A ship barely inside the least distance turns away rather than closing further.
        assertEquals(Course.TURN_AWAY, ShipBearing.course(true, ShipBearing.MIN_HOLD - 1D, 4D, null));
    }

    @Test
    void aRunInPutsTheBowOnTheTargetAndATurnAwayTakesTheShipOffIt()
    {
        assertEquals(0F, ShipBearing.heading(Course.RUN_IN, SHIP, TARGET, HOLD, 1), 1.0E-4F);
        assertEquals(ShipBearing.TURN_AWAY_ANGLE, ShipBearing.heading(Course.TURN_AWAY, SHIP, TARGET, HOLD, 1), 1.0E-4F);
        assertEquals(-ShipBearing.TURN_AWAY_ANGLE, ShipBearing.heading(Course.TURN_AWAY, SHIP, TARGET, HOLD, -1), 1.0E-4F);
        // The point it steams for lies a lead ahead on that heading, so it never stops at it.
        assertEquals(ShipBearing.LEAD, ShipBearing.point(Course.RUN_IN, SHIP, TARGET, HOLD, 1).distanceTo(SHIP), 1.0E-6D);
    }

    @Test
    void aCirclingShipSteamsAcrossItsTargetAndLeansInOrOutToHoldTheRange()
    {
        // At the distance it circles at, its course is square to the bearing: its side is on the target.
        assertEquals(90F, ShipBearing.heading(Course.CIRCLE, SHIP, TARGET, HOLD, 1), 1.0E-4F);
        assertEquals(-90F, ShipBearing.heading(Course.CIRCLE, SHIP, TARGET, HOLD, -1), 1.0E-4F);
        assertTrue(ShipBearing.inward(HOLD + 10D, HOLD) > 0F, "out beyond its circle it leans in");
        assertTrue(ShipBearing.inward(HOLD - 10D, HOLD) < 0F, "inside it it leans out");
        assertEquals(ShipBearing.MAX_CIRCLE_TURN, ShipBearing.inward(HOLD * 4D, HOLD), 1.0E-4F, "never turns in more than that");
        assertEquals(-ShipBearing.MAX_CIRCLE_TURN, ShipBearing.inward(0D, HOLD), 1.0E-4F);
    }

    @Test
    void aBlockedCourseIsSwungOffUntilTheHullHasWaterToSteamThrough()
    {
        // Everything to the ship's south, where its target lies, is land; the water lies east.
        Predicate<Vec3> eastOnly = point -> point.x > 1D;
        Vec3 clear = ShipBearing.clearPoint(Course.RUN_IN, SHIP, TARGET, HOLD, 1, eastOnly);
        assertTrue(eastOnly.test(clear), "steams for water rather than straight at the beach");
        assertEquals(ShipBearing.LEAD, clear.distanceTo(SHIP), 1.0E-6D, "still a lead ahead of itself");
    }

    @Test
    void aCourseThatIsAlreadyClearIsNotSwungAtAll()
    {
        Vec3 clear = ShipBearing.clearPoint(Course.RUN_IN, SHIP, TARGET, HOLD, 1, point -> true);
        assertEquals(ShipBearing.point(Course.RUN_IN, SHIP, TARGET, HOLD, 1), clear);
    }

    @Test
    void aShipBoxedInOnEveryHeadingKeepsTheCourseItWanted()
    {
        Vec3 blocked = ShipBearing.clearPoint(Course.RUN_IN, SHIP, TARGET, HOLD, 1, point -> false);
        assertEquals(ShipBearing.point(Course.RUN_IN, SHIP, TARGET, HOLD, 1), blocked);
    }

    @Test
    void theBowIsLaidWhereASlowRoundMeetsAMovingTargetRatherThanOnItsWake()
    {
        // The target runs east at a tenth of a block a tick; the round makes one block a tick.
        Vec3 aim = ShipBearing.intercept(SHIP, TARGET, 1D, ticks -> new Vec3(0.1D * ticks, 0D, 0D));
        assertTrue(aim.x > 3D, "led ahead of the target by roughly its run over the round's flight");
        assertEquals(TARGET.z, aim.z, 1.0E-6D);
    }

    @Test
    void aRoundWhoseSpeedIsUnknownOrInstantIsNotLedAtAll()
    {
        assertEquals(TARGET, ShipBearing.intercept(SHIP, TARGET, Double.NaN, ticks -> new Vec3(9D, 0D, 0D)));
        assertEquals(TARGET, ShipBearing.intercept(SHIP, TARGET, 0D, ticks -> new Vec3(9D, 0D, 0D)));
    }

    @Test
    void aStandingTargetIsAimedAtWhereItStands()
    {
        assertEquals(TARGET, ShipBearing.intercept(SHIP, TARGET, 1D, ticks -> Vec3.ZERO));
    }
}
