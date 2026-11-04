package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.driveables.damage.PlaneCrashDamage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

/** Pins autorotation: a helicopter that has lost its engine can still be brought down in one piece. */
class HelicopterAutorotationTest
{
    @Test
    @DisplayName("Air rising through the disc keeps an unpowered rotor turning; without it the rotor runs down")
    void inflowDrivesTheRotor()
    {
        float descending = 0.5F;
        float hovering = 0.5F;
        for (int tick = 0; tick < 200; tick++)
        {
            descending = HelicopterPhysics.autorotate(descending, HelicopterPhysics.AUTOROTATION_INFLOW, 0D, 0F);
            hovering = HelicopterPhysics.autorotate(hovering, 0D, 0D, 0F);
        }
        assertEquals(1F, descending, 1.0E-4F, "a steady autorotating descent holds the rotor at speed");
        assertEquals(0F, hovering, "a rotor with no air through it stops");
        float gliding = 0.5F;
        for (int tick = 0; tick < 200; tick++)
            gliding = HelicopterPhysics.autorotate(gliding, 0.2D, 1D, 0F);
        assertTrue(gliding > 0.9F, "forward speed helps to keep it turning");
        assertTrue(HelicopterPhysics.autorotate(1F, 5D, 5D, 0F) <= HelicopterPhysics.MAX_WINDMILL_SPEED);
    }

    @Test
    @DisplayName("Raising the collective spends the rotor's speed: a flare cushions the landing but cannot be held")
    void flareSpendsRotorSpeed()
    {
        float rotor = 1F;
        for (int tick = 0; tick < 60; tick++)
            rotor = HelicopterPhysics.autorotate(rotor, 0D, 0D, 1F);
        assertTrue(rotor < 0.3F, "held up for three seconds, the flare has bled the rotor dry");
        float lowered = HelicopterPhysics.autorotate(1F, HelicopterPhysics.AUTOROTATION_INFLOW, 0D, 0.1F);
        float raised = HelicopterPhysics.autorotate(1F, HelicopterPhysics.AUTOROTATION_INFLOW, 0D, 1F);
        assertTrue(raised < lowered);
    }

    @Test
    @DisplayName("A windmilling disc slows the descent like a parachute, harder the faster it turns, and never lifts")
    void discDragSlowsTheDescent()
    {
        Vec3 falling = new Vec3(0.3D, -0.8D, 0D);
        Vec3 turning = HelicopterPhysics.autorotationDrag(falling, 1F);
        Vec3 stopped = HelicopterPhysics.autorotationDrag(falling, 0F);
        assertEquals(falling, stopped);
        assertTrue(turning.y > falling.y);
        assertTrue(turning.y <= 0D);
        assertEquals(falling.x, turning.x);
        Vec3 climbing = new Vec3(0D, 0.2D, 0D);
        assertSame(climbing, HelicopterPhysics.autorotationDrag(climbing, 1F));
        // At the inflow that keeps the rotor at speed, the disc's drag matches the helicopter's gravity.
        double brake = HelicopterPhysics.autorotationDrag(new Vec3(0D, -HelicopterPhysics.AUTOROTATION_INFLOW, 0D), 1F).y + HelicopterPhysics.AUTOROTATION_INFLOW;
        assertEquals(HelicopterPhysics.GRAVITY, brake, 0.002D);
    }

    @Test
    @DisplayName("A belly slide scrapes the airframe while it is fast, and nothing once it has slowed")
    void bellySlideScrapes()
    {
        assertEquals(0F, PlaneCrashDamage.bellyScrape(PlaneCrashDamage.BELLY_SLIDE_SPEED, 1F));
        assertEquals(0F, PlaneCrashDamage.bellyScrape(1D, 0F));
        assertEquals(0F, PlaneCrashDamage.bellyScrape(Double.NaN, 1F));
        float slow = PlaneCrashDamage.bellyScrape(0.5D, 1F);
        float fast = PlaneCrashDamage.bellyScrape(1.5D, 1F);
        assertTrue(slow > 0F);
        assertTrue(fast > slow);
        // A slide from landing speed loses a share of the core, not the aircraft.
        double speed = 1.5D;
        float scraped = 0F;
        while (speed > PlaneCrashDamage.BELLY_SLIDE_SPEED)
        {
            scraped += PlaneCrashDamage.bellyScrape(speed, 1F);
            speed *= PlaneCrashDamage.BELLY_FRICTION_RETENTION;
        }
        assertTrue(scraped > 0.01F && scraped < 0.2F, "a belly landing is survivable: " + scraped);
    }
}
