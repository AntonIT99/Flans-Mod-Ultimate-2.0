package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.parts.FlansRepairs;
import com.flansmodultimate.common.driveables.RepairToolFeedback;
import com.flansmodultimate.common.item.ToolItem;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AAGunRepairTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void repairToolTargetsHullWithoutMountingAndUsesSharedCostsAndHealthCap()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            AAGun gun = gun(world);
            gun.setHealth(500);
            ToolType definition = mock(ToolType.class);
            when(definition.isHealDriveables()).thenReturn(true);
            when(definition.getHealAmount()).thenReturn(100);
            when(definition.getToolLife()).thenReturn(200);
            ToolItem tool = mock(ToolItem.class);
            when(tool.getConfigType()).thenReturn(definition);
            ItemStack stack = mock(ItemStack.class);
            when(stack.getItem()).thenReturn(tool);
            Player player = mock(Player.class);
            when(player.level()).thenReturn(world.level);
            when(player.isAlive()).thenReturn(true);
            when(player.getEyePosition()).thenReturn(new Vec3(0D, 1D, 4D));
            when(player.getPersistentData()).thenReturn(new CompoundTag());
            when(player.getAbilities()).thenReturn(new Abilities());
            when(player.getCooldowns()).thenReturn(mock(ItemCooldowns.class));
            when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);
            when(world.level.getEntities(eq(player), any(), any())).thenReturn(List.of(gun));
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(0D, 0D, -5D);

            assertSame(gun, FlansRepairs.toolTarget(player, start, end).orElseThrow());
            assertTrue(new Vec3(0D, 1D, 1D).distanceTo(FlansRepairs.toolHitPoint(gun, start, end).orElseThrow()) < 1.0E-6D);
            assertEquals(InteractionResult.PASS, gun.interact(player, InteractionHand.MAIN_HAND));
            assertTrue(gun.getPassengers().isEmpty());
            assertTrue(FlansRepairs.useTool(player, gun, stack));
            assertEquals(550, gun.getHealth());
            assertFalse(FlansRepairs.useTool(player, gun, stack));
            verify(stack).setDamageValue(1);
            verify(player.getCooldowns()).addCooldown(tool, 20);

            when(world.level.getGameTime()).thenReturn(20L);
            when(stack.getDamageValue()).thenReturn(1);
            gun.setHealth(980);
            assertTrue(FlansRepairs.useTool(player, gun, stack));
            assertEquals(1000, gun.getHealth());
            when(world.level.getGameTime()).thenReturn(40L);
            assertFalse(FlansRepairs.useTool(player, gun, stack));
            verify(stack).setDamageValue(2);
            RepairToolFeedback.PartHealth health = RepairToolFeedback.parts(gun).get(0);
            assertEquals(1000F, health.health());
            assertEquals(1000F, health.maximum());
            assertEquals("Test AA gun", health.displayName());

            gun.setHealth(500);
            when(player.getEyePosition()).thenReturn(new Vec3(0D, 1D, 20D));
            assertFalse(FlansRepairs.useTool(player, gun, stack));
            assertEquals(500, gun.getHealth());
            verify(stack, times(2)).setDamageValue(anyInt());
        }
    }

    @Test
    void wrecksCannotBeRepairedOrConsumeToolDurability()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            when(ModCommonConfig.get().driveableWreckLifetime()).thenReturn(300);
            AAGun gun = gun(world);
            assertFalse(gun.repairFromTool(100));
            gun.setHealth(500);
            assertFalse(gun.repairFromTool(0));
            assertFalse(gun.repairFromTool(-100));
            gun.hurt(mock(DamageSource.class), 1000F);
            assertTrue(gun.isWreck());
            assertFalse(gun.repairFromTool(100));
            Player player = mock(Player.class);
            when(player.level()).thenReturn(world.level);
            when(player.isAlive()).thenReturn(true);
            ItemStack stack = mock(ItemStack.class);
            assertFalse(FlansRepairs.useTool(player, gun, stack));
            verify(stack, never()).setDamageValue(anyInt());
            assertEquals(0, gun.getHealth());
        }
    }

    private static AAGun gun(EntityTestWorld world)
    {
        AAGun gun = new AAGun(EntityType.SNOWBALL, world.level);
        AAGunType type = mock(AAGunType.class);
        when(type.getHealth()).thenReturn(1000);
        when(type.getName()).thenReturn("Test AA gun");
        when(type.getHitBoxWidth()).thenReturn(2F);
        when(type.getHitBoxHeight()).thenReturn(2F);
        try (var types = mockStatic(InfoType.class))
        {
            gun.setShortName("testAaGun");
            types.when(() -> InfoType.getInfoType("testAaGun")).thenReturn(type);
            assertSame(type, gun.getConfigType());
        }
        return gun;
    }
}
