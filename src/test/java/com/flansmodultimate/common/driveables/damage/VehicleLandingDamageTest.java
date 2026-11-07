package com.flansmodultimate.common.driveables.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleLandingDamageTest
{
    @Test
    void settlingOntoTheSuspensionIsNotALanding()
    {
        assertFalse(VehicleLandingDamage.evaluate(0D, 1F).damaging());
        assertFalse(VehicleLandingDamage.evaluate(VehicleLandingDamage.SAFE_DESCENT_SPEED, 1F).damaging());
        assertFalse(VehicleLandingDamage.evaluate(-1.5D, 1F).damaging());
        assertFalse(VehicleLandingDamage.evaluate(Double.NaN, 1F).damaging());
    }

    @Test
    void aThumpJustPastTheSafeSpeedIsNotWorthReporting()
    {
        assertFalse(VehicleLandingDamage.evaluate(0.67D, 1F).damaging());
    }

    @Test
    void clearingALedgeOnlyScuffsTheHull()
    {
        VehicleLandingDamage.Impact impact = VehicleLandingDamage.evaluate(0.8D, 1F);

        assertTrue(impact.damaging());
        assertTrue(impact.coreHealthFraction() < 0.05F, "a short drop should cost well under a twentieth of the core");
    }

    @Test
    void aTenBlockDropCostsAboutAFifthOfTheCoreAsLegacyDidOnItsOwnHulls()
    {
        // Legacy dealt a flat -descent * 50, which took about a fifth off the
        // roughly 120 hitpoint cores period packs authored. Reproducing that
        // share is what keeps the drop meaningful for a pack authoring thousands.
        VehicleLandingDamage.Impact impact = VehicleLandingDamage.evaluate(1.26D, 1F);

        assertEquals(0.2F, impact.coreHealthFraction(), 0.02F);
    }

    @Test
    void theRunningGearTakesAFifthOfWhatTheHullTakes()
    {
        VehicleLandingDamage.Impact impact = VehicleLandingDamage.evaluate(1.26D, 1F);

        assertEquals(impact.coreHealthFraction() * 0.2F, impact.runningGearHealthFraction(), 1.0E-6F);
    }

    @Test
    void harderLandingsCostMoreThanProportionallyMore()
    {
        VehicleLandingDamage.Impact gentle = VehicleLandingDamage.evaluate(1D, 1F);
        VehicleLandingDamage.Impact hard = VehicleLandingDamage.evaluate(1.5D, 1F);

        assertTrue(hard.coreHealthFraction() > gentle.coreHealthFraction() * 2F);
    }

    @Test
    void aLongDropWritesTheHullOff()
    {
        assertTrue(VehicleLandingDamage.evaluate(2.4D, 1F).writeOff());
        assertFalse(VehicleLandingDamage.evaluate(1.26D, 1F).writeOff());
    }

    @Test
    void severityNeverExceedsOneHoweverFarTheHullFell()
    {
        assertEquals(1F, VehicleLandingDamage.evaluate(50D, 1F).severity());
        assertEquals(1F, VehicleLandingDamage.evaluate(Double.MAX_VALUE, 1F).severity());
    }

    @Test
    void aForgivingFallDamageFactorScalesTheLossAndZeroMakesATypeImmune()
    {
        VehicleLandingDamage.Impact full = VehicleLandingDamage.evaluate(1.26D, 1F);
        VehicleLandingDamage.Impact half = VehicleLandingDamage.evaluate(1.26D, 0.5F);

        assertEquals(full.coreHealthFraction() * 0.5F, half.coreHealthFraction(), 1.0E-6F);
        assertFalse(VehicleLandingDamage.evaluate(1.26D, 0F).damaging());
        assertFalse(VehicleLandingDamage.evaluate(2.4D, 0F).damaging());
    }

    @Test
    void aGenerousFallDamageFactorCanWriteAHullOffOnALesserDrop()
    {
        assertFalse(VehicleLandingDamage.evaluate(1.5D, 1F).writeOff());
        assertTrue(VehicleLandingDamage.evaluate(1.5D, 4F).writeOff());
    }
}
