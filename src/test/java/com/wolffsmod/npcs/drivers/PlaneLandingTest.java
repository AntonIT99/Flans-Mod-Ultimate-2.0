package com.wolffsmod.npcs.drivers;

import com.wolffsmod.npcs.drivers.PlaneLanding.Stage;
import com.wolffsmod.npcs.drivers.PlaneLanding.Step;
import com.wolffsmod.npcs.drivers.PlaneLanding.View;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class PlaneLandingTest
{
    private static final Vec3 SITE = new Vec3(0D, 64D, 0D);
    /** Flying north toward the site, a block a tick. */
    private static final Vec3 NORTH = new Vec3(0D, 0D, -1D);

    private static View flying(double z, double height)
    {
        return new View(new Vec3(0D, 64D + height, z), NORTH, 180F, false, height, 64D, 1D, 0.6D);
    }

    @Test
    void aFarPlaneLinesUpOnItsWayInAndGlidesDownFromThePointShortOfTheSite()
    {
        PlaneLanding landing = new PlaneLanding();
        Step out = landing.land(flying(400D, 30D), SITE);
        assertEquals(Stage.APPROACH, out.stage());
        assertEquals(180F, Math.abs(out.heading()), 1.0E-3F);
        Step glide = landing.land(flying(PlaneLanding.APPROACH_DISTANCE, 1D + PlaneLanding.APPROACH_DISTANCE * PlaneLanding.GLIDE_SLOPE), SITE);
        assertEquals(Stage.FINAL, glide.stage());
        assertTrue(glide.throttle() < 1F);
        // Above its glide it noses down, below it up.
        assertTrue(landing.land(flying(64D, 30D), SITE).pitch() < 0F);
        assertTrue(landing.land(flying(64D, 2D), SITE).pitch() > 0F);
    }

    @Test
    void justAboveTheGroundItFlaresAndOnItsWheelsItBrakesToAStop()
    {
        PlaneLanding landing = new PlaneLanding();
        landing.land(flying(400D, 30D), SITE);
        landing.land(flying(PlaneLanding.APPROACH_DISTANCE, 12D), SITE);
        Step flare = landing.land(flying(8D, 1D + PlaneLanding.FLARE_HEIGHT - 0.5D), SITE);
        assertEquals(Stage.FLARE, flare.stage());
        assertEquals(0F, flare.throttle());
        assertEquals(PlaneLanding.FLARE_PITCH, flare.pitch());
        Step rollout = landing.land(new View(new Vec3(0D, 65D, 2D), NORTH, 180F, true, 1D, 64D, 1D, 0.6D), SITE);
        assertEquals(Stage.ROLLOUT, rollout.stage());
        assertTrue(rollout.brake());
        assertEquals(Stage.LANDED, landing.land(new View(new Vec3(0D, 65D, 0D), Vec3.ZERO, 180F, true, 1D, 64D, 1D, 0.6D), SITE).stage());
    }

    @Test
    void aPlaneThatOvershootsGoesRoundAgain()
    {
        PlaneLanding landing = new PlaneLanding();
        landing.land(flying(400D, 30D), SITE);
        landing.land(flying(PlaneLanding.APPROACH_DISTANCE, 12D), SITE);
        assertEquals(Stage.APPROACH, landing.land(flying(-PlaneLanding.OVERSHOOT - 10D, 10D), SITE).stage());
        landing.abort();
        assertFalse(landing.active());
    }
}
