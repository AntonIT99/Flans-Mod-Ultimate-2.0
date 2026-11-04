package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.api.driveables.AircraftDamage;
import com.flansmodultimate.api.driveables.AircraftMode;
import com.flansmodultimate.api.driveables.CrashMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pins how an aircraft's wear costs it in the air, and how what it loses decides how it comes down. */
class AircraftDamageModelTest
{
    private static AircraftDamage wings(float left, float right)
    {
        return new AircraftDamage(left, right, 1F, 1F, 1F, 1F, 1F);
    }

    @Test
    @DisplayName("An intact aircraft loses nothing, and the shares are kept in range")
    void intactCostsNothing()
    {
        AircraftDamage intact = AircraftDamage.INTACT;
        assertTrue(intact.isIntact());
        assertEquals(1F, AircraftDamageModel.liftFraction(intact));
        assertEquals(0F, AircraftDamageModel.rollImbalance(intact));
        assertEquals(1F, AircraftDamageModel.tailAuthority(intact));
        assertEquals(1F, AircraftDamageModel.thrustFraction(intact));
        assertEquals(0F, AircraftDamageModel.misfireChance(intact));
        assertEquals(0F, AircraftDamageModel.fuelLeakPerSecond(intact));
        assertEquals(0F, AircraftDamageModel.vibration(intact, AircraftMode.FIXED_WING));
        assertEquals(CrashMode.NONE, AircraftDamageModel.crashMode(intact, AircraftMode.FIXED_WING));
        AircraftDamage odd = new AircraftDamage(Float.NaN, 2F, -1F, 1F, 1F, 1F, 1F);
        assertEquals(1F, odd.leftWing());
        assertEquals(1F, odd.rightWing());
        assertEquals(0F, odd.tail());
    }

    @Test
    @DisplayName("A worn wing lifts less and pulls the aircraft over toward itself, as a lost one does in full")
    void wornWingRollsTowardItself()
    {
        AircraftDamage worn = wings(0.5F, 1F);
        assertTrue(AircraftDamageModel.liftFraction(worn) < 1F);
        assertTrue(AircraftDamageModel.liftFraction(worn) > 0.5F);
        assertTrue(AircraftDamageModel.rollImbalance(worn) < 0F, "the weaker left wing drops: a roll to the left");
        assertTrue(AircraftDamageModel.rollImbalance(wings(1F, 0.5F)) > 0F);
        // At the ends it matches what losing the wing outright always did: half the lift and a full roll.
        assertEquals(0.5F, AircraftDamageModel.liftFraction(wings(0F, 1F)), 1.0E-6F);
        assertEquals(-1F, AircraftDamageModel.rollImbalance(wings(0F, 1F)), 1.0E-6F);
        assertTrue(Math.abs(AircraftDamageModel.rollImbalance(worn)) < 0.25F, "a holed wing can still be held level with the stick");
    }

    @Test
    @DisplayName("Holed wing tanks leak, worse the more holed, worst once the wing is gone")
    void holedWingsLeakFuel()
    {
        assertEquals(0F, AircraftDamageModel.fuelLeakPerSecond(wings(0.7F, 1F)));
        float light = AircraftDamageModel.fuelLeakPerSecond(wings(0.5F, 1F));
        float heavy = AircraftDamageModel.fuelLeakPerSecond(wings(0.1F, 1F));
        float torn = AircraftDamageModel.fuelLeakPerSecond(wings(0F, 1F));
        assertTrue(light > 0F);
        assertTrue(heavy > light);
        assertTrue(torn > heavy);
        assertEquals(2F * heavy, AircraftDamageModel.fuelLeakPerSecond(wings(0.1F, 0.1F)), 1.0E-6F);
    }

