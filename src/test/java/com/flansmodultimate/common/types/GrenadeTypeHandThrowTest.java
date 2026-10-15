package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Hold-to-throw grenades turn the ordinary throw into the Labjac Edition's overhand and underhand throws. */
class GrenadeTypeHandThrowTest
{
    private static final double EPSILON = 1.0E-6D;

    @Test
    void overhandLobsHalfAgainAsHigh()
    {
        GrenadeType type = grenade("HoldToThrow true", "ThrowSpeed 1");
        Vec3 velocity = type.handThrowVelocity(new Vec3(0.3D, 0.2D, 0.1D), false);
        assertEquals(0.3D, velocity.x, EPSILON);
        assertEquals(0.3D, velocity.y, EPSILON);
        assertEquals(0.1D, velocity.z, EPSILON);
    }

    @Test
    void underhandIsSlowerFlatterAndKickedUpwards()
    {
        GrenadeType type = grenade("HoldToThrow true", "ThrowSpeed 2", "UnderhandThrowSpeedMultiplier 0.5");
        Vec3 velocity = type.handThrowVelocity(new Vec3(0.4D, 0.2D, 0D), true);
        assertEquals(0.2D, velocity.x, EPSILON);
        assertEquals(0.2D * 0.5D * 0.6D + 0.08D * 2D, velocity.y, EPSILON);
    }

    @Test
    void throwSpeedMultipliersScaleTheThrow()
    {
        GrenadeType type = grenade("HoldToThrow true", "OverhandThrowSpeedMultiplier 2");
        Vec3 velocity = type.handThrowVelocity(new Vec3(1D, 0D, -1D), false);
        assertEquals(2D, velocity.x, EPSILON);
        assertEquals(-2D, velocity.z, EPSILON);
    }

    private static GrenadeType grenade(String... lines)
    {
        GrenadeType type = new GrenadeType();
        type.load(new TypeFile("testGrenade", EnumType.GRENADE, new ContentPack("test", Path.of("build", "test-packs", "grenades")), List.of(lines)));
        return type;
    }
}
