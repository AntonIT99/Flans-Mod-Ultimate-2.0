package com.flansmodultimate.common.driveables.damage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Pins how a wreck tumbles, drags, slides, sinks and lands. */
class WreckPhysicsTest
{
    private static final double GRAVITY = 0.08D;

    @Test
    @DisplayName("A wreck destroyed standing still does not tumble; one thrown hard does")
    void tumbleGrowsWithSpeed()
    {
        assertEquals(0F, WreckPhysics.tumbleFrom(0D, 1D));
        assertTrue(WreckPhysics.tumbleFrom(0.5D, 1D) > 0F);
        assertTrue(WreckPhysics.tumbleFrom(1D, 1D) > WreckPhysics.tumbleFrom(0.3D, 1D), "a faster wreck tumbles harder");
        assertTrue(WreckPhysics.tumbleFrom(0.5D, -1D) < 0F, "the spread decides which way it goes over");
        assertEquals(WreckPhysics.MAX_SPIN, WreckPhysics.tumbleFrom(100D, 1D), "however hard it was hit");
        assertEquals(0F, WreckPhysics.tumbleFrom(Double.NaN, 1D));
    }

    @Test
    @DisplayName("The ground takes a tumble out of a wreck; the air barely does")
    void contactStopsTheTumble()
    {
        float spin = 10F;
        assertTrue(WreckPhysics.dampSpin(spin, false) > WreckPhysics.dampSpin(spin, true));
        assertTrue(WreckPhysics.dampSpin(spin, false) > spin * 0.9F, "a wreck in the air keeps spinning");
        float contact = spin;
        for (int tick = 0; tick < 20 && contact != 0F; tick++)
            contact = WreckPhysics.dampSpin(contact, true);
        assertEquals(0F, contact, "a tumble on the ground stops rather than creeping on for ever");
        assertEquals(0F, WreckPhysics.dampSpin(WreckPhysics.SPIN_EPSILON * 0.5F, false), "the last of a tumble is dropped");
        assertEquals(0F, WreckPhysics.dampSpin(Float.NaN, false));
    }

    @Test
    @DisplayName("A hull on its wheels rolls on; one dragging its belly ploughs to a stop")
    void runningGearDecidesTheDrag()
    {
        assertTrue(WreckPhysics.groundDragRetention(true) > WreckPhysics.groundDragRetention(false));
        assertTrue(WreckPhysics.groundDragRetention(false) > 0D);
        assertTrue(WreckPhysics.groundDragRetention(true) < 1D, "nothing rolls for ever");
        assertTrue(WreckPhysics.waterDragRetention() < 1D);
    }

    @Test
    @DisplayName("A shallow slope holds a wreck; a steep one gets it moving downhill")
    void onlySteepSlopesSlide()
    {
        assertEquals(0D, WreckPhysics.slideAcceleration(0D, GRAVITY), "level ground holds it");
        assertEquals(0D, WreckPhysics.slideAcceleration(0.3D, GRAVITY), "and so does a shallow slope");
        double gentle = WreckPhysics.slideAcceleration(0.6D, GRAVITY);
        double steep = WreckPhysics.slideAcceleration(1.2D, GRAVITY);
        assertTrue(gentle > 0D);
        assertTrue(steep > gentle, "a steeper slope pulls harder");
        assertTrue(steep < GRAVITY, "but never harder than gravity itself");
        assertEquals(-gentle, WreckPhysics.slideAcceleration(-0.6D, GRAVITY), 1.0E-9D, "it slides towards the lower ground");
        assertEquals(0D, WreckPhysics.slideAcceleration(Double.NaN, GRAVITY));
        assertEquals(0D, WreckPhysics.slideAcceleration(1D, 0D));
    }

    @Test
    @DisplayName("A hull built to float goes down more slowly than one that was not")
    void flotationSlowsTheSinking()
    {
        double ship = WreckPhysics.sinkSpeed(0.1F, true);
        double tank = WreckPhysics.sinkSpeed(0F, false);
        assertTrue(ship < tank, "a ship's compartments hold air");
        assertTrue(ship > 0D, "but it still goes down");
        assertTrue(WreckPhysics.sinkSpeed(0.25F, true) < WreckPhysics.sinkSpeed(0.02F, true), "more buoyancy, slower");
        assertEquals(WreckPhysics.sinkSpeed(0F, false), WreckPhysics.sinkSpeed(Float.NaN, false), "an unreadable buoyancy sinks like none");
    }

    @Test
    @DisplayName("A filling hull lists further over each tick, up to its beam ends")
    void listingReachesItsLimit()
    {
        float roll = 0F;
        for (int tick = 0; tick < 2000; tick++)
            roll = WreckPhysics.listAngle(roll, true);
        assertTrue(roll > 20F, "it does go over");
        float settled = WreckPhysics.listAngle(roll, true);
        assertEquals(roll, settled, "and stops there rather than rolling right round");
        assertTrue(WreckPhysics.listAngle(0F, false) < 0F, "the other side lists the other way");
        float pitch = 0F;
        for (int tick = 0; tick < 2000; tick++)
            pitch = WreckPhysics.trimAngle(pitch, true);
        assertTrue(pitch > 0F);
        assertEquals(pitch, WreckPhysics.trimAngle(pitch, true), "the trim is bounded too");
    }

    @ParameterizedTest
    @CsvSource({"0.0, 0.0", "0.25, 0.0", "0.3, 0.0", "1.6, 1.0", "4.0, 1.0"})
    @DisplayName("Settling onto the ground is no impact; going straight in is the worst there is")
    void impactSeverityRunsFromNothingToWorst(double fallSpeed, float expected)
    {
        assertEquals(expected, WreckPhysics.impactSeverity(fallSpeed), 0.06F);
    }

    @Test
    @DisplayName("A wreck coming down crushes what is under it, a heavy one worse")
    void crushGrowsWithTheLanding()
    {
        assertEquals(0F, WreckPhysics.crushDamage(0F, true), "a wreck that settles crushes nothing");
        assertTrue(WreckPhysics.crushDamage(1F, true) > WreckPhysics.crushDamage(0.2F, true));
        assertTrue(WreckPhysics.crushDamage(1F, false) < WreckPhysics.crushDamage(1F, true), "a light machine does less");
        assertEquals(0F, WreckPhysics.crushDamage(Float.NaN, true));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0D, 0.1D, 0.24D})
    @DisplayName("A span too short to measure gives no gradient")
    void shortSpansGiveNoGradient(double span)
    {
        assertEquals(0D, WreckPhysics.gradient(64D, 68D, span));
    }

    @Test
    @DisplayName("The gradient is the rise between two samples over the distance between them")
    void gradientMeasuresTheSlope()
    {
        assertEquals(1D, WreckPhysics.gradient(64D, 68D, 4D));
        assertEquals(-1D, WreckPhysics.gradient(68D, 64D, 4D));
        assertEquals(0D, WreckPhysics.gradient(64D, 64D, 4D));
        assertEquals(0D, WreckPhysics.gradient(Double.NaN, 64D, 4D));
    }
}
