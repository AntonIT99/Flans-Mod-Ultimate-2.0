package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.entity.geometry.AAGunBarrelGeometry;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerProjectileAuthorityTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aaGunsUseAuthoredFallbacksOrServerMeasurementsIncludingSentries()
    {
        AAGun gun = mock(AAGun.class);
        AAGunType type = mock(AAGunType.class);
        when(gun.getConfigType()).thenReturn(type);
        when(gun.position()).thenReturn(Vec3.ZERO);
        when(type.getNumBarrels()).thenReturn(1);
        when(type.getBarrelX()).thenReturn(new float[]{16});
        when(type.getBarrelY()).thenReturn(new float[]{24});
        when(type.getBarrelZ()).thenReturn(new float[]{8});
        when(type.isSentry()).thenReturn(true);
        when(gun.getBarrelOrigin(anyInt(), anyBoolean())).thenCallRealMethod();
        Vec3 expected = AAGunBarrelGeometry.legacyBarrelOffset(16, 24, 8, 0, 0);
        assertEquals(expected, gun.getBarrelOrigin(0, false));
        assertEquals(expected.add(0, AAGunBarrelGeometry.SENTRY_ORIGIN_Y_OFFSET, 0), gun.getBarrelOrigin(0, true));
        Vec3 pivot = new Vec3(0, 20, 0);
        Vec3 muzzle = new Vec3(30, 0, 4);
        when(type.hasMeasuredBarrels()).thenReturn(true);
        when(type.getMeasuredBarrelPivots()).thenReturn(new Vec3[]{pivot});
        when(type.getMeasuredBarrelMuzzles()).thenReturn(new Vec3[]{muzzle});
        assertEquals(AAGunBarrelGeometry.modelBarrelOffset(pivot, muzzle, 0, 0), gun.getBarrelOrigin(0, false));
        when(type.firesFromBarrelLine(0)).thenReturn(true);
        assertEquals(expected, gun.getBarrelOrigin(0, false));
    }

    @Test
    void manualGuidanceReadsLiveOwnerPositionAndAimWhileFixedGuidanceKeepsItsLaunchRay()
    {
        Bullet bullet = mock(Bullet.class);
        BulletType type = mock(BulletType.class);
        LivingEntity owner = mock(LivingEntity.class);
        bullet.configType = type;
        bullet.firedShot = new FiredShot(null, type, owner, owner, 0);
        bullet.velocity = Vec3.ZERO;
        when(type.isManualGuidance()).thenReturn(true);
        when(type.getTrackPhaseSpeed()).thenReturn(2F);
        when(type.getTrackPhaseTurn()).thenReturn(1F);
        when(owner.position()).thenReturn(Vec3.ZERO);
        when(owner.getLookAngle()).thenReturn(new Vec3(1, 0, 0));
        when(bullet.position()).thenReturn(Vec3.ZERO);
        doCallRealMethod().when(bullet).updateManualGuidance();
        // Turn radius provides a nonzero aim distance at the mocked origin.
        when(type.getTurnRadius()).thenReturn(10F);
        bullet.updateManualGuidance();
        assertEquals(new Vec3(2, 0, 0), bullet.velocity);
        when(owner.position()).thenReturn(new Vec3(0, 10, 0));
        when(owner.getLookAngle()).thenReturn(new Vec3(0, 1, 0));
        bullet.updateManualGuidance();
        assertEquals(new Vec3(0, 2, 0), bullet.velocity);
        when(type.isFixedDirection()).thenReturn(true);
        bullet.initialPos = Vec3.ZERO;
        bullet.lookVector = new Vec3(0, 0, 1);
        bullet.updateManualGuidance();
        assertEquals(new Vec3(0, 0, 2), bullet.velocity);
        when(type.isManualGuidance()).thenReturn(false);
        bullet.updateManualGuidance();
        assertEquals(new Vec3(0, 0, 2), bullet.velocity);
    }
}
