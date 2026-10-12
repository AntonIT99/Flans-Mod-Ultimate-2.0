package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcDriving.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcDrivingTest
{
    /** Turns on the spot at 4.5 degrees per tick, full throttle in 20 ticks. */
    private static final Settings TRACKED = new Settings(true, 4.5F, 3F, 0.05F, 0.125F, 0.03F, 0.4F, 0.45F, 0.15F, true, 110F, 10D);
    /** Has to roll round a circle of 3 blocks to turn. */
    private static final Settings WHEELED = new Settings(false, 4.5F, 3F, 0.05F, 0.125F, 0.03F, 0.4F, 0.45F, 0.15F, true, 110F, 10D);

    private static Situation ahead(float hullYaw, float wantedYaw, double speed)
    {
        return new Situation(hullYaw, wantedYaw, 100D, 0F, Double.MAX_VALUE, 4D, speed, 1F);
    }

    @Test
    void aVehiclePicksUpSpeedGraduallyAndRollsOutWhenItHasNowhereToGo()
    {
        NpcDriving driving = new NpcDriving();
        assertEquals(0.05F, driving.drive(ahead(0F, 0F, 0D), TRACKED).throttle(), 1.0E-6F);
        for (int tick = 0; tick < 30; tick++)
            driving.drive(ahead(0F, 0F, 0.1D), TRACKED);
        assertEquals(1F, driving.getThrottle(), 1.0E-6F);
        // Coasting sheds speed more slowly than braking would.
        assertEquals(0.97F, driving.idle(0F, TRACKED).throttle(), 1.0E-6F);
        for (int tick = 0; tick < 40; tick++)
            driving.idle(0F, TRACKED);
        assertEquals(0F, driving.getThrottle());
    }

    @Test
    void aTrackedVehicleTurnsOnTheSpotAndSlowsWhileItsHullPointsAway()
    {
        NpcDriving driving = new NpcDriving();
        Command first = driving.drive(ahead(0F, 90F, 0D), TRACKED);
        assertEquals(4.5F, first.hullYaw(), 1.0E-4F);
        float hull = first.hullYaw();
        for (int tick = 0; tick < 60; tick++)
            hull = driving.drive(ahead(hull, 90F, 0.05D), TRACKED).hullYaw();
        assertEquals(90F, hull, 1.0E-3F);
        // While 85 degrees off, it creeps at little more than its floor; aligned, it reaches full throttle.
        NpcDriving turning = new NpcDriving();
        for (int tick = 0; tick < 3; tick++)
            turning.drive(ahead(0F, 90F, 0D), TRACKED);
        assertTrue(turning.getThrottle() <= 0.25F, "throttle while misaligned: " + turning.getThrottle());
        assertEquals(1F, driving.getThrottle(), 1.0E-6F);
    }

    @Test
    void aWheeledVehicleHasToRollToTurnAndTurnsWiderTheFasterItGoes()
    {
        NpcDriving driving = new NpcDriving();
        // Standing still it cannot turn, but it keeps rolling so that it can.
        Command standing = driving.drive(ahead(0F, 90F, 0D), WHEELED);
        assertEquals(0F, standing.hullYaw(), 1.0E-6F);
        assertTrue(standing.throttle() > 0F);
        // Rolling at 0.1 blocks per tick round 3 blocks is about 1.8 degrees per tick at low throttle.
        Command rolling = driving.drive(ahead(0F, 90F, 0.1D), WHEELED);
        assertTrue(rolling.hullYaw() > 1.5F && rolling.hullYaw() < 2F, "turned " + rolling.hullYaw());
        NpcDriving fast = new NpcDriving();
        for (int tick = 0; tick < 30; tick++)
            fast.drive(ahead(0F, 0F, 0.2D), WHEELED);
        float slowTurn = new NpcDriving().drive(ahead(0F, 90F, 0.2D), WHEELED).hullYaw();
        float fastTurn = fast.drive(ahead(0F, 90F, 0.2D), WHEELED).hullYaw();
        assertTrue(fastTurn < slowTurn, "at speed the circle widens: " + fastTurn + " against " + slowTurn);
        // A misaligned wheeled vehicle keeps at least half its speed, or it would take forever to come round.
        NpcDriving round = new NpcDriving();
        for (int tick = 0; tick < 40; tick++)
            round.drive(ahead(0F, 170F, 0.1D), new Settings(false, 4.5F, 3F, 0.05F, 0.125F, 0.03F, 0.4F, 0.45F, 0.15F, false, 110F, 10D));
        assertEquals(0.5F, round.getThrottle(), 1.0E-6F);
    }

    @Test
    void aCornerAheadSlowsTheVehicleMoreTheSharperAndTheNearerItIs()
    {
        float open = settle(new Situation(0F, 0F, 100D, 0F, Double.MAX_VALUE, 4D, 0.1D, 1F));
        float sharpNear = settle(new Situation(0F, 0F, 100D, 90F, 1D, 4D, 0.1D, 1F));
        float gentleNear = settle(new Situation(0F, 0F, 100D, 45F, 1D, 4D, 0.1D, 1F));
        float sharpFar = settle(new Situation(0F, 0F, 100D, 90F, 3D, 4D, 0.1D, 1F));
        assertEquals(1F, open, 1.0E-6F);
        assertEquals(0.45F, sharpNear, 1.0E-5F);
        assertEquals(0.725F, gentleNear, 1.0E-5F);
        assertEquals(0.725F, sharpFar, 1.0E-5F);
        assertEquals(1F, settle(new Situation(0F, 0F, 100D, 90F, 6D, 4D, 0.1D, 1F)), 1.0E-6F);
    }

    private static float settle(Situation situation)
    {
        NpcDriving driving = new NpcDriving();
        for (int tick = 0; tick < 60; tick++)
            driving.drive(situation, TRACKED);
        return driving.getThrottle();
    }

    @Test
    void aNearbyGoalBehindIsReachedInReverseAfterBrakingToAHalt()
    {
        NpcDriving driving = new NpcDriving();
        for (int tick = 0; tick < 30; tick++)
            driving.drive(ahead(0F, 0F, 0.1D), TRACKED);
        Situation behind = new Situation(0F, 170F, 6D, 0F, Double.MAX_VALUE, 4D, 0.1D, 1F);
        // It brakes first: no jump from forward to reverse.
        assertEquals(0.875F, driving.drive(behind, TRACKED).throttle(), 1.0E-6F);
        float hull = 0F;
        for (int tick = 0; tick < 60; tick++)
            hull = driving.drive(new Situation(hull, 170F, 6D, 0F, Double.MAX_VALUE, 4D, 0.05D, 1F), TRACKED).hullYaw();
        assertTrue(driving.isReversing());
        assertEquals(-0.4F, driving.getThrottle(), 1.0E-6F);
        // The rear points at the goal, so the hull faces away from it.
        assertEquals(-10F, hull, 1.0E-3F);
        // Once reversing it stays in reverse until the goal is clearly ahead, not merely under the first threshold.
        driving.drive(new Situation(hull, hull + 100F, 6D, 0F, Double.MAX_VALUE, 4D, 0.05D, 1F), TRACKED);
        assertTrue(driving.isReversing());
        driving.drive(new Situation(hull, hull + 80F, 6D, 0F, Double.MAX_VALUE, 4D, 0.05D, 1F), TRACKED);
        assertFalse(driving.isReversing());
    }

    @Test
    void aFarGoalBehindOrAVehicleNotAllowedToReverseTurnsRound()
    {
        NpcDriving far = new NpcDriving();
        far.drive(new Situation(0F, 170F, 40D, 0F, Double.MAX_VALUE, 4D, 0D, 1F), TRACKED);
        assertFalse(far.isReversing());
        NpcDriving forbidden = new NpcDriving();
        Settings forwardOnly = new Settings(true, 4.5F, 3F, 0.05F, 0.125F, 0.03F, 0.4F, 0.45F, 0.15F, false, 110F, 10D);
        Command command = forbidden.drive(new Situation(0F, 170F, 6D, 0F, Double.MAX_VALUE, 4D, 0D, 1F), forwardOnly);
        assertFalse(forbidden.isReversing());
        assertEquals(4.5F, command.hullYaw(), 1.0E-4F);
    }

    @Test
    void aVehicleAheadSlowsAndThenStopsTheOneBehind()
    {
        assertEquals(1F, NpcDriving.followLimit(Double.MAX_VALUE, 6));
        assertEquals(1F, NpcDriving.followLimit(6D, 6));
        assertEquals(0.5F, NpcDriving.followLimit(4.5D, 6), 1.0E-6F);
        assertEquals(0F, NpcDriving.followLimit(3D, 6));
        assertEquals(0F, NpcDriving.followLimit(-1D, 6));
        NpcDriving driving = new NpcDriving();
        for (int tick = 0; tick < 30; tick++)
            driving.drive(ahead(0F, 0F, 0.1D), TRACKED);
        for (int tick = 0; tick < 20; tick++)
            driving.drive(new Situation(0F, 0F, 100D, 0F, Double.MAX_VALUE, 4D, 0.1D, 0F), TRACKED);
        assertEquals(0F, driving.getThrottle());
    }

    @Test
    void presetsFillInTheDrivingValuesAndLeaveTheRestAlone()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.set(NpcTuning.Value.STOP_DISTANCE, 3);
        tuning.applyPreset(NpcTuning.DrivePreset.HEAVY_TANK);
        assertEquals(NpcTuning.DriveType.HEAVY_TRACKED, tuning.driveType());
        assertEquals(45, tuning.get(NpcTuning.Value.DRIVE_ACCELERATION));
        assertEquals(700, tuning.get(NpcTuning.Value.HULL_TURN_RATE));
        assertEquals(3, tuning.get(NpcTuning.Value.STOP_DISTANCE));
        // Custom only records the choice.
        tuning.applyPreset(NpcTuning.DrivePreset.CUSTOM);
        assertEquals(45, tuning.get(NpcTuning.Value.DRIVE_ACCELERATION));
        assertEquals(0, tuning.get(NpcTuning.Value.DRIVE_PRESET));
    }
}
