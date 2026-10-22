package com.flansmodultimate.common.entity;

import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.flansmodultimate.apocalyse.common.entity.SkullDroneEntity;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused") // MockedStatic resources install attribute suppliers for their lifetime without direct calls.
class SkullFlightTest
{
    private static EntityType<SkullBossEntity> bossType;
    private static EntityType<SkullDroneEntity> droneType;

    @BeforeAll
    @SuppressWarnings("unchecked") // Reuse registered types without mutating Forge's frozen registry; the tested entities are real skull classes.
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        bossType = (EntityType<SkullBossEntity>) (EntityType<?>) EntityType.GHAST;
        droneType = (EntityType<SkullDroneEntity>) (EntityType<?>) EntityType.BLAZE;
    }

    @Test
    void bossFollowsTheLegacySpringAndBobbingTrajectoryForSixHundredTicks() throws ReflectiveOperationException
    {
        try (EntityTestWorld world = new EntityTestWorld(); var attributes = attributes(); var config = mockStatic(ModApocalypseConfig.class))
        {
            config.when(ModApocalypseConfig::apocalypseMobsEnabled).thenReturn(true);
            when(world.level.getDifficulty()).thenReturn(Difficulty.NORMAL);
            SkullBossEntity boss = new SkullBossEntity(bossType, world.level);
            Vec3 home = new Vec3(0D, 220D, 0D);
            boss.setHome(home);
            boss.setPos(17D, 200D, -11D);
            boss.setDeltaMovement(0.8D, 0.2D, -0.35D);
            suppressAttacks(boss);
            Flight reference = new Flight(boss.position(), boss.getDeltaMovement());

            for (int tick = 1; tick <= 600; tick++)
            {
                reference = legacyBossTick(reference, home, tick);
                world.tick(boss);
                assertVector(reference.position(), boss.position());
                assertVector(reference.velocity(), boss.getDeltaMovement());
            }
            assertTrue(boss.isNoAi());
            assertEquals(0D, boss.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        }
    }

    @Test
    void droneApproachesItsHoverPointAndTurnsFromTheLegacyPreMovePosition() throws ReflectiveOperationException
    {
        try (EntityTestWorld world = new EntityTestWorld(); var attributes = attributes(); var config = mockStatic(ModApocalypseConfig.class))
        {
            config.when(ModApocalypseConfig::apocalypseMobsEnabled).thenReturn(true);
            when(world.level.getDifficulty()).thenReturn(Difficulty.NORMAL);
            SkullDroneEntity drone = new SkullDroneEntity(droneType, world.level);
            RandomSource random = mock(RandomSource.class);
            when(random.nextInt(anyInt())).thenReturn(1);
            replaceRandom(drone, random);
            Vec3 feet = new Vec3(40D, 70D, -20D);
            LivingEntity target = world.target(LivingEntity.class, feet);
            when(target.position()).thenReturn(feet);
            drone.setTarget(target);
            drone.setPos(0D, 64D, 0D);
            drone.setYRot(0F);
            drone.setXRot(0F);
            drone.setDeltaMovement(0.4D, 0.2D, -0.3D);
            Flight reference = new Flight(drone.position(), drone.getDeltaMovement());
            float yaw = 0F;
            float pitch = 0F;

            for (int tick = 1; tick <= 180; tick++)
            {
                Flight inherited = legacyInheritedMovement(reference);
                Vec3 look = feet.subtract(drone.position());
                float desiredYaw = (float) (Math.atan2(look.z, look.x) * 180D / Math.PI) - 90F;
                float desiredPitch = (float) (-Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * 180D / Math.PI);
                yaw += Mth.wrapDegrees(desiredYaw - yaw) / 2F;
                pitch += (desiredPitch - pitch) / 2F;
                Vec3 velocity = feet.add(0D, 10D, 0D).subtract(inherited.position()).scale(0.06F);
                reference = new Flight(inherited.position().add(velocity), velocity);

                world.tick(drone);

                // Modern Entity.move ignores steps shorter than sqrt(1e-7), settling within 0.006 blocks of the legacy hover point.
                assertVector(reference.position(), drone.position(), 0.006D);
                assertVector(reference.velocity(), drone.getDeltaMovement(), 0.0004D);
                assertEquals(yaw, drone.getYRot(), 1.0E-4F);
                assertEquals(pitch, drone.getXRot(), 1.0E-4F);
            }
            assertTrue(drone.isNoAi());

            drone.setTarget(null);
            reference = new Flight(drone.position(), drone.getDeltaMovement());
            for (int tick = 0; tick < 60; tick++)
            {
                reference = legacyInheritedMovement(reference);
                world.tick(drone);
                assertVector(reference.position(), drone.position());
                assertVector(reference.velocity(), drone.getDeltaMovement());
            }
        }
    }

    @Test
    void olderSavesRestoreNoAiAndMigrateThePortsKnockbackImmunity()
    {
        try (EntityTestWorld world = new EntityTestWorld(); var attributes = attributes())
        {
            SkullBossEntity boss = new SkullBossEntity(bossType, world.level);
            var knockback = boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            assertNotNull(knockback);
            knockback.setBaseValue(1D);
            CompoundTag oldBoss = new CompoundTag();
            boss.addAdditionalSaveData(oldBoss);
            oldBoss.putBoolean("NoAI", false);
            oldBoss.remove("apocalypse_flight_version");
            boss.readAdditionalSaveData(oldBoss);
            assertTrue(boss.isNoAi());
            assertEquals(0D, boss.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));

            // Once migrated, an explicit custom attribute in a new save remains customizable.
            knockback.setBaseValue(0.5D);
            CompoundTag currentBoss = new CompoundTag();
            boss.addAdditionalSaveData(currentBoss);
            boss.readAdditionalSaveData(currentBoss);
            assertEquals(0.5D, boss.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));

            SkullDroneEntity drone = new SkullDroneEntity(droneType, world.level);
            CompoundTag oldDrone = new CompoundTag();
            drone.addAdditionalSaveData(oldDrone);
            oldDrone.putBoolean("NoAI", false);
            drone.readAdditionalSaveData(oldDrone);
            assertTrue(drone.isNoAi());
        }
    }

    private static MockedStatic<DefaultAttributes> attributes()
    {
        var attributes = mockStatic(DefaultAttributes.class, CALLS_REAL_METHODS);
        attributes.when(() -> DefaultAttributes.getSupplier(bossType)).thenReturn(SkullBossEntity.createAttributes().build());
        attributes.when(() -> DefaultAttributes.getSupplier(droneType)).thenReturn(SkullDroneEntity.createAttributes().build());
        return attributes;
    }

    private static Flight legacyBossTick(Flight previous, Vec3 home, int tick)
    {
        Flight inherited = legacyInheritedMovement(previous);
        Vec3 position = inherited.position();
        float height = (float) (home.y - 40D) + (float) Math.sin(tick / 200F) * 40F;
        Vec3 velocity = new Vec3(inherited.velocity().x - (position.x - home.x) * 0.1F / 20F, (height - position.y) * 0.1F / 20F, inherited.velocity().z - (position.z - home.z) * 0.1F / 20F);
        return new Flight(position.add(velocity), velocity);
    }

    /** EntityLivingBase.onLivingUpdate with NoAI=true; EntityLiving.canBeSteered=false also prevents travel in 1.12.2. */
    private static Flight legacyInheritedMovement(Flight previous)
    {
        Vec3 damped = previous.velocity().scale(0.98D);
        damped = new Vec3(legacyDeadzone(damped.x), legacyDeadzone(damped.y), legacyDeadzone(damped.z));
        return new Flight(previous.position(), damped);
    }

    private static double legacyDeadzone(double value)
    {
        return Math.abs(value) < 0.003D ? 0D : value;
    }

    private static void assertVector(Vec3 expected, Vec3 actual)
    {
        assertVector(expected, actual, 1.0E-7D);
    }

    private static void assertVector(Vec3 expected, Vec3 actual, double tolerance)
    {
        assertEquals(expected.x, actual.x, tolerance);
        assertEquals(expected.y, actual.y, tolerance);
        assertEquals(expected.z, actual.z, tolerance);
    }

    @SuppressWarnings("java:S3011") // Isolate hundreds of real movement ticks from the boss's unrelated attack phases.
    private static void suppressAttacks(SkullBossEntity boss) throws ReflectiveOperationException
    {
        var ticks = SkullBossEntity.class.getDeclaredField("actionTicks");
        ticks.setAccessible(true);
        ticks.setInt(boss, -1000);
    }

    @SuppressWarnings("java:S3011") // A deterministic entity RNG prevents random retargeting/repositioning in the trajectory test.
    private static void replaceRandom(Entity entity, RandomSource random) throws ReflectiveOperationException
    {
        var field = Entity.class.getDeclaredField("random");
        field.setAccessible(true);
        field.set(entity, random);
    }

    private record Flight(Vec3 position, Vec3 velocity)
    {}
}
