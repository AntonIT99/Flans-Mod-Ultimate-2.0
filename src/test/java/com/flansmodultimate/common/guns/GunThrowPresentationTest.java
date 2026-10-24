package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.equipment.IEquipmentPolicy;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.EnumEntityAimPose;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GunThrowPresentationTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void virtualThrowUsesThePlayerPoseWithoutChangingTheRealActiveUseItem()
    {
        LivingEntity holder = mock(LivingEntity.class, withSettings().extraInterfaces(IEquipmentPolicy.class));
        IEquipmentPolicy policy = (IEquipmentPolicy) holder;
        when(policy.flansWeaponAnimations()).thenReturn(true);
        when(policy.flansmodultimateChargingThrow(InteractionHand.MAIN_HAND)).thenReturn(true);
        when(holder.getMainHandItem()).thenReturn(new ItemStack(Items.SPLASH_POTION));
        when(holder.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        ServerLevel level = mock(ServerLevel.class);
        when(holder.level()).thenReturn(level);
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::entityAimPose).thenReturn(EnumEntityAimPose.DYNAMIC);
            assertEquals(InteractionHand.MAIN_HAND, GunArmPoses.chargingThrowHand(holder));
            assertEquals(new GunArmPoses.Result(GunArmPoses.Arm.THROW, GunArmPoses.Arm.NONE), GunArmPoses.resolve(holder));
            assertFalse(holder.isUsingItem());
            when(policy.flansmodultimateChargingThrow(InteractionHand.MAIN_HAND)).thenReturn(false);
            assertEquals(GunArmPoses.Result.NONE, GunArmPoses.resolve(holder));
            verify(holder, never()).startUsingItem(any());
        }
    }

    @Test
    void theThrowingItemModelPropertyRecognizesTheVirtualHand()
    {
        GunType type = mock(GunType.class);
        when(type.isThrowable()).thenReturn(true);
        GunItem gun = mock(GunItem.class, CALLS_REAL_METHODS);
        when(gun.getConfigType()).thenReturn(type);
        LivingEntity holder = mock(LivingEntity.class, withSettings().extraInterfaces(IEquipmentPolicy.class));
        IEquipmentPolicy policy = (IEquipmentPolicy) holder;
        ItemStack displayed = mock(ItemStack.class);
        when(displayed.getItem()).thenReturn(gun);
        when(holder.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(displayed);
        when(policy.flansmodultimateChargingThrow(InteractionHand.MAIN_HAND)).thenReturn(true);
        assertTrue(gun.isChargingThrow(holder, displayed));
        assertFalse(holder.isUsingItem());
        when(policy.flansmodultimateChargingThrow(InteractionHand.MAIN_HAND)).thenReturn(false);
        assertFalse(gun.isChargingThrow(holder, displayed));
    }
}
