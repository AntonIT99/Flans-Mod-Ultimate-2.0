package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AAGunWreckTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void lethalOwnerlessDamageLeavesAnInoperableWreckThatExpires()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            when(ModCommonConfig.get().driveableWreckLifetime()).thenReturn(2);
            AAGun gun = new AAGun(EntityType.SNOWBALL, world.level);
            gun.configType = new AAGunType();
            gun.setHealth(10);

            assertTrue(gun.hurt(mock(DamageSource.class), 10F));
            assertTrue(gun.isWreck());
            assertFalse(gun.isAlive());
            assertFalse(gun.isRemoved());
            assertEquals(InteractionResult.PASS, gun.interact(mock(Player.class), InteractionHand.MAIN_HAND));
            assertFalse(gun.hurt(mock(DamageSource.class), 10F));
            CompoundTag saved = new CompoundTag();
            gun.addAdditionalSaveData(saved);
            assertEquals(0, saved.getInt("wreck_age"));

            for (int i = 0; i < 40; i++)
                world.tick(gun);
            assertTrue(gun.isRemoved());
        }
    }

    @Test
    void disabledWrecksRemoveDestroyedGunsImmediately()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            AAGun gun = new AAGun(EntityType.SNOWBALL, world.level);
            gun.configType = new AAGunType();
            gun.setHealth(10);
            gun.hurt(mock(DamageSource.class), 10F);
            assertTrue(gun.isRemoved());
        }
    }
}
