package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.GroundHandling;
import com.flansmodultimate.api.GroundPropulsion;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.sim.NpcDriving.*;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class NpcPropulsionInheritanceTest
{
    private static final double TOP = 0.5D;

    /** A hull whose engine pulls the less the faster it goes, with brakes that do not depend on speed. */
    private record Engine(double launch, double braking, double rolling) implements GroundPropulsion
    {
        @Override
        public double topSpeed()
        {
            return TOP;
        }

        @Override
        public double reverseSpeed()
        {
            return 0.1D;
        }

        @Override
        public boolean realistic()
        {
            return true;
        }

        @Override
        public OptionalDouble massKg()
        {
            return OptionalDouble.empty();
        }

        @Override
        public OptionalDouble enginePowerKw()
        {
            return OptionalDouble.empty();
        }

        @Override
        public double acceleration(double speed)
        {
            return Math.max(0D, launch * (1D - Math.abs(speed) / TOP));
        }

        @Override
        public double deceleration(double speed, double brake)
        {
            return rolling + (braking - rolling) * brake;
        }
    }

    private static Settings settings(NpcTuning tuning, GroundPropulsion engine)
    {
        return Settings.of(tuning, new NpcWeaponOptions(), true, GroundHandling.EMPTY, engine);
    }

    @Test
    void responsesLeftAtZeroFollowTheEngineAtTheVehiclesSpeed()
    {
        Settings settings = settings(new NpcTuning(), new Engine(0.01D, 0.02D, 0.004D));
        assertEquals(0.02F, settings.accelerationAt(0D, TOP), 1.0E-6F);
        assertEquals(0.01F, settings.accelerationAt(TOP * 0.5D, TOP), 1.0E-6F);
        assertEquals(0F, settings.accelerationAt(TOP, TOP));
        assertEquals(0.04F, settings.brakingAt(TOP, TOP), 1.0E-6F);
        assertEquals(0.008F, settings.coastingAt(TOP, TOP), 1.0E-6F);
        // Without a reverse speed of its own the vehicle reverses as fast as its engine lets it.
        assertEquals(0.2F, settings.reverseShare(), 1.0E-6F);
        // Braking at 0.02 blocks per tick per tick from 0.5 blocks per tick takes 25 ticks over 6.25 blocks.
        assertEquals(6.25D, settings.stoppingDistance(TOP, TOP), 1.0E-6D);
    }

    @Test
    void anOwnLandSpeedKeepsTheEnginesTimingRatherThanItsBlocks()
    {
        Settings settings = settings(new NpcTuning(), new Engine(0.01D, 0.02D, 0.004D));
        // With twice the model's top speed, moving at the model's top speed is half throttle.
        assertEquals(settings.accelerationAt(TOP * 0.5D, TOP), settings.accelerationAt(TOP, TOP * 2D), 1.0E-6F);
    }

    @Test
    void customRatesAndMissingEnginesKeepFixedResponses()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.DRIVE_ACCELERATION, 200);
        Settings custom = settings(tuning, new Engine(0.01D, 0.02D, 0.004D));
        assertEquals(0.1F, custom.accelerationAt(0D, TOP), 1.0E-6F);
        assertEquals(0.04F, custom.brakingAt(TOP, TOP), 1.0E-6F);
        tuning.set(Value.DRIVE_BRAKING, 300);
        tuning.set(Value.DRIVE_COASTING, 40);
        assertNull(settings(tuning, new Engine(0.01D, 0.02D, 0.004D)).propulsion());
        Settings fallback = Settings.of(new NpcTuning(), new NpcWeaponOptions(), true, GroundHandling.EMPTY);
        assertEquals(0.05F, fallback.accelerationAt(0D, TOP), 1.0E-6F);
        assertEquals(0.125F, fallback.brakingAt(TOP, TOP), 1.0E-6F);
        assertEquals(0.03F, fallback.coastingAt(TOP, TOP), 1.0E-6F);
        assertEquals(0.4F, fallback.reverseShare(), 1.0E-6F);
    }

    @Test
    void aHeavierEnginePicksUpSpeedMoreSlowly()
    {
        Settings light = settings(new NpcTuning(), new Engine(0.02D, 0.02D, 0.004D));
        Settings heavy = settings(new NpcTuning(), new Engine(0.005D, 0.02D, 0.004D));
        assertTrue(throttleAfter(light, 20) > throttleAfter(heavy, 20) * 2F);
        assertTrue(throttleAfter(heavy, 400) > 0.95F);
    }

    /** Drives straight ahead with the NPC moving at the speed its throttle asks for. */
    private static float throttleAfter(Settings settings, int ticks)
    {
        NpcDriving driving = new NpcDriving();
        for (int tick = 0; tick < ticks; tick++)
            driving.drive(new Situation(0F, 0F, 1000D, 0F, Double.MAX_VALUE, 4D, driving.getThrottle() * TOP, 1F, 0D, TOP), settings);
        return driving.getThrottle();
    }

    @Test
    void aVehicleThatBrakesSlowlySlowsForACornerBeyondItsLookahead()
    {
        Settings slow = settings(new NpcTuning(), new Engine(0.02D, 0.002D, 0.001D));
        NpcDriving driving = new NpcDriving();
        for (int tick = 0; tick < 200; tick++)
            driving.drive(new Situation(0F, 0F, 1000D, 0F, Double.MAX_VALUE, 4D, driving.getThrottle() * TOP, 1F, 0D, TOP), slow);
        float cruising = driving.getThrottle();
        assertTrue(cruising > 0.99F);
        Situation corner = new Situation(0F, 0F, 1000D, 90F, 20D, 4D, cruising * TOP, 1F, 0D, TOP);
        // Twenty blocks out is far beyond the four-block lookahead, but stopping from full speed takes longer than that.
        assertTrue(driving.drive(corner, slow).throttle() < cruising);
        assertEquals(1F, NpcDriving.approachLimit(20D, TOP, 0.125F, 0.45F));
        assertEquals(0.45F, NpcDriving.approachLimit(0D, TOP, 0.002F, 0.45F), 1.0E-6F);
        assertEquals(1F, NpcDriving.approachLimit(0D, 0D, 0.002F, 0.45F));
    }
}
