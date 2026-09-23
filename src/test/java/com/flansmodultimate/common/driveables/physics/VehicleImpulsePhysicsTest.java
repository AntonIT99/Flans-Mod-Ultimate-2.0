package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.driveables.physics.VehicleImpulsePhysics.CollisionImpulse;
import com.flansmodultimate.common.driveables.physics.VehicleImpulsePhysics.ImpulseMass;
import com.flansmodultimate.common.driveables.physics.VehicleImpulsePhysics.MassSource;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Outside pushes must be weighed against mass the way momentum conservation
 * would weigh them, so a tank shrugs off what throws a jeep.
 */
class VehicleImpulsePhysicsTest
{
    private static final double REFERENCE_KG = 100D;
    private static final double FALLBACK_KG = 10_000D;
    private static final double TANK_KG = 64_000D;
    private static final double HMMWV_KG = 2_400D;
    private static final double JEEP_KG = 1_100D;

    // ------------------------------------------------------------ mass

    @Test
    void realMassWinsOverLegacyMass()
    {
        ImpulseMass mass = VehicleImpulsePhysics.resolveMass(64_000F, 1_500F, FALLBACK_KG);
        assertEquals(64_000F, mass.massKg());
        assertEquals(MassSource.REAL_MASS, mass.source());
    }

    @Test
    void legacyMassIsReadAsKilograms()
    {
        ImpulseMass mass = VehicleImpulsePhysics.resolveMass(null, 2_666F, FALLBACK_KG);
        assertEquals(2_666F, mass.massKg());
        assertEquals(MassSource.LEGACY_MASS, mass.source());
    }

    @Test
    void placeholderLegacyMassesFallBack()
    {
        // Aircraft placeholders authored in shipped packs, already in kilograms.
        for (float placeholder : new float[] { 2.7F, 3.2F, 16.7F, 99F })
        {
            ImpulseMass mass = VehicleImpulsePhysics.resolveMass(null, placeholder, FALLBACK_KG);
            assertEquals(MassSource.FALLBACK, mass.source(), "Mass " + placeholder);
            assertEquals((float) FALLBACK_KG, mass.massKg());
        }
    }

    @Test
    void missingMassUsesTheClassFallback()
    {
        ImpulseMass mass = VehicleImpulsePhysics.resolveMass(null, null, FALLBACK_KG);
        assertEquals((float) FALLBACK_KG, mass.massKg());
        assertEquals(MassSource.FALLBACK, mass.source());
    }

    @Test
    void anUnusableRealMassFallsThroughToLegacyMass()
    {
        assertEquals(MassSource.LEGACY_MASS, VehicleImpulsePhysics.resolveMass(Float.NaN, 3_000F, FALLBACK_KG).source());
        assertEquals(MassSource.LEGACY_MASS, VehicleImpulsePhysics.resolveMass(0F, 3_000F, FALLBACK_KG).source());
    }

    @Test
    void resolvedMassNeverDropsBelowTheFloor()
    {
        float floor = VehiclePhysicsConstants.MIN_IMPULSE_MASS_KG;
        assertEquals(floor, VehicleImpulsePhysics.resolveMass(null, null, 1D).massKg());
        assertEquals(floor, VehicleImpulsePhysics.resolveMass(10F, null, FALLBACK_KG).massKg());
        assertEquals(floor, VehicleImpulsePhysics.resolveMass(null, null, Double.NaN).massKg());
    }

    // ------------------------------------------------------- knockback

    @Test
    void aBodyAtOrBelowTheReferenceMassKeepsTheWholePush()
    {
        assertEquals(1D, VehicleImpulsePhysics.knockbackScale(REFERENCE_KG, REFERENCE_KG));
        assertEquals(1D, VehicleImpulsePhysics.knockbackScale(60D, REFERENCE_KG));
    }

    @Test
    void heavierBodiesKeepReferenceOverMass()
    {
        assertEquals(REFERENCE_KG / HMMWV_KG, VehicleImpulsePhysics.knockbackScale(HMMWV_KG, REFERENCE_KG), 1.0E-12D);
    }

    @Test
    void aPointBlankExplosionCannotMoveAMainBattleTank()
    {
        assertSame(Vec3.ZERO, VehicleImpulsePhysics.scaleExternalImpulse(new Vec3(1D, 0.3D, 0D), TANK_KG, REFERENCE_KG));
    }

    @Test
    void aPointBlankExplosionStillMovesALightJeep()
    {
        Vec3 scaled = VehicleImpulsePhysics.scaleExternalImpulse(new Vec3(1D, 0D, 0D), JEEP_KG, REFERENCE_KG);
        assertEquals(REFERENCE_KG / JEEP_KG, scaled.x, 1.0E-12D);
    }

    @Test
    void verticalPushIsWeighedLikeHorizontal()
    {
        Vec3 scaled = VehicleImpulsePhysics.scaleExternalImpulse(new Vec3(0.5D, 0.5D, 0D), 400D, REFERENCE_KG);
        assertEquals(0.125D, scaled.x, 1.0E-12D);
        assertEquals(0.125D, scaled.y, 1.0E-12D);
    }

