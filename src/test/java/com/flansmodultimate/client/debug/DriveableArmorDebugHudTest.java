package com.flansmodultimate.client.debug;

import com.flansmodultimate.common.driveables.collision.AAGunCollisionHelper;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriveableArmorDebugHudTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aaGunHudUsesNearestHullHealthAndTracksRepairsAndWrecks()
    {
        AAGun near = gun(-3D, 500);
        AAGun far = gun(-6D, 1000);
        Vec3 origin = new Vec3(0D, 1D, 0D);
        Vec3 motion = new Vec3(0D, 0D, -8D);
        var aimed = DriveableArmorDebugHud.aimedAAGun(List.of(far, near), origin, motion);
        assertNotNull(aimed);
        assertEquals(0.25F, aimed.time());
        assertEquals(500F, aimed.health().current());
        assertEquals(1000F, aimed.health().maximum());
        assertEquals(0xFFFF55, DriveableArmorDebugHud.healthColor(aimed.health()));
        assertNull(aimed.armor());

        when(near.getHealth()).thenReturn(1000);
        aimed = DriveableArmorDebugHud.aimedAAGun(List.of(near), origin, motion);
        assertEquals(0x55FF55, DriveableArmorDebugHud.healthColor(aimed.health()));
        when(near.getHealth()).thenReturn(0);
        when(near.isWreck()).thenReturn(true);
        when(near.isAlive()).thenReturn(false);
        aimed = DriveableArmorDebugHud.aimedAAGun(List.of(near), origin, motion);
        assertEquals(0F, aimed.health().current());
        assertEquals(0xFF5555, DriveableArmorDebugHud.healthColor(aimed.health()));
        when(near.isRemoved()).thenReturn(true);
        assertNull(DriveableArmorDebugHud.aimedAAGun(List.of(near), origin, motion));
        assertNull(DriveableArmorDebugHud.aimedAAGun(List.of(far), origin, new Vec3(0D, 0D, 8D)));
    }

    private static AAGun gun(double z, int health)
    {
        AAGun gun = mock(AAGun.class);
        AAGunType type = mock(AAGunType.class);
        when(type.getHealth()).thenReturn(1000);
        when(gun.getConfigType()).thenReturn(type);
        when(gun.getZ()).thenReturn(z);
        when(gun.getHealth()).thenReturn(health);
        AAGunCollisionHelper hull = new AAGunCollisionHelper(gun, 2F, 2F);
        when(gun.clipCollisionBox(any(), any(), anyDouble())).thenAnswer(call -> hull.clipSegment(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
        return gun;
    }
}
