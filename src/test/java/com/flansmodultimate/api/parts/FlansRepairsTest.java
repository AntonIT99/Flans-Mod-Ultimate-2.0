/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.item.ToolItem;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlansRepairsTest
{
    @Test
    void toolRayDoesNotPreferAnNpcBehindANearerDriveable()
    {
        Player player = mock(Player.class);
        LivingEntity npc = mock(LivingEntity.class, withSettings().extraInterfaces(IFlanRepairTarget.class));
        Driveable driveable = mock(Driveable.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        PartBody body = body(null);
        body.setPose(new PartBodyPose(new Vec3(0D, 0D, -4D), 0F, 1F, MountAim.REST));
        when(((IFlanRepairTarget) npc).flansmodultimatePartBody()).thenReturn(Optional.of(body));
        when(level.getEntities(eq(player), any(), any())).thenReturn(List.of(npc, driveable));
        BulletHit hit = mock(BulletHit.class);
        when(hit.getIntersectTime()).thenReturn(0.1F);
        when(driveable.attackFromBullet(any(), any(), eq(true))).thenReturn(List.of(hit));
        assertSame(driveable, FlansRepairs.toolTarget(player, Vec3.ZERO, new Vec3(0D, 0D, -6D)).orElseThrow());
        when(hit.getIntersectTime()).thenReturn(0.9F);
        assertSame(npc, FlansRepairs.toolTarget(player, Vec3.ZERO, new Vec3(0D, 0D, -6D)).orElseThrow());
    }

    @Test
    void npcCarrierDeclarationRequiresALivingCarrierAndDoesNotImplyHullSupport()
    {
        LivingEntity carrier = mock(LivingEntity.class, withSettings().extraInterfaces(IFlanRepairTarget.class));
        LivingEntity aircraft = mock(LivingEntity.class);
        VehicleType type = mock(VehicleType.class);
        when(type.isCanMountEntity()).thenReturn(true);
        PartBody body = body(type);
        when(((IFlanRepairTarget) carrier).flansmodultimatePartBody()).thenReturn(Optional.of(body));
        when(carrier.isAlive()).thenReturn(true);
        assertTrue(FlansRepairs.isCarrier(carrier));
        assertFalse(FlansRepairs.supports(carrier, aircraft));
        when(type.isCanMountEntity()).thenReturn(false);
        assertFalse(FlansRepairs.isCarrier(carrier));
        assertFalse(FlansRepairs.supports(carrier, aircraft));
        when(type.isCanMountEntity()).thenReturn(true);
        when(carrier.isAlive()).thenReturn(false);
        assertFalse(FlansRepairs.isCarrier(carrier));
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void realToolsConsumeDurabilityAndEnforceActorCooldownForNpcRepairTargets()
    {
        LivingEntity actor = mock(LivingEntity.class);
        LivingEntity target = mock(LivingEntity.class, withSettings().extraInterfaces(IFlanRepairTarget.class));
        IFlanRepairTarget repairable = (IFlanRepairTarget) target;
        ServerLevel level = mock(ServerLevel.class);
        AtomicLong time = new AtomicLong();
        AtomicInteger damage = new AtomicInteger();
        CompoundTag data = new CompoundTag();
        when(actor.level()).thenReturn(level);
        when(target.level()).thenReturn(level);
        when(actor.isAlive()).thenReturn(true);
        when(target.isAlive()).thenReturn(true);
        when(actor.getEyePosition()).thenReturn(Vec3.ZERO);
        when(actor.getPersistentData()).thenReturn(data);
        when(level.getGameTime()).thenAnswer(invocation -> time.get());
        PartBody body = body(null);
        body.damage("core", 500F, false);
        when(repairable.flansmodultimatePartBody()).thenReturn(Optional.of(body));
        when(repairable.flansmodultimateRepair(any(), anyInt())).thenAnswer(invocation -> FlansRepairs.repairBody(body, invocation.getArgument(0), invocation.getArgument(1)));
        ToolType definition = mock(ToolType.class);
        when(definition.isHealDriveables()).thenReturn(true);
        when(definition.getToolLife()).thenReturn(200);
        when(definition.getHealAmount()).thenReturn(100);
        ToolItem item = mock(ToolItem.class);
        when(item.getConfigType()).thenReturn(definition);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        when(stack.getDamageValue()).thenAnswer(invocation -> damage.get());
        doAnswer(invocation ->
        {
            damage.set(invocation.getArgument(0));
            return null;
        }).when(stack).setDamageValue(anyInt());
        assertTrue(FlansRepairs.useTool(actor, target, stack));
        assertEquals(550F, body.getPart("core").orElseThrow().health());
        assertEquals(1, damage.get());
        assertFalse(FlansRepairs.useTool(actor, target, stack));
        time.set(20L);
        assertTrue(FlansRepairs.useTool(actor, target, stack));
        assertEquals(600F, body.getPart("core").orElseThrow().health());
        assertEquals(2, damage.get());
        damage.set(200);
        time.set(40L);
        assertFalse(FlansRepairs.useTool(actor, target, stack));
    }

    @Test
    void rebuildingRequiresMaterialsAndConsumesThemBeforeTheNextRepair()
    {
        DriveableType type = mock(DriveableType.class);
        when(type.getItemsRequired(any(), isNull())).thenReturn(List.of(new ItemStack(Items.IRON_INGOT, 2)));
        PartBody body = body(type);
        body.damage("leftTrack", 100F, false);
        Player player = mock(Player.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(player.getAbilities()).thenReturn(new Abilities());
        Inventory inventory = new Inventory(player);
        when(player.getInventory()).thenReturn(inventory);
        assertFalse(FlansRepairs.rebuild(body, player, "leftTrack"));
        assertTrue(body.getPart("leftTrack").orElseThrow().destroyed());
        inventory.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        assertTrue(FlansRepairs.rebuild(body, player, "leftTrack"));
        assertEquals(10F, body.getPart("leftTrack").orElseThrow().health());
        assertTrue(inventory.getItem(0).isEmpty());
        assertFalse(FlansRepairs.rebuild(body, player, "leftTrack"));
    }

    @Test
    void nanorepairFuelCannotBeDrawnFromAnEmptyOrInsufficientCan()
    {
        PartType type = mock(PartType.class);
        when(type.getCategory()).thenReturn(PartType.Category.FUEL);
        when(type.getFuel()).thenReturn(100);
        PartItem item = mock(PartItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        AtomicInteger remainingDamage = new AtomicInteger(95);
        when(stack.getDamageValue()).thenAnswer(invocation -> remainingDamage.get());
        doAnswer(invocation ->
        {
            remainingDamage.set(invocation.getArgument(0));
            return null;
        }).when(stack).setDamageValue(anyInt());
        assertFalse(FlansRepairs.consumeFuel(stack, 10));
        verify(stack, never()).shrink(anyInt());
        remainingDamage.set(90);
        assertTrue(FlansRepairs.consumeFuel(stack, 10));
        verify(stack).setDamageValue(100);
        verify(stack).shrink(1);
    }

    private static PartBody body(DriveableType type)
    {
        return new PartBody(type, Map.of(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(1000F, -1F, 0F, -1F, 2F, 2F, 2F, 1F, 0F), EnumDriveablePart.LEFT_TRACK,
            CollisionBox.inWorldUnits(100F, 3F, 0F, 0F, 1F, 1F, 1F, 1F, 0F)), Vec3.ZERO, Vec3.ZERO);
    }
}