    @Test
    void nonFiniteImpulsesAreDropped()
    {
        assertSame(Vec3.ZERO, VehicleImpulsePhysics.scaleExternalImpulse(new Vec3(Double.NaN, 0D, 0D), JEEP_KG, REFERENCE_KG));
    }

    // ------------------------------------------------------- collision

    @Test
    void collisionConservesMomentum()
    {
        CollisionImpulse impulse = VehicleImpulsePhysics.collision(TANK_KG, HMMWV_KG, 0.05D, 0.1D);
        assertEquals(0D, TANK_KG * impulse.selfDelta() + HMMWV_KG * impulse.otherDelta(), 1.0E-9D);
    }

    @Test
    void aTankBarelySlowsWhileTheVehicleItStrikesIsCarriedAlong()
    {
        CollisionImpulse impulse = VehicleImpulsePhysics.collision(TANK_KG, HMMWV_KG, 0.05D, 0.1D);
        assertTrue(-impulse.selfDelta() < 0.003D, "the tank loses almost none of its speed");
        assertTrue(impulse.otherDelta() > 0.05D, "the struck vehicle is carried along at the tank's speed");
    }

    @Test
    void aVehicleStrikingATankIsStoppedAndBarelyMovesIt()
    {
        double closing = 0.25D;
        CollisionImpulse impulse = VehicleImpulsePhysics.collision(HMMWV_KG, TANK_KG, closing, 0.1D);
        assertTrue(impulse.otherDelta() < 0.01D, "the tank gains less than 0.2 m/s");
        double strikerAfter = closing + impulse.selfDelta();
        assertTrue(strikerAfter < 0D && strikerAfter > -0.025D, "the striker stops and rebounds only slightly");
    }

    @Test
    void separationSpeedFollowsRestitution()
    {
        double closing = 0.2D;
        CollisionImpulse impulse = VehicleImpulsePhysics.collision(1_000D, 1_000D, closing, 0.1D);
        double separation = impulse.otherDelta() - (closing + impulse.selfDelta());
        assertEquals(0.1D * closing, separation, 1.0E-12D);
    }

    @Test
    void bodiesThatAreNotClosingExchangeNothing()
    {
        assertTrue(VehicleImpulsePhysics.collision(1_000D, 1_000D, 0D, 0.1D).isNone());
        assertTrue(VehicleImpulsePhysics.collision(1_000D, 1_000D, -0.1D, 0.1D).isNone());
        assertTrue(VehicleImpulsePhysics.collision(1_000D, 1_000D, Double.NaN, 0.1D).isNone());
    }

    // -------------------------------------------------------- friction

    @Test
    void minimumDecelerationNeverUndoesAHarderStop()
    {
        Vec3 after = new Vec3(0.1D, 0D, 0D);
        assertSame(after, VehicleImpulsePhysics.enforceMinimumDeceleration(new Vec3(0.2D, 0D, 0D), after, 0.015D, 0.002D));
    }

    @Test
    void minimumDecelerationSlowsACoastingModel()
    {
        Vec3 result = VehicleImpulsePhysics.enforceMinimumDeceleration(
            new Vec3(0.2D, 0D, 0D), new Vec3(0.2D, 0D, 0D), 0.015D, 0.002D);
        assertEquals(0.185D, result.x, 1.0E-12D);
    }

    @Test
    void minimumDecelerationLeavesVerticalMotionAlone()
    {
        Vec3 result = VehicleImpulsePhysics.enforceMinimumDeceleration(
            new Vec3(0.2D, -0.3D, 0D), new Vec3(0.2D, -0.4D, 0D), 0.015D, 0.002D);
        assertEquals(-0.4D, result.y);
    }

    @Test
    void crawlingMotionIsBroughtToRest()
    {
        Vec3 result = VehicleImpulsePhysics.enforceMinimumDeceleration(
            new Vec3(0.0025D, 0D, 0D), new Vec3(0.0025D, 0D, 0D), 0.001D, 0.002D);
        assertEquals(0D, result.horizontalDistance());
    }

    @Test
    void aParkedVehiclePushedAtFourMetresPerSecondStopsWithinASlide()
    {
        double deceleration = VehiclePhysicsUnits.metresPerSecondSquaredToBlocksPerTickSquared(
            VehiclePhysicsConstants.PARKED_GROUND_FRICTION_DECELERATION_MS2);
        Vec3 velocity = new Vec3(4D / VehiclePhysicsUnits.TICKS_PER_SECOND, 0D, 0D);
        double distance = 0D;
        int ticks = 0;
        while (velocity.horizontalDistance() > 0D && ticks < 100)
        {
            velocity = VehicleImpulsePhysics.enforceMinimumDeceleration(velocity, velocity, deceleration,
                VehiclePhysicsConstants.GROUND_REST_SPEED_BLOCKS_PER_TICK);
            distance += velocity.horizontalDistance();
            ticks++;
        }
        assertTrue(ticks <= 20, "stops within a second, took " + ticks + " ticks");
        assertTrue(distance <= 1.34D, "slides " + distance + " blocks");
    }
}
