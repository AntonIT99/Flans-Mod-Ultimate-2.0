package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Hold-to-throw grenades turn the ordinary throw into the Labjac Edition's overhand and underhand throws. */
class GrenadeTypeHandThrowTest
{
    private static final double EPSILON = 1.0E-6D;

    @Test
    void overhandLobsHalfAgainAsHighAtTheSameSpeed()
    {
        GrenadeType type = grenade("HoldToThrow true", "ThrowSpeed 1");
        Vec3 legacy = new Vec3(0.3D, 0.2D, 0.1D);
        Vec3 velocity = type.handThrowVelocity(legacy, false, 1F);
        assertEquals(legacy.length(), velocity.length(), EPSILON);
        assertEquals(1D, velocity.y / velocity.x, EPSILON);
        assertEquals(3D, velocity.x / velocity.z, EPSILON);
    }

    @Test
    void underhandIsSlowerFlatterAndKickedUpwards()
    {
        GrenadeType type = grenade("HoldToThrow true", "ThrowSpeed 2", "UnderhandThrowSpeedMultiplier 0.5");
        Vec3 legacy = new Vec3(0.4D, 0.2D, 0D);
        Vec3 velocity = type.handThrowVelocity(legacy, true, 1F);
        assertEquals(legacy.length() * 0.5D, velocity.length(), EPSILON);
        assertEquals((0.2D * 0.5D * 0.6D + 0.08D * 2D) / (0.4D * 0.5D), velocity.y / velocity.x, EPSILON);
    }

    @Test
    void throwSpeedMultipliersScaleTheThrow()
    {
        GrenadeType type = grenade("HoldToThrow true", "OverhandThrowSpeedMultiplier 2");
        Vec3 velocity = type.handThrowVelocity(new Vec3(1D, 0D, -1D), false, 1F);
        assertEquals(2D, velocity.x, EPSILON);
        assertEquals(-2D, velocity.z, EPSILON);
    }

    @Test
    void chargeScalesTheThrowAndIsClamped()
    {
        GrenadeType type = grenade("HoldToThrow true");
        Vec3 legacy = new Vec3(1D, 0D, 0D);
        assertEquals(0.25D, type.handThrowVelocity(legacy, false, 0.25F).x, EPSILON);
        assertEquals(0D, type.handThrowVelocity(legacy, false, -1F).length(), EPSILON);
        assertEquals(1D, type.handThrowVelocity(legacy, false, 3F).x, EPSILON);
        assertEquals(1D, type.handThrowVelocity(legacy, false, Float.NaN).x, EPSILON);
    }

    @Test
    void quickClickThrowsAtFullSpeedAndHoldingChargesFromNothing()
    {
        GrenadeType type = grenade("HoldToThrow true", "ThrowChargeTime 20");
        assertEquals(1F, type.handThrowCharge(0), EPSILON);
        assertEquals(1F, type.handThrowCharge(GrenadeType.QUICK_THROW_TICKS), EPSILON);
        assertEquals(0.05F, type.handThrowCharge(GrenadeType.QUICK_THROW_TICKS + 1F), EPSILON);
        assertEquals(0.5F, type.handThrowCharge(GrenadeType.QUICK_THROW_TICKS + 10F), EPSILON);
        assertEquals(1F, type.handThrowCharge(GrenadeType.QUICK_THROW_TICKS + 40F), EPSILON);
    }

    @Test
    void noChargeTimeAlwaysThrowsAtFullSpeed()
    {
        GrenadeType type = grenade("HoldToThrow true", "ThrowChargeTime 0");
        assertEquals(1F, type.handThrowCharge(GrenadeType.QUICK_THROW_TICKS + 2F), EPSILON);
    }

    private static GrenadeType grenade(String... lines)
    {
        GrenadeType type = new GrenadeType();
        type.load(new TypeFile("testGrenade", EnumType.GRENADE, new ContentPack("test", Path.of("build", "test-packs", "grenades")), List.of(lines)));
        return type;
    }
}
