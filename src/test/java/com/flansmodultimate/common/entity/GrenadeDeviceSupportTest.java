package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GrenadeDeviceSupportTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static ServerLevel level()
    {
        ServerLevel level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(level.getServer()).thenReturn(server);
        when(server.isSameThread()).thenReturn(true);
        return level;
    }

    @Test
    void remainingUsesAndStationaryPlacementSurviveSavingAndOldBagsKeepTheirAuthoredUses()
    {
        GrenadeType type = mock(GrenadeType.class);
        when(type.getNumUses()).thenReturn(5);
        Grenade source = savedGrenade();
        source.shortname = "medicalBag";
        source.axes.setAngles(35F, -75F, 90F);
        source.numUsesRemaining = 1;
        source.stuck = true;
        source.stuckPos = new BlockPos(3, 64, 7);
        CompoundTag saved = new CompoundTag();
        source.addAdditionalSaveData(saved);
        try (var types = mockStatic(InfoType.class))
        {
            types.when(() -> InfoType.getInfoType("medicalBag")).thenReturn(type);
            Grenade restored = savedGrenade();
            restored.readAdditionalSaveData(saved);
            assertEquals(1, restored.numUsesRemaining);
            assertEquals(90F, restored.axes.getRoll(), 0.001F);
            assertTrue(restored.stuck);
            assertEquals(source.stuckPos, restored.stuckPos);
            saved.remove(Grenade.NBT_USES);
            restored.readAdditionalSaveData(saved);
            assertEquals(5, restored.numUsesRemaining);
        }
    }

    @Test
    void spawnDataCarriesTheChosenDirectionSpeedAndRoll() throws ReflectiveOperationException
    {
        Grenade source = savedGrenade();
        source.shortname = "debris";
        source.velocity = new Vec3(0.1D, 0.8D, -0.2D);
        source.axes.setAngles(35F, -75F, 90F);
        Grenade target = spy(savedGrenade());
        doNothing().when(target).refreshDimensions();
        GrenadeType type = mock(GrenadeType.class);
        var bytes = com.flansmodultimate.platform.network.TestPacketBuffers.buffer();
        try (var types = mockStatic(InfoType.class))
        {
            types.when(() -> InfoType.getInfoType("debris")).thenReturn(type);
            source.writeSpawnData(new com.flansmodultimate.platform.network.PacketBuffer(bytes));
            target.readSpawnData(new com.flansmodultimate.platform.network.PacketBuffer(bytes));
            assertEquals(0, bytes.readableBytes());
            assertEquals(source.velocity, target.getDeltaMovement());
            assertEquals(35F, target.axes.getYaw(), 0.001F);
            assertEquals(-75F, target.axes.getPitch(), 0.001F);
            assertEquals(90F, target.axes.getRoll(), 0.001F);
            verify(target, never()).discard();
        }
        finally
        {
            bytes.release();
        }
    }

    private static Grenade savedGrenade()
    {
        // 1.21 entity data is a fixed array that only the entity constructor completes, so build a real grenade.
        Grenade grenade = new Grenade(EntityType.SNOWBALL, level());
        grenade.axes = new RotatedAxes();
        return grenade;
    }

    @Test
    void stationaryPlacementAndReleaseAreSynchronizedThroughEntityMetadata()
    {
        Grenade server = savedGrenade();
        Grenade client = savedGrenade();
        server.setStationaryPlacement(true);
        client.getEntityData().assignValues(server.getEntityData().packDirty());
        assertTrue(client.isStuck());
        assertEquals(Vec3.ZERO, client.angularVelocity);
        assertEquals(Vec3.ZERO, client.getDeltaMovement());
        server.setStationaryPlacement(false);
        client.getEntityData().assignValues(server.getEntityData().packDirty());
        assertFalse(client.isStuck());
    }

    private static LivingEntity recipient(ServerLevel level)
    {
        LivingEntity recipient = mock(LivingEntity.class);
        when(recipient.level()).thenReturn(level);
        when(recipient.isAlive()).thenReturn(true);
        when(recipient.getUUID()).thenReturn(UUID.randomUUID());
        when(recipient.getMaxHealth()).thenReturn(20F);
        when(recipient.getHealth()).thenReturn(20F);
        return recipient;
    }

    @Test
    void successfulPlacementCreatesAStationaryDeviceAndLeavesConsumptionToTheCaller()
    {
        ServerLevel level = level();
        LivingEntity operator = recipient(level);
        when(operator.position()).thenReturn(Vec3.ZERO);
        GrenadeType type = mock(GrenadeType.class);
        when(type.isRemote()).thenReturn(true);
        ShootableItem item = mock(ShootableItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack carried = mock(ItemStack.class);
        when(carried.getItem()).thenReturn(item);
        BlockPos support = new BlockPos(1, -1, 0);
        BlockState state = mock(BlockState.class);
        FluidState fluid = mock(FluidState.class);
        when(fluid.isEmpty()).thenReturn(true);
        ServerChunkCache chunks = mock(ServerChunkCache.class);
        when(level.getChunkSource()).thenReturn(chunks);
        when(chunks.hasChunk(anyInt(), anyInt())).thenReturn(true);
        when(level.getBlockState(support)).thenReturn(state);
        when(state.isFaceSturdy(level, support, Direction.UP)).thenReturn(true);
        when(level.getFluidState(support.above())).thenReturn(fluid);
        when(level.noCollision(any(AABB.class))).thenReturn(true);
        when(level.addFreshEntity(any())).thenReturn(true);
        try (var created = mockConstruction(Grenade.class))
        {
            Entity placed = GrenadeDeviceSupport.place(operator, carried, support, new Vec3(1D, 0D, 0D), false).orElseThrow();
            Grenade grenade = created.constructed().get(0);
            assertSame(grenade, placed);
            assertTrue(grenade.stuck);
            assertEquals(support, grenade.stuckPos);
            verify(grenade).setDeltaMovement(Vec3.ZERO);
            verify(carried, never()).shrink(anyInt());
            when(type.getHitBoxSize()).thenReturn(2F);
            when(level.noCollision(any(AABB.class))).thenAnswer(call -> call.getArgument(0, AABB.class).getXsize() <= 1D);
            assertTrue(GrenadeDeviceSupport.place(operator, carried, support, new Vec3(1D, 0D, 0D), false).isEmpty());
            assertEquals(1, created.constructed().size());
            when(level.noCollision(any(AABB.class))).thenReturn(true);
            when(level.getServer().isSameThread()).thenReturn(false);
            assertTrue(GrenadeDeviceSupport.place(operator, carried, support, new Vec3(1D, 0D, 0D), false).isEmpty());
            assertEquals(1, created.constructed().size());
        }
    }

    private static Grenade bag(ServerLevel level, GrenadeType type, int uses)
    {
        Grenade bag = mock(Grenade.class);
        when(bag.level()).thenReturn(level);
        when(bag.getConfigType()).thenReturn(type);
        when(type.isDeployableBag()).thenReturn(true);
        bag.numUsesRemaining = uses;
        return bag;
    }

    @Test
    void onlyTheOwnerCanDetonateAnArmedRemoteChargeIncludingAfterOwnerResolutionIsLost()
    {
        ServerLevel level = level();
        LivingEntity owner = recipient(level);
        LivingEntity stranger = recipient(level);
        GrenadeType type = mock(GrenadeType.class);
        Grenade charge = bag(level, type, 1);
        charge.throwerUUID = owner.getUUID();
        when(type.isRemote()).thenReturn(true);
        when(type.getPrimeDelay()).thenReturn(40);
        charge.tickCount = 39;
        assertTrue(GrenadeDeviceSupport.ownedBy(charge, owner));
        assertFalse(GrenadeDeviceSupport.detonate(charge, owner));
        charge.tickCount = 40;
        assertFalse(GrenadeDeviceSupport.detonate(charge, stranger));
        assertTrue(GrenadeDeviceSupport.detonate(charge, owner));
        verify(charge).detonate(level);
        charge.detonated = true;
        assertFalse(GrenadeDeviceSupport.detonate(charge, owner));
    }

    @Test
    void healingUsesTheAuthoredAmountAndAHealthyRecipientDoesNotConsumeAnotherUse()
    {
        ServerLevel level = level();
        LivingEntity patient = recipient(level);
        AtomicReference<Float> health = new AtomicReference<>(18F);
        when(patient.getHealth()).thenAnswer(call -> health.get());
        doAnswer(call ->
        {
            health.set(Math.min(20F, health.get() + call.getArgument(0, Float.class)));
            return null;
        }).when(patient).heal(anyFloat());
        GrenadeType type = mock(GrenadeType.class);
        when(type.getHealAmount()).thenReturn(4F);
        Grenade bag = bag(level, type, 2);
        assertTrue(GrenadeDeviceSupport.useSupply(bag, patient, ItemStack.EMPTY, stack -> false));
        assertEquals(20F, health.get());
        assertEquals(1, bag.numUsesRemaining);
        assertFalse(GrenadeDeviceSupport.useSupply(bag, patient, ItemStack.EMPTY, stack -> false));
        assertEquals(1, bag.numUsesRemaining);
        verify(bag, never()).discard();
        when(patient.distanceToSqr(bag)).thenReturn(16D);
        health.set(8F);
        assertFalse(GrenadeDeviceSupport.useSupply(bag, patient, ItemStack.EMPTY, stack -> false));
    }

    @Test
    void ammoBagsHonorRearmPermissionInventoryRefusalAndTheLastUse()
    {
        ServerLevel level = level();
        LivingEntity recipient = recipient(level);
        GrenadeType type = mock(GrenadeType.class);
        when(type.getNumClips()).thenReturn(2);
        Grenade bag = bag(level, type, 1);
        ItemStack weapon = mock(ItemStack.class);
        GunItem gun = mock(GunItem.class);
        GunType gunType = mock(GunType.class);
        when(weapon.getItem()).thenReturn(gun);
        when(gun.getConfigType()).thenReturn(gunType);
        when(gunType.getNumAmmoItemsInGun(weapon)).thenReturn(2);
        ItemStack ammo = new ItemStack(Items.IRON_NUGGET, 64);
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> FlansEquipment.createDefaultAmmunition(weapon)).thenReturn(Optional.of(ammo));
            List<ItemStack> delivered = new ArrayList<>();
            assertFalse(GrenadeDeviceSupport.useSupply(bag, recipient, weapon, delivered::add));
            when(gunType.isAllowRearm()).thenReturn(true);
            assertFalse(GrenadeDeviceSupport.useSupply(bag, recipient, weapon, stack -> false));
            assertEquals(1, bag.numUsesRemaining);
            assertTrue(GrenadeDeviceSupport.useSupply(bag, recipient, weapon, delivered::add));
            assertEquals(4, delivered.get(0).getCount());
            assertEquals(64, ammo.getCount());
            assertEquals(0, bag.numUsesRemaining);
            verify(bag).discard();
        }
    }
}
