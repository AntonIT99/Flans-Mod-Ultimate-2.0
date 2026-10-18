package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityLifecycleTest
{
    private enum Kind
    {
        BULLET(Bullet::new), GRENADE(Grenade::new), SEAT(Seat::new), WHEEL(Wheel::new), VEHICLE(Vehicle::new), PLANE(Plane::new), MECHA(Mecha::new), AA_GUN(AAGun::new), DEPLOYED_GUN(
            DeployedGun::new), PARACHUTE(Parachute::new), FLAG(Flag::new), FLAGPOLE(Flagpole::new);

        private final BiFunction<EntityType<?>, Level, Entity> factory;

        Kind(BiFunction<EntityType<?>, Level, Entity> factory)
        {
            this.factory = factory;
        }
    }

    @BeforeAll
    static void bootstrapRegistries() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void ownerlessBulletKeepsFlyingInsteadOfSilentlyDiscardingOnAnException()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Bullet bullet = bullet(world);
            Vec3 origin = new Vec3(0.5D, 64.5D, 0.5D);
            world.tick(bullet);

            assertFalse(bullet.isRemoved(), "A caught tick exception must fail this test too");
            assertTrue(bullet.getZ() > origin.z);
        }
    }

    @Test
    void bulletDeliversPlayerDamageBeforeBeingRemoved()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Player target = world.target(Player.class, new Vec3(0.5D, 64D, 2D));
            Bullet bullet = bullet(world);

            world.tick(bullet);

            verify(target).hurt(argThat(source -> source.getDirectEntity() == bullet), floatThat(damage -> damage > 0F && Float.isFinite(damage)));
            assertTrue(bullet.isRemoved(), "The round spends its penetration budget on the player");
        }
    }

    @Test
    void bulletDeliversMobDamageBeforeBeingRemoved()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            LivingEntity target = world.target(LivingEntity.class, new Vec3(0.5D, 64D, 2D));
            Bullet bullet = bullet(world);

            world.tick(bullet);

            verify(target).hurt(argThat(source -> source.getDirectEntity() == bullet), floatThat(damage -> damage > 0F && Float.isFinite(damage)));
            assertTrue(bullet.isRemoved());
        }
    }

    @Test
    void solidWallStopsBulletBeforeItDamagesAPlayer()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Player target = world.target(Player.class, new Vec3(0.5D, 64D, 3D));
            world.blocks.put(new BlockPos(0, 64, 1), Blocks.STONE.defaultBlockState());
            Bullet bullet = bullet(world);

            world.tick(bullet);

            verify(target, never()).hurt(any(), anyFloat());
            assertTrue(bullet.isRemoved());
            assertTrue(bullet.isDetonated(), "A caught exception discards the round without completing its impact lifecycle");
        }
    }

    @Test
    void ownerlessGrenadeKeepsFlying()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Grenade grenade = new Grenade(EntityType.SNOWBALL, world.level);
            grenade.configType = new GrenadeType();
            grenade.setPos(0.5D, 64.5D, 0.5D);
            grenade.setDeltaMovement(0D, 0D, 1D);

            world.tick(grenade);

            assertFalse(grenade.isRemoved(), "A caught grenade tick exception must fail this test too");
            assertTrue(grenade.getZ() > 0.5D);
        }
    }

    @Test
    void grenadeStopsAtASolidWallWithoutAnOwner()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Grenade grenade = new Grenade(EntityType.SNOWBALL, world.level);
            grenade.configType = new GrenadeType();
            grenade.setPos(0.5D, 64.5D, 0.5D);
            grenade.setDeltaMovement(0D, 0D, 1D);
            world.blocks.put(new BlockPos(0, 64, 1), Blocks.STONE.defaultBlockState());

            world.tick(grenade);

            assertFalse(grenade.isRemoved());
            assertEquals(0D, grenade.getDeltaMovement().z, 1.0E-6D);
            assertEquals(1D, grenade.getZ(), 1.0E-6D);
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void spawnConstructorsInitializeTheirEntityData(Kind kind)
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            // The deserialization constructors accept a type; no mod registry or renderer is needed here.
            Entity entity = assertDoesNotThrow(() -> kind.factory.apply(EntityType.SNOWBALL, world.level));
            entity.setPos(12D, 64D, -7D);
            assertEquals(new Vec3(12D, 64D, -7D), entity.position());
            assertNotNull(entity.getEntityData());
            assertFalse(entity.isRemoved());
        }
    }

    @ParameterizedTest
    @EnumSource(value = Kind.class, names = {"SEAT", "WHEEL"})
    void proxiesWaitForAMissingParentAndThenExpire(Kind kind)
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Entity proxy = kind.factory.apply(EntityType.SNOWBALL, world.level);
            for (int tick = 0; tick < 100; tick++)
                world.tick(proxy);
            assertFalse(proxy.isRemoved(), "Spawn order can leave the parent unavailable briefly");

            world.tick(proxy);

            assertTrue(proxy.isRemoved(), "An orphan must not remain forever");
        }
    }

    @ParameterizedTest
    @EnumSource(value = Kind.class, names = {"VEHICLE", "PLANE", "MECHA", "AA_GUN", "DEPLOYED_GUN", "PARACHUTE"})
    void entitiesWithMissingDefinitionsAreRemovedWithoutThrowing(Kind kind)
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Entity entity = kind.factory.apply(EntityType.SNOWBALL, world.level);

            world.tick(entity);

            assertTrue(entity.isRemoved());
        }
    }

    @Test
    void stationaryBulletHasFiniteMotionAndOrientation()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Bullet bullet = bullet(world);
            bullet.setDeltaMovement(Vec3.ZERO);

            world.tick(bullet);

            assertFalse(bullet.isRemoved());
            assertTrue(Double.isFinite(bullet.getDeltaMovement().lengthSqr()));
            assertTrue(Float.isFinite(bullet.getYRot()) && Float.isFinite(bullet.getXRot()));
        }
    }

    @Test
    void missileClearsADeadTargetAndKeepsFlying()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Bullet bullet = bullet(world);
            bullet.lockedOnTo = mock(LivingEntity.class);

            world.tick(bullet);

            assertFalse(bullet.isRemoved());
            assertNull(bullet.lockedOnTo);
            assertTrue(bullet.getZ() > 0.5D);
        }
    }

    @Test
    void projectileOutsideTickingChunksIsRemovedBeforeTracing()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Bullet bullet = bullet(world);
            when(world.level.isPositionEntityTicking(any())).thenReturn(false);

            world.tick(bullet);

            assertTrue(bullet.isRemoved());
            verify(world.level, never()).clip(any());
        }
    }

    @Test
    void bulletWhoseSavedOwnerIsGoneKeepsFlying()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Bullet bullet = bullet(world);
            bullet.checkForUUIDs = true;
            bullet.attackerUUID = java.util.UUID.randomUUID();
            bullet.shooterUUID = java.util.UUID.randomUUID();

            world.tick(bullet);

            assertFalse(bullet.isRemoved());
            assertTrue(bullet.getFiredShot().getAttacker().isEmpty());
            assertTrue(bullet.getFiredShot().getCausingEntity().isEmpty());
            assertTrue(bullet.getZ() > 0.5D);
        }
    }

    @Test
    void grenadeWhoseSavedThrowerIsGoneKeepsFlying()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Grenade grenade = new Grenade(EntityType.SNOWBALL, world.level);
            grenade.configType = new GrenadeType();
            grenade.checkForUUIDs = true;
            grenade.throwerUUID = java.util.UUID.randomUUID();
            grenade.setPos(0.5D, 64.5D, 0.5D);
            grenade.setDeltaMovement(0D, 0D, 1D);

            world.tick(grenade);

            assertFalse(grenade.isRemoved());
            assertTrue(grenade.getOwner().isEmpty());
            assertTrue(grenade.getZ() > 0.5D);
        }
    }

    private static Bullet bullet(EntityTestWorld world)
    {
        Bullet bullet = new Bullet(EntityType.SNOWBALL, world.level);
        bullet.configType = new BulletType();
        bullet.firedShot = new FiredShot(null, bullet.configType, null, null, 0);
        bullet.penetratingPower = 1F;
        bullet.setPos(0.5D, 64.5D, 0.5D);
        bullet.setDeltaMovement(0D, 0D, 3D);
        return bullet;
    }
}
