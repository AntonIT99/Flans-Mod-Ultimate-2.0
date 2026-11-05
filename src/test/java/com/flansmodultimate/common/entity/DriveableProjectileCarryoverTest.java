package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.guns.ArmourImpactEffects;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.hits.DriveableHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriveableProjectileCarryoverTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void shellCarriesOnlyUnspentDamageToCoreEvenWhenThisTicksMotionEndsAtTheFrontPlate()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.fire(fixture.trace(2D).get(0));
            assertTrue(fixture.front.isDestroyed());
            assertEquals(130F, fixture.core.getHealth());
            assertEquals(100F, fixture.parts.get(EnumDriveablePart.ENGINE_ROOM_1).getHealth());
            verify(fixture.vehicle).onPartDestroyed(EnumDriveablePart.FRONTAL_ARMOR);
            ArgumentCaptor<ResolvedArmorHit> armor = ArgumentCaptor.forClass(ResolvedArmorHit.class);
            fixture.impactEffects.verify(() -> ArmourImpactEffects.send(any(), any(), any(), any(), anyFloat(), armor.capture(), anyBoolean()), times(2));
            assertTrue(armor.getAllValues().get(0).isArmoured());
            assertFalse(armor.getAllValues().get(1).isArmoured());
        }
    }

    @Test
    void carriesDamageAcrossMultipleDestroyedPartsInOrder()
    {
        try (Fixture fixture = new Fixture())
        {
            DriveablePart inner = fixture.addPart(EnumDriveablePart.WEAK_SPOT, 20F, -1F, -2F, 0.25F);
            fixture.fire(fixture.trace(2D).get(0));
            assertTrue(inner.isDestroyed());
            assertEquals(150F, fixture.core.getHealth());
        }
    }

    @Test
    void woundedPlateConsumesOnlyItsRemainingHealth()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.front.damage(25F, false);
            fixture.fire(fixture.trace(2D).get(0));
            assertEquals(105F, fixture.core.getHealth());
        }
    }

    @Test
    void normalizedKineticDamageIsCarriedWithoutRecalculatingItForTheOpenHull()
    {
        try (Fixture fixture = new Fixture())
        {
            when(ModCommonConfig.get().newDamageSystemDamageReference()).thenReturn(5F);
            when(fixture.type.getResolvedHealth()).thenReturn(new VehicleHealthScaler.Result(true, true, 330F, Map.of(), Map.of(), List.of()));
            when(fixture.shot.getProjectileMass()).thenReturn(1000F);
            when(fixture.shot.getMuzzleVelocity()).thenReturn(20F);
            float damage = ShootingHelper.getKineticDamage(1000F, 20D) * 1.25F;
            fixture.fire(fixture.trace(2D).get(0));
            assertEquals(230F - damage, fixture.core.getHealth(), 0.01F);
        }
    }

    @Test
    void noCarryoverWithoutSurplusOrWithoutDestroyingTheFirstPart()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.damage.setDamageVsVehicles(30F);
            ShootingHelper.HitData result = fixture.vehicle.bulletHit(fixture.shot, fixture.bullet, fixture.trace(2D).get(0), new ShootingHelper.HitData(100F, 0F, false));
            assertTrue(fixture.front.isDestroyed());
            assertEquals(200F, fixture.core.getHealth());
            assertEquals(0F, result.penetratingPower());
        }
        try (Fixture fixture = new Fixture())
        {
            fixture.damage.setDamageVsVehicles(20F);
            fixture.fire(fixture.trace(2D).get(0));
            assertEquals(10F, fixture.front.getHealth());
            assertEquals(200F, fixture.core.getHealth());
        }
    }

    @Test
    void independentlyArmouredInteriorStillStopsTheShell()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.armor(Map.of(EnumDriveablePart.CORE, new ArmorPlate(200F, 0F)));
            ShootingHelper.HitData result = fixture.fire(fixture.trace(2D).get(0));
            assertTrue(fixture.front.isDestroyed());
            assertEquals(200F, fixture.core.getHealth());
            assertEquals(0F, result.penetratingPower());
        }
    }

    @Test
    void alreadyTracedContactsDoNotDamageTheCoreTwice()
    {
        try (Fixture fixture = new Fixture())
        {
            List<DriveableHit> hits = fixture.trace(8D);
            ShootingHelper.HitData result = fixture.fire(hits.get(0));
            fixture.vehicle.bulletHit(fixture.shot, fixture.bullet, hits.get(1), result);
            assertEquals(130F, fixture.core.getHealth());
        }
    }

    @Test
    void impactFuseStillAllowsShellDamageToCarryIntoTheCore()
    {
        try (Fixture fixture = new Fixture())
        {
            when(fixture.bullet.isExplodeOnImpact()).thenReturn(true);
            DriveableHit hit = fixture.trace(2D).get(0);
            ShootingHelper.HitData input = new ShootingHelper.HitData(1F, 0F, false);
            ShootingHelper.HitData result = ShootingHelper.onHit(fixture.world.level, fixture.shot, hit, hit.getHitPosition(), fixture.motion(2D), input, null);
            assertTrue(result.penetratingPower() <= 0F);
            assertEquals(130F, fixture.core.getHealth());
            assertSame(input.resolvedDriveableParts(), result.resolvedDriveableParts());
        }
    }

    @Test
    void heatSpendsItsJetOnTheFirstActivePartEvenWhenItDestroysThatPart()
    {
        try (Fixture fixture = new Fixture())
        {
            when(fixture.bullet.isHeat()).thenReturn(true);
            ShootingHelper.HitData result = fixture.fire(fixture.trace(2D).get(0));
            assertTrue(fixture.front.isDestroyed());
            assertEquals(200F, fixture.core.getHealth());
            assertEquals(0F, result.penetratingPower());
        }
    }

    @Test
    void heatPassesThroughAnAlreadyDestroyedPlateToHitTheCore()
    {
        try (Fixture fixture = new Fixture())
        {
            when(fixture.bullet.isHeat()).thenReturn(true);
            fixture.front.damage(30F, false);
            List<DriveableHit> hits = fixture.trace(8D);
            assertEquals(EnumDriveablePart.CORE, hits.get(0).getPart());
            ShootingHelper.HitData result = fixture.fire(hits.get(0));
            assertEquals(100F, fixture.core.getHealth());
            assertEquals(0F, result.penetratingPower());
        }
    }

    @Test
    void subsequentMachineGunFireStillHasToDefeatTheCoreArmor()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.fire(fixture.trace(2D).get(0));
            when(fixture.shot.getPenetrationAt100m()).thenReturn(10F);
            ShootingHelper.HitData result = fixture.fire(fixture.trace(8D).get(0));
            assertEquals(130F, fixture.core.getHealth());
            assertEquals(0F, result.penetratingPower());
        }
    }

    @Test
    void heatMeetsTheCoreHeatProtectionAfterPassingThroughADestroyedPart()
    {
        try (Fixture fixture = new Fixture())
        {
            when(fixture.bullet.isHeat()).thenReturn(true);
            when(fixture.type.getResolvedArmor()).thenReturn(VehicleArmorResolver.resolve(new VehicleArmorSpec(
                Map.of(EnumArmorFacing.FRONT, new ArmorPlate(80F, 0F, 200F)), Map.of(), Map.of()), fixture.parts.keySet()));
            fixture.front.damage(30F, false);
            ShootingHelper.HitData result = fixture.fire(fixture.trace(8D).get(0));
            assertEquals(200F, fixture.core.getHealth());
            assertEquals(0F, result.penetratingPower());
        }
    }

    @Test
    void heshStillActsAtTheFirstSurface()
    {
        try (Fixture fixture = new Fixture())
        {
            when(fixture.bullet.isHesh()).thenReturn(true);
            ShootingHelper.HitData result = fixture.fire(fixture.trace(2D).get(0));
            assertTrue(fixture.front.isDestroyed());
            assertEquals(200F, fixture.core.getHealth());
            assertEquals(0F, result.penetratingPower());
        }
    }

    private static final class Fixture implements AutoCloseable
    {
        private final EntityTestWorld world = new EntityTestWorld();
        private final MockedStatic<ArmourImpactEffects> impactEffects = mockStatic(ArmourImpactEffects.class);
        private final Vehicle vehicle = spy(new Vehicle(EntityType.ARMOR_STAND, world.level));
        private final DriveableType type = mock(DriveableType.class);
        private final Map<EnumDriveablePart, DriveablePart> parts = new EnumMap<>(EnumDriveablePart.class);
        private final DriveablePart front = addPart(EnumDriveablePart.FRONTAL_ARMOR, 30F, -1F, -3F, 0.5F);
        private final DriveablePart core = addPart(EnumDriveablePart.CORE, 200F, -1F, -1F, 2F);
        private final BulletType bullet = mock(BulletType.class);
        private final DamageStats damage = new DamageStats();
        private final FiredShot shot = mock(FiredShot.class);

        private Fixture()
        {
            addPart(EnumDriveablePart.ENGINE_ROOM_1, 100F, 4F, -1F, 2F);
            vehicle.configType = type;
            vehicle.driveableData = mock(DriveableData.class);
            when(vehicle.driveableData.getParts()).thenReturn(parts);
            when(vehicle.driveableData.getPart(any())).thenAnswer(call -> parts.get(call.getArgument(0)));
            when(type.getModelScale()).thenReturn(1F);
            when(type.getBounds()).thenReturn(Optional.empty());
            when(type.getResolvedHealth()).thenReturn(VehicleHealthScaler.resolve(false, null, Map.of(), 1D));
            // Keep destruction effects outside this focused projectile test; health and hitbox tracing are real.
            doNothing().when(vehicle).onPartDestroyed(any());
            damage.setDamageVsVehicles(100F);
            when(bullet.getDamage()).thenReturn(damage);
            when(shot.getBulletType()).thenReturn(bullet);
            when(shot.getPenetrationAt100m()).thenReturn(100F);
            when(shot.getExplosiveMass()).thenReturn(1F);
            armor(Map.of());
        }

        private DriveablePart addPart(EnumDriveablePart part, float health, float x, float z, float depth)
        {
            DriveablePart state = new DriveablePart(part, CollisionBox.inWorldUnits(health, x, 0F, z, 2F, 1F, depth, 5F, 0F));
            parts.put(part, state);
            return state;
        }

        private void armor(Map<EnumDriveablePart, ArmorPlate> overrides)
        {
            when(type.getResolvedArmor()).thenReturn(VehicleArmorResolver.resolve(new VehicleArmorSpec(Map.of(EnumArmorFacing.FRONT, new ArmorPlate(80F, 0F)), Map.of(), overrides),
                parts.keySet()));
        }

        private Vec3 motion(double length)
        {
            return vehicle.modelLocalDirectionToWorld(vehicle.partFrameToModelLocal(new Vec3(0D, 0D, length)));
        }

        private List<DriveableHit> trace(double length)
        {
            return vehicle.attackFromBullet(vehicle.partFrameToWorld(new Vec3(0D, 0.5D, -4D)), motion(length)).stream().map(DriveableHit.class::cast).toList();
        }

        private ShootingHelper.HitData fire(DriveableHit hit)
        {
            return vehicle.bulletHit(shot, bullet, hit, new ShootingHelper.HitData(1F, 0F, false));
        }

        @Override
        public void close()
        {
            impactEffects.close();
            world.close();
        }
    }
}
