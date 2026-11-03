package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmplacementAimTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("java:S3011") // Exercise the native AA traverse step without unrelated shooting and world physics.
    void aaTraverseKeepsAutomatedAimAfterTheOperatorLookIsReset() throws ReflectiveOperationException
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            AAGun gun = spy(new AAGun(EntityType.SNOWBALL, world.level));
            gun.configType = mock(AAGunType.class);
            when(gun.configType.getTopViewLimit()).thenReturn(75F);
            when(gun.configType.getTraverseSpeed()).thenReturn(20F);
            LivingEntity operator = world.target(LivingEntity.class, Vec3.ZERO);
            doReturn(operator).when(gun).getFirstPassenger();
            doReturn(Vec3.ZERO).when(gun).getEmplacementOrigin();
            gun.aimEmplacement(operator, new Vec3(-10D, 10D, 10D));
            // The look controller has overwritten both angles before the mount ticks.
            when(operator.getYRot()).thenReturn(0F);
            when(operator.getXRot()).thenReturn(0F);
            var traverse = AAGun.class.getDeclaredMethod("updateAimFromPassenger", LivingEntity.class);
            traverse.setAccessible(true);
            traverse.invoke(gun, operator);
            assertEquals(1F, gun.getGunYaw(), 0.001F);
            assertEquals(-1F, gun.getGunPitch(), 0.001F);
            when(world.level.getGameTime()).thenReturn(1L);
            traverse.invoke(gun, operator);
            assertEquals(2F, gun.getGunYaw(), 0.001F);
            assertEquals(-2F, gun.getGunPitch(), 0.001F);
            when(world.level.getGameTime()).thenReturn(2L);
            traverse.invoke(gun, operator);
            assertEquals(1F, gun.getGunYaw(), 0.001F);
            assertEquals(-1F, gun.getGunPitch(), 0.001F);
        }
    }

    @Test
    void deployedGunKeepsAutomatedAimAndMechanicalLimitsWhenPositioningItsOperator()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            for (int x = -3; x <= 3; x++)
                for (int z = -3; z <= 3; z++)
                    world.blocks.put(new BlockPos(x, -1, z), Blocks.STONE.defaultBlockState());
            when(world.level.noCollision(any(), any())).thenReturn(true);
            DeployedGun gun = spy(new DeployedGun(EntityType.SNOWBALL, world.level));
            gun.configType = new GunType();
            gun.blockPos = BlockPos.ZERO;
            LivingEntity operator = world.target(LivingEntity.class, Vec3.ZERO);
            doReturn(operator).when(gun).getFirstPassenger();
            doReturn(Vec3.ZERO).when(gun).getShootingOrigin();
            gun.aimEmplacement(operator, new Vec3(-10D, 100D, 10D));
            when(operator.getYRot()).thenReturn(0F);
            when(operator.getXRot()).thenReturn(0F);
            gun.positionRider(operator, (entity, x, y, z) ->
            {
            });
            assertEquals(40F, gun.getYRot(), 0.001F);
            assertEquals(-60F, gun.getXRot(), 0.001F);
            when(world.level.getGameTime()).thenReturn(2L);
            gun.positionRider(operator, (entity, x, y, z) ->
            {
            });
            assertEquals(0F, gun.getYRot(), 0.001F);
            assertEquals(0F, gun.getXRot(), 0.001F);
        }
    }
}
