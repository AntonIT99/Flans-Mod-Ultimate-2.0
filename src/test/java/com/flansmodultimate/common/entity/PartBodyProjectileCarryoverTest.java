package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.parts.IFlanPartTarget;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.guns.ArmourImpactEffects;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.hits.PartBodyHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartBodyProjectileCarryoverTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void shellAndHeatUseTheSameSurplusBudgetAsRealDriveables()
    {
        for (boolean heat : new boolean[]{false, true})
        {
            try (Fixture fixture = new Fixture())
            {
                when(fixture.bullet.isHeat()).thenReturn(heat);
                fixture.fire(fixture.trace().get(0), new ShootingHelper.HitData(1F, 0F, false));
                assertTrue(fixture.body.isShotOff(EnumDriveablePart.FRONTAL_ARMOR));
                assertEquals(130F, fixture.body.partState(EnumDriveablePart.CORE).getHealth());
            }
        }
    }

    @Test
    void heatCanDestroyBothTheOuterPlateAndCore()
    {
        try (Fixture fixture = new Fixture())
        {
            when(fixture.bullet.isHeat()).thenReturn(true);
            fixture.damage.setDamageVsVehicles(250F);
            fixture.fire(fixture.trace().get(0), new ShootingHelper.HitData(1F, 0F, false));
            assertTrue(fixture.body.isShotOff(EnumDriveablePart.FRONTAL_ARMOR));
            assertTrue(fixture.body.isShotOff(EnumDriveablePart.CORE));
        }
    }

    @Test
    void theNpcDamageCallbackCanRejectTheHitWithoutStartingCarryover()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.rejectDamage = true;
            fixture.fire(fixture.trace().get(0), new ShootingHelper.HitData(1F, 0F, false));
            assertEquals(30F, fixture.body.partState(EnumDriveablePart.FRONTAL_ARMOR).getHealth());
            assertEquals(200F, fixture.body.partState(EnumDriveablePart.CORE).getHealth());
        }
    }

    @Test
    void carryoverPreservesTheWearersDamageModifiers()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.damageMultiplier = 0.5F;
            fixture.fire(fixture.trace().get(0), new ShootingHelper.HitData(1F, 0F, false));
            assertTrue(fixture.body.isShotOff(EnumDriveablePart.FRONTAL_ARMOR));
            // The NPC receives 50 effective damage: 30 spent on the plate and 20 on the core.
            assertEquals(180F, fixture.body.partState(EnumDriveablePart.CORE).getHealth());
            verify(fixture.target).flansmodultimateHurtPart(eq("core"), any(), eq(40F));
        }
    }

    @Test
    void heatStopsAtExtraArmorOnTheStruckFace()
    {
        try (Fixture fixture = new Fixture())
        {
            when(fixture.bullet.isHeat()).thenReturn(true);
            fixture.armor(Map.of(EnumDriveablePart.CORE, new ArmorPlate(1F, 0F)));
            fixture.fire(fixture.trace().get(0), new ShootingHelper.HitData(1F, 0F, false));
            assertTrue(fixture.body.isShotOff(EnumDriveablePart.FRONTAL_ARMOR));
            assertEquals(200F, fixture.body.partState(EnumDriveablePart.CORE).getHealth());
        }
    }

    @Test
    void laterMachineGunFireMeetsTheCoreFallbackPlate()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.fire(fixture.trace().get(0), new ShootingHelper.HitData(1F, 0F, false));
            when(fixture.shot.getPenetrationAt100m()).thenReturn(10F);
            fixture.fire(fixture.trace().get(0), new ShootingHelper.HitData(1F, 0F, false));
            assertEquals(130F, fixture.body.partState(EnumDriveablePart.CORE).getHealth());
        }
    }

    @Test
    void precomputedInteriorContactsDoNotDealDamageTwice()
    {
        try (Fixture fixture = new Fixture())
        {
            List<PartBodyHit> hits = fixture.trace();
            ShootingHelper.HitData data = fixture.fire(hits.get(0), new ShootingHelper.HitData(1F, 0F, false));
            fixture.fire(hits.get(1), data);
            assertEquals(130F, fixture.body.partState(EnumDriveablePart.CORE).getHealth());
        }
    }

    @Test
    void heatBlastExcludesTheLastJetDamagedPartAtTheOriginalDetonation()
    {
        try (Fixture fixture = new Fixture())
        {
            when(fixture.bullet.isHeat()).thenReturn(true);
            PartBodyHit first = fixture.trace().get(0);
            fixture.fire(first, new ShootingHelper.HitData(1F, 0F, false));
            assertEquals(EnumDriveablePart.CORE, fixture.body.consumeShapedCharge(first.getHitPosition(), 0L));
            assertNull(fixture.body.consumeShapedCharge(first.getHitPosition(), 0L));
        }
    }

    private static final class Fixture implements AutoCloseable
    {
        private final EntityTestWorld world = new EntityTestWorld();
        private final MockedStatic<ArmourImpactEffects> effects = mockStatic(ArmourImpactEffects.class);
        private final Entity entity = mock(Entity.class, withSettings().extraInterfaces(IFlanPartTarget.class));
        private final IFlanPartTarget target = (IFlanPartTarget) entity;
        private final DriveableType type = mock(DriveableType.class);
        private final BulletType bullet = mock(BulletType.class);
        private final FiredShot shot = mock(FiredShot.class);
        private final DamageStats damage = new DamageStats();
        private final Map<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        private final PartBody body;
        private float damageMultiplier = 1F;
        private boolean rejectDamage;

        private Fixture()
        {
            boxes.put(EnumDriveablePart.FRONTAL_ARMOR, CollisionBox.inWorldUnits(30F, -1F, 0F, -3F, 2F, 1F, 0.5F, 5F, 0F));
            boxes.put(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(200F, -1F, 0F, -1F, 2F, 1F, 2F, 5F, 0F));
            when(type.getResolvedHealth()).thenReturn(VehicleHealthScaler.resolve(false, null, Map.of(), 1D));
            armor(Map.of());
            body = new PartBody(type, boxes, Vec3.ZERO, Vec3.ZERO);
            when(entity.level()).thenReturn(world.level);
            when(entity.isAlive()).thenAnswer(call -> !body.isShotOff(EnumDriveablePart.CORE));
            when(target.flansmodultimateModelArmour()).thenReturn(true);
            when(target.flansmodultimateDamageModel()).thenReturn(Optional.of(type));
            when(target.flansmodultimatePartBody()).thenReturn(Optional.of(body));
            when(target.flansmodultimateHurtPart(anyString(), any(), anyFloat())).thenAnswer(call ->
            {
                if (rejectDamage)
                    return false;
                body.damage(call.getArgument(0), call.getArgument(2, Float.class) * damageMultiplier, false);
                return true;
            });
            damage.setDamageVsVehicles(100F);
            when(bullet.getDamage()).thenReturn(damage);
            when(shot.getBulletType()).thenReturn(bullet);
            when(shot.getPenetrationAt100m()).thenReturn(100F);
            when(shot.getExplosiveMass()).thenReturn(1F);
            DamageSource damageSource = mock(DamageSource.class);
            when(shot.getDamageSource(any(), nullable(Bullet.class))).thenReturn(damageSource);
        }

        private void armor(Map<EnumDriveablePart, ArmorPlate> overrides)
        {
            when(type.getResolvedArmor()).thenReturn(VehicleArmorResolver.resolve(
                new VehicleArmorSpec(Map.of(EnumArmorFacing.FRONT, new ArmorPlate(80F, 0F)), Map.of(), overrides), boxes.keySet()));
        }

        private List<PartBodyHit> trace()
        {
            return body.trace(entity, new Vec3(0D, 0.5D, 4D), new Vec3(0D, 0D, -8D)).stream().map(PartBodyHit.class::cast).toList();
        }

        private ShootingHelper.HitData fire(PartBodyHit hit, ShootingHelper.HitData data)
        {
            return ShootingHelper.onHit(world.level, shot, hit, hit.getHitPosition(), new Vec3(0D, 0D, -2D), data, null);
        }

        @Override
        public void close()
        {
            effects.close();
            world.close();
        }
    }
}
