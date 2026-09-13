package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelicopterPhysicsTest
{
    private static final Vec3 UP = new Vec3(0D, 1D, 0D);
    private static final double EPSILON = 1E-7D;

    private static HelicopterPhysics.Performance legacy()
    {
        return HelicopterPhysics.resolve(RealWorldVehicleSpec.EMPTY, 1F, 0.08F, 1F, 1D);
    }

    private static RealWorldVehicleSpec spec(float mass, float power, Float speed, Float climb, float diameter, int count)
    {
        return new RealWorldVehicleSpec(mass, speed, power, null,
            new RealWorldVehicleSpec.Aircraft(null, null, climb, diameter, count), null, null);
    }

    @Test
    void legacyPackHoversAtHalfCollectiveAndClimbsAboveIt()
    {
        assertEquals(0D, HelicopterPhysics.step(Vec3.ZERO, UP, legacy(), 0.5F, 1F, 1F).length(), EPSILON);
        assertTrue(HelicopterPhysics.step(Vec3.ZERO, UP, legacy(), 0.6F, 1F, 1F).y > 0D);
        assertTrue(HelicopterPhysics.step(Vec3.ZERO, UP, legacy(), 0.4F, 1F, 1F).y < 0D);
    }

    @Test
    void horizontalSpeedRangeIsLinearAcrossTheWholeThrottleRange()
    {
        assertEquals(0D, HelicopterPhysics.horizontalSpeedFraction(0F));
        assertEquals(0.25D, HelicopterPhysics.horizontalSpeedFraction(0.25F));
        assertEquals(0.49D, HelicopterPhysics.horizontalSpeedFraction(0.49F), EPSILON);
        assertEquals(0.5D, HelicopterPhysics.horizontalSpeedFraction(0.5F));
        assertEquals(0.75D, HelicopterPhysics.horizontalSpeedFraction(0.75F));
        assertEquals(1D, HelicopterPhysics.horizontalSpeedFraction(1F));
    }

    @Test
    void levelingBrakesDriftWithoutAddingForwardMotion()
    {
        for (float throttle : new float[] {0F, 0.25F, 0.5F, 0.75F, 1F})
        {
            assertEquals(0D, HelicopterPhysics.step(Vec3.ZERO, UP, legacy(), throttle, 1F, 1F)
                .horizontalDistance(), EPSILON);
            Vec3 velocity = new Vec3(1D, 0D, -1D);
            for (int tick = 0; tick < 40; tick++)
                velocity = HelicopterPhysics.step(velocity, UP, legacy(), throttle, 1F, 1F);
            assertTrue(velocity.horizontalDistance() < 0.02D);
        }
    }

    @Test
    void pitchDirectionAndThrottleDetermineTranslation()
    {
        for (var performance : new HelicopterPhysics.Performance[] {legacy(),
            HelicopterPhysics.resolve(spec(5000F, 1000F, 250F, 8F, 12F, 1), 1F, 0.08F, 1F, 1D)})
        {
            Vec3 low = Vec3.ZERO;
            Vec3 high = Vec3.ZERO;
            Vec3 backward = Vec3.ZERO;
            for (int tick = 0; tick < 2000; tick++)
            {
                low = HelicopterPhysics.step(low, new Vec3(0.6D, 0.8D, 0D), performance, 0.25F, 1F, 1F);
                high = HelicopterPhysics.step(high, new Vec3(0.6D, 0.8D, 0D), performance, 1F, 1F, 1F);
                backward = HelicopterPhysics.step(backward, new Vec3(-0.6D, 0.8D, 0D), performance, 1F, 1F, 1F);
            }
            assertTrue(low.x > 0D && low.x <= high.x * 0.25D + EPSILON);
            assertEquals(-high.x, backward.x, EPSILON);
        }
    }

    @Test
    void loweringCollectiveReversesClimbPromptly()
    {
        var performance = HelicopterPhysics.resolve(spec(5000F, 1000F, 250F, 8F, 12F, 1), 1F, 0.08F, 1F, 1D);
        Vec3 velocity = new Vec3(0D, performance.climbSpeed(), 0D);
        for (int tick = 0; tick < 20; tick++)
            velocity = HelicopterPhysics.step(velocity, UP, performance, 0.25F, 1F, 1F);
        assertTrue(velocity.y < 0D);
        for (int tick = 0; tick < 20; tick++)
            velocity = HelicopterPhysics.step(velocity, UP, performance, 0.75F, 1F, 1F);
        assertTrue(velocity.y > 0D);
    }

    @Test
    void bankingRedirectsLiftAndRequiresMoreCollective()
    {
        Vec3 tilted = new Vec3(0.6D, 0.8D, 0D);
        Vec3 velocity = HelicopterPhysics.step(Vec3.ZERO, tilted, legacy(), 0.55F, 1F, 1F);
        assertTrue(velocity.x > 0D);
        assertTrue(velocity.y < 0D);
        assertTrue(HelicopterPhysics.step(Vec3.ZERO, UP.scale(-1D), legacy(), 0.5F, 1F, 1F).y
            < -HelicopterPhysics.GRAVITY);
    }

    @Test
    void hoverNeverRestoresDestroyedRotorsOrAnUnpoweredEngine()
    {
        for (float collective : new float[] {0.49F, 0.5F, 0.51F, 1F})
        {
            assertEquals(0D, HelicopterPhysics.lift(legacy(), collective, 1F, 0F));
            assertEquals(0D, HelicopterPhysics.lift(legacy(), collective, 0F, 1F));
        }
        assertEquals(0D, HelicopterPhysics.lift(legacy(), 0F, 1F, 1F));
        assertEquals(HelicopterPhysics.GRAVITY * 0.5D,
            HelicopterPhysics.lift(legacy(), 0.5F, 1F, 0.5F), EPSILON);
    }

    @Test
    void rotorDiscPowerAndMassDetermineAvailableLift()
    {
        var baseline = HelicopterPhysics.resolve(spec(5000F, 1000F, 250F, null, 12F, 1), 1F, 0.08F, 1F, 1D);
        var heavy = HelicopterPhysics.resolve(spec(10000F, 1000F, 250F, null, 12F, 1), 1F, 0.08F, 1F, 1D);
        var tandem = HelicopterPhysics.resolve(spec(5000F, 1000F, 250F, null, 12F, 2), 1F, 0.08F, 1F, 1D);
        var upgrade = HelicopterPhysics.resolve(spec(5000F, 1000F, 250F, null, 12F, 1), 1.2F, 0.08F, 1F, 1D);
        assertEquals(baseline.maximumLift() * 0.5D, heavy.maximumLift(), EPSILON);
        assertTrue(tandem.maximumLift() > baseline.maximumLift());
        assertTrue(upgrade.maximumLift() > baseline.maximumLift());
        assertTrue(HelicopterPhysics.lift(heavy, 0.5F, 1F, 1F) < HelicopterPhysics.GRAVITY);
    }

    @Test
    void thrustAuthoredVtolUsesLiftEngineThrustWithoutRotorGeometry()
    {
        var source = new RealWorldVehicleSpec(5000F, null, null, 75F, null, null, null);
        var performance = HelicopterPhysics.resolve(source, 1F, 0.08F, 1F, 1D);
        assertEquals(HelicopterPhysics.GRAVITY * 75000D / (5000D * VehiclePhysicsUnits.STANDARD_GRAVITY),
            performance.maximumLift(), EPSILON);
        assertEquals(0D, HelicopterPhysics.step(Vec3.ZERO, UP, performance, 0.5F, 1F, 1F).y, EPSILON);
        var upgraded = HelicopterPhysics.resolve(source, 1.2F, 0.08F, 1F, 1D);
        assertTrue(upgraded.maximumLift() > performance.maximumLift());
    }

    @Test
    void incompletePowerGroupFallsBackButSpeedAndClimbStillApply()
    {
        var incomplete = new RealWorldVehicleSpec(5000F, 250F, null, null,
            new RealWorldVehicleSpec.Aircraft(null, null, 8F, 12F, 1), null, null);
        var performance = HelicopterPhysics.resolve(incomplete, 1F, 0.08F, 1F, 0.5D);
        assertEquals(legacy().maximumLift(), performance.maximumLift(), EPSILON);
        assertEquals(VehiclePhysicsUnits.kmhToBlocksPerTick(250D, 0.5D), performance.terminalSpeed(), EPSILON);
        assertEquals(0.2D, performance.climbSpeed(), EPSILON);
    }

    @Test
    void publishedClimbAndLevelSpeedAreStableForceEquilibria()
    {
        var performance = HelicopterPhysics.resolve(spec(5000F, 1000F, 250F, 8F, 12F, 1), 1F, 0.08F, 1F, 1D);
        Vec3 climbing = new Vec3(0D, performance.climbSpeed(), 0D);
        assertEquals(climbing.y, HelicopterPhysics.step(climbing, UP, performance, 1F, 1F, 1F).y, EPSILON);
        double upY = HelicopterPhysics.GRAVITY / performance.maximumLift();
        Vec3 tilted = new Vec3(Math.sqrt(1D - upY * upY), upY, 0D);
        Vec3 cruise = new Vec3(performance.terminalSpeed(), 0D, 0D);
        assertEquals(cruise.x, HelicopterPhysics.step(cruise, tilted, performance, 1F, 1F, 1F).x, EPSILON);
        Vec3 velocity = Vec3.ZERO;
        for (int tick = 0; tick < 5000; tick++)
            velocity = HelicopterPhysics.step(velocity, tilted, performance, 1F, 1F, 1F);
        assertEquals(cruise.x, velocity.x, 0.01D);
        assertEquals(0D, velocity.y, EPSILON);
    }

    @Test
    void spinUpAndCoastDownRemainBoundedAndCannotReverse()
    {
        float rpm = 0F;
        for (int tick = 0; tick < 61; tick++)
            rpm = HelicopterPhysics.spool(rpm, true);
        assertEquals(1F, rpm);
        assertTrue(HelicopterPhysics.spool(rpm, false) > 0F);
        for (int tick = 0; tick < 101; tick++)
            rpm = HelicopterPhysics.spool(rpm, false);
        assertEquals(0F, rpm);
        assertEquals(0F, HelicopterPhysics.spool(Float.NaN, false));
    }

    @Test
    void fractionalAndCounterRotatingAnimationsStayContinuousAcrossBaseWraps()
    {
        assertEquals(180F, HelicopterPhysics.rotorAngle(350D, 370D, 0.5F, 0.5F));
        assertEquals(-180F, HelicopterPhysics.rotorAngle(350D, 370D, 0.5F, -0.5F));
        assertEquals(185F, HelicopterPhysics.rotorAngle(350D, 370D, 1F, 0.5F));
        assertEquals(200F, HelicopterPhysics.rotorAngle(0D, 400D, 0.5F, 1F));
        assertEquals(0F, HelicopterPhysics.rotorAngle(1D, 2D, 1F, Float.NaN));
    }
}