    @Test
    @DisplayName("A hit engine loses power slowly at first, then misfires and fails")
    void engineLosesPowerAndMisfires()
    {
        float previous = 1F;
        for (float health = 1F; health > 0F; health -= 0.1F)
        {
            AircraftDamage damage = new AircraftDamage(1F, 1F, 1F, 1F, health, 1F, 1F);
            float thrust = AircraftDamageModel.thrustFraction(damage);
            assertTrue(thrust <= previous + 1.0E-6F);
            assertTrue(thrust > 0.4F);
            previous = thrust;
        }
        assertTrue(AircraftDamageModel.thrustFraction(new AircraftDamage(1F, 1F, 1F, 1F, 0.8F, 1F, 1F)) > 0.95F, "a scratched engine barely notices");
        assertEquals(0F, AircraftDamageModel.thrustFraction(new AircraftDamage(1F, 1F, 1F, 1F, 0F, 1F, 1F)));
        assertEquals(0F, AircraftDamageModel.misfireChance(new AircraftDamage(1F, 1F, 1F, 1F, 0.5F, 1F, 1F)));
        assertTrue(AircraftDamageModel.misfireChance(new AircraftDamage(1F, 1F, 1F, 1F, 0.1F, 1F, 1F)) > AircraftDamageModel.misfireChance(new AircraftDamage(1F, 1F, 1F, 1F, 0.3F, 1F, 1F)));
    }

    @Test
    @DisplayName("What a fixed-wing aircraft loses decides how it comes down")
    void fixedWingCrashModes()
    {
        assertEquals(CrashMode.SPIRAL_LEFT, AircraftDamageModel.crashMode(wings(0F, 1F), AircraftMode.FIXED_WING));
        assertEquals(CrashMode.SPIRAL_RIGHT, AircraftDamageModel.crashMode(wings(1F, 0F), AircraftMode.FIXED_WING));
        assertEquals(CrashMode.TUMBLE, AircraftDamageModel.crashMode(wings(0F, 0F), AircraftMode.FIXED_WING));
        assertEquals(CrashMode.FLAT_SPIN, AircraftDamageModel.crashMode(new AircraftDamage(1F, 1F, 0F, 1F, 1F, 1F, 1F), AircraftMode.FIXED_WING));
        assertEquals(CrashMode.TUMBLE, AircraftDamageModel.crashMode(new AircraftDamage(0F, 1F, 0F, 1F, 1F, 1F, 1F), AircraftMode.FIXED_WING));
        assertEquals(CrashMode.DIVE, AircraftDamageModel.crashMode(new AircraftDamage(1F, 1F, 1F, 0F, 1F, 1F, 1F), AircraftMode.FIXED_WING));
        assertEquals(CrashMode.NONE, AircraftDamageModel.crashMode(new AircraftDamage(1F, 1F, 1F, 1F, 0F, 1F, 1F), AircraftMode.FIXED_WING), "a dead engine glides");
        assertEquals(CrashMode.DIVE, AircraftDamageModel.wreckMode(AircraftDamage.INTACT, AircraftMode.FIXED_WING), "a dead pilot's aircraft noses over");
    }

    @Test
    @DisplayName("What a rotorcraft loses decides how it comes down")
    void rotorcraftCrashModes()
    {
        assertEquals(CrashMode.ROTOR_LOSS, AircraftDamageModel.crashMode(new AircraftDamage(1F, 1F, 1F, 1F, 1F, 0F, 1F), AircraftMode.ROTORCRAFT));
        assertEquals(CrashMode.MAST_SPIN, AircraftDamageModel.crashMode(new AircraftDamage(1F, 1F, 1F, 1F, 1F, 1F, 0F), AircraftMode.ROTORCRAFT));
        assertEquals(CrashMode.MAST_SPIN, AircraftDamageModel.crashMode(new AircraftDamage(1F, 1F, 0F, 1F, 1F, 1F, 1F), AircraftMode.ROTORCRAFT));
        assertEquals(CrashMode.NONE, AircraftDamageModel.crashMode(wings(0F, 0F), AircraftMode.ROTORCRAFT), "a helicopter has no wings to lose");
        assertEquals(CrashMode.ROTOR_LOSS, AircraftDamageModel.wreckMode(AircraftDamage.INTACT, AircraftMode.ROTORCRAFT));
        assertTrue(AircraftDamageModel.rotorLift(new AircraftDamage(1F, 1F, 1F, 1F, 1F, 0.2F, 1F)) < 1F);
        assertEquals(0F, AircraftDamageModel.tailRotorAuthority(new AircraftDamage(1F, 1F, 1F, 1F, 1F, 1F, 0F)));
    }
}
