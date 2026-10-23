package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcThrownWeaponTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void projectileSlotAndMainHandThrowsConsumeOnlyAfterSuccessfulNativeLaunch()
    {
        for (boolean inHand : List.of(false, true))
            for (boolean consume : List.of(false, true))
            {
                EntityNPCInterface npc = mock(EntityNPCInterface.class);
                LivingEntity target = mock(LivingEntity.class);
                ServerLevel level = mock(ServerLevel.class);
                when(npc.level()).thenReturn(level);
                when(target.level()).thenReturn(level);
                when(target.isAlive()).thenReturn(true);
                when(npc.getEyePosition()).thenReturn(Vec3.ZERO);
                when(target.getEyePosition()).thenReturn(new Vec3(12D, 0D, 0D));
                when(target.getDeltaMovement()).thenReturn(Vec3.ZERO);
                ItemStack throwable = new ItemStack(Items.STICK, 2);
                ItemStack held = inHand ? throwable : new ItemStack(Items.IRON_SWORD);
                when(npc.getMainHandItem()).thenReturn(held);
                when(npc.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(held);
                when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
                npc.inventory = new DataInventory(npc);
                IItemStack wrapped = mock(IItemStack.class);
                when(wrapped.getMCItemStack()).thenReturn(throwable);
                if (!inHand)
                    npc.inventory.weapons.put(1, wrapped);
                EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
                when(properties.throwable()).thenReturn(true);
                when(properties.ranged()).thenReturn(true);
                ProjectileShot shot = mock(ProjectileShot.class);
                when(shot.speed()).thenReturn(2.5F);
                try (var equipment = mockStatic(FlansEquipment.class); var options = mockStatic(NpcEquipment.class, CALLS_REAL_METHODS))
                {
                    options.when(() -> NpcEquipment.enabled(eq(npc), any())).thenReturn(true);
                    options.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(consume);
                    equipment.when(() -> FlansEquipment.getWeaponProperties(throwable, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
                    equipment.when(() -> FlansEquipment.prepareThrow(npc, throwable)).thenReturn(Optional.of(shot));
                    assertTrue(NpcEquipment.ranged(npc));
                    assertFalse(NpcItemAttacks.ranged(npc, target));
                    assertEquals(2, throwable.getCount());
                    AbstractArrow projectile = mock(AbstractArrow.class);
                    projectile.pickup = AbstractArrow.Pickup.ALLOWED;
                    when(shot.launch(any(), any(), anyBoolean())).thenReturn(List.of(projectile));
                    assertTrue(NpcItemAttacks.ranged(npc, target));
                    assertEquals(consume ? 1 : 2, throwable.getCount());
                    assertFalse(npc.updateAI);
                    assertTrue(NpcItemAttacks.ranged(npc, target));
                    assertEquals(consume ? 0 : 2, throwable.getCount());
                    assertEquals(consume ? AbstractArrow.Pickup.ALLOWED : AbstractArrow.Pickup.CREATIVE_ONLY, projectile.pickup);
                    if (!inHand)
                    {
                        assertEquals(!consume, npc.inventory.weapons.containsKey(1));
                        assertSame(held, npc.getMainHandItem());
                        verify(npc, never()).setItemInHand(any(), any());
                    }
                    else if (consume)
                        verify(npc).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
    }

    @Test
    void aConsumedMainHandThrowableIsImmediatelyReplacedByAnInventoryGunAndAmmo()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity target = mock(LivingEntity.class);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(npc.isAlive()).thenReturn(true);
        when(target.level()).thenReturn(level);
        when(target.isAlive()).thenReturn(true);
        when(npc.getEyePosition()).thenReturn(Vec3.ZERO);
        when(target.getEyePosition()).thenReturn(new Vec3(12D, 0D, 0D));
        when(target.getDeltaMovement()).thenReturn(Vec3.ZERO);
        ItemStack throwable = new ItemStack(Items.STICK);
        ItemStack gun = new ItemStack(Items.BLAZE_ROD);
        ItemStack ammunition = new ItemStack(Items.IRON_NUGGET, 3);
        npc.inventory = new DataInventory(npc);
        IItemStack thrownEntry = mock(IItemStack.class);
        IItemStack gunEntry = mock(IItemStack.class);
        IItemStack ammoEntry = mock(IItemStack.class);
        when(thrownEntry.getMCItemStack()).thenReturn(throwable);
        when(gunEntry.getMCItemStack()).thenReturn(gun);
        when(ammoEntry.getMCItemStack()).thenReturn(ammunition);
        npc.inventory.weapons.put(0, thrownEntry);
        npc.inventory.drops.put(0, gunEntry);
        npc.inventory.drops.put(3, ammoEntry);
        when(npc.getMainHandItem()).thenAnswer(call -> npc.inventory.weapons.get(0).getMCItemStack());
        when(npc.getItemInHand(InteractionHand.MAIN_HAND)).thenAnswer(call -> npc.getMainHandItem());
        EquippedWeaponProperties thrownProperties = mock(EquippedWeaponProperties.class);
        when(thrownProperties.throwable()).thenReturn(true);
        when(thrownProperties.ranged()).thenReturn(true);
        EquippedWeaponProperties gunProperties = mock(EquippedWeaponProperties.class);
        when(gunProperties.ranged()).thenReturn(true);
        ProjectileShot shot = mock(ProjectileShot.class);
        when(shot.speed()).thenReturn(2.5F);
        when(shot.launch(any(), any(), anyBoolean())).thenReturn(List.of(mock(Entity.class)));
        try (var equipment = mockStatic(FlansEquipment.class); var options = mockStatic(NpcEquipment.class, CALLS_REAL_METHODS))
        {
            options.when(() -> NpcEquipment.enabled(eq(npc), any())).thenReturn(true);
            equipment.when(() -> FlansEquipment.getWeaponProperties(throwable, ItemStack.EMPTY)).thenReturn(Optional.of(thrownProperties));
            equipment.when(() -> FlansEquipment.getWeaponProperties(gun, ItemStack.EMPTY)).thenReturn(Optional.of(gunProperties));
            equipment.when(() -> FlansEquipment.prepareThrow(npc, throwable)).thenReturn(Optional.of(shot));
            equipment.when(() -> FlansEquipment.acceptsAmmunition(gun, ammunition)).thenReturn(true);
            assertTrue(NpcItemAttacks.ranged(npc, target));
            assertTrue(throwable.isEmpty());
            assertSame(gunEntry, npc.inventory.weapons.get(0));
            assertSame(ammoEntry, npc.inventory.weapons.get(1));
            assertTrue(npc.inventory.drops.isEmpty());
            assertEquals(3, ammunition.getCount());
            assertFalse(npc.updateAI);
        }
    }

    @Test
    @SuppressWarnings("java:S3011") // Mockito skips construction; install the final AI settings that the combat goal reads.
    void aProjectileSlotThrowDisplaysItsItemDuringWindupAndClearsItOnThrowOrCancellation() throws ReflectiveOperationException
    {
        for (boolean grenade : List.of(false, true))
        {
            EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcThrowPresentation.class, NpcEquipmentRangedGoal.Holder.class));
            var aiField = EntityNPCInterface.class.getField("ais");
            aiField.setAccessible(true);
            aiField.set(npc, new DataAI(npc));
            AtomicReference<ItemStack> display = new AtomicReference<>(ItemStack.EMPTY);
            NpcThrowPresentation presentation = (NpcThrowPresentation) npc;
            doAnswer(call ->
            {
                display.set(call.getArgument(0));
                return null;
            }).when(presentation).wolffsmodnpcsThrowDisplay(any());
            LivingEntity target = mock(LivingEntity.class);
            when(npc.getTarget()).thenReturn(target);
            ServerLevel level = mock(ServerLevel.class);
            when(npc.level()).thenReturn(level);
            when(npc.isAlive()).thenReturn(true);
            when(target.level()).thenReturn(level);
            when(target.isAlive()).thenReturn(true);
            when(npc.getEyePosition()).thenReturn(Vec3.ZERO);
            when(target.getEyePosition()).thenReturn(new Vec3(12D, 0D, 0D));
            when(target.getDeltaMovement()).thenReturn(Vec3.ZERO);
            LookControl look = mock(LookControl.class);
            PathNavigation navigation = mock(PathNavigation.class);
            when(npc.getLookControl()).thenReturn(look);
            when(npc.getNavigation()).thenReturn(navigation);
            Sensing sensing = mock(Sensing.class);
            when(npc.getSensing()).thenReturn(sensing);
            when(sensing.hasLineOfSight(target)).thenReturn(true);
            npc.stats = mock(DataStats.class);
            npc.stats.ranged = mock(DataRanged.class);
            when(npc.stats.ranged.getRange()).thenReturn(32);
            when(npc.isInRange(target, 32D)).thenReturn(true);
            ItemStack throwable = new ItemStack(Items.STICK);
            ItemStack spare = new ItemStack(Items.STICK);
            ItemStack held = new ItemStack(Items.IRON_SWORD);
            when(npc.getMainHandItem()).thenReturn(held);
            when(npc.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(held);
            when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
            npc.inventory = new DataInventory(npc);
            IItemStack wrapped = mock(IItemStack.class);
            when(wrapped.getMCItemStack()).thenReturn(throwable);
            npc.inventory.weapons.put(1, wrapped);
            IItemStack spareEntry = mock(IItemStack.class);
            when(spareEntry.getMCItemStack()).thenReturn(spare);
            npc.inventory.drops.put(0, spareEntry);
            EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
            when(properties.throwable()).thenReturn(true);
            when(properties.ranged()).thenReturn(true);
            when(properties.chargeTime()).thenReturn(8);
            when(properties.shotDelay()).thenReturn(20D);
            ProjectileShot shot = mock(ProjectileShot.class);
            when(shot.speed()).thenReturn(2.5F);
            when(shot.launch(any(), any(), anyBoolean())).thenReturn(List.of(mock(Entity.class)));
            try (var equipment = mockStatic(FlansEquipment.class);
                var projectiles = mockStatic(FlansProjectiles.class);
                var options = mockStatic(NpcEquipment.class, CALLS_REAL_METHODS);
                var vehicles = mockStatic(NpcVehicleState.class);
                var attacks = mockStatic(NpcItemAttacks.class, CALLS_REAL_METHODS))
            {
                options.when(() -> NpcEquipment.enabled(eq(npc), any())).thenReturn(true);
                options.when(() -> NpcEquipment.offHandGun(npc)).thenReturn(false);
                NpcVehicleState vehicle = mock(NpcVehicleState.class, RETURNS_DEEP_STUBS);
                vehicles.when(() -> NpcVehicleState.of(npc)).thenReturn(vehicle);
                attacks.when(() -> NpcItemAttacks.launched(eq(npc), eq(target), any())).thenAnswer(call -> null);
                if (grenade)
                {
                    projectiles.when(() -> FlansProjectiles.isGrenade(throwable)).thenReturn(true);
                    projectiles.when(() -> FlansProjectiles.isGrenade(spare)).thenReturn(true);
                    projectiles.when(() -> FlansProjectiles.prepare(eq(npc), eq(throwable), eq(ItemStack.EMPTY), isNull(), isNull(), eq(false), any())).thenReturn(Optional.of(shot));
                    projectiles.when(() -> FlansProjectiles.prepare(eq(npc), eq(spare), eq(ItemStack.EMPTY), isNull(), isNull(), eq(false), any())).thenReturn(Optional.of(shot));
                }
                else
                {
                    equipment.when(() -> FlansEquipment.getWeaponProperties(throwable, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
                    equipment.when(() -> FlansEquipment.getWeaponProperties(spare, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
                    equipment.when(() -> FlansEquipment.prepareThrow(npc, throwable)).thenReturn(Optional.of(shot));
                    equipment.when(() -> FlansEquipment.prepareThrow(npc, spare)).thenReturn(Optional.of(shot));
                }
                NpcEquipmentRangedGoal goal = new NpcEquipmentRangedGoal(npc);
                when(((NpcEquipmentRangedGoal.Holder) npc).wolffsmodnpcsItemGoal()).thenReturn(goal);
                goal.tick();
                assertSame(throwable, display.get());
                assertSame(held, npc.getMainHandItem());
                goal.stop();
                assertTrue(display.get().isEmpty());
                assertEquals(1, throwable.getCount());
                int windup = grenade ? 10 : 8;
                for (int tick = 0; tick < windup; tick++)
                {
                    goal.tick();
                    assertSame(throwable, display.get());
                    assertEquals(1, throwable.getCount());
                }
                goal.tick();
                assertTrue(display.get().isEmpty());
                assertTrue(throwable.isEmpty());
                assertSame(held, npc.getMainHandItem());
                assertSame(spareEntry, npc.inventory.weapons.get(1));
                assertTrue(npc.inventory.drops.isEmpty());
                for (int tick = 0; tick < 19; tick++)
                {
                    assertEquals(!grenade, NpcEquipmentRangedGoal.rushes(npc));
                    goal.tick();
                    assertTrue(display.get().isEmpty());
                    assertEquals(1, spare.getCount());
                }
                for (int tick = 0; tick < windup; tick++)
                {
                    goal.tick();
                    assertSame(spare, display.get());
                    assertEquals(1, spare.getCount());
                }
                goal.tick();
                assertTrue(display.get().isEmpty());
                assertTrue(spare.isEmpty());
                assertFalse(npc.inventory.weapons.containsKey(1));
                verify(npc, never()).setItemInHand(any(), any());
                verify(npc, never()).setItemSlot(any(), any());
                verify(npc, never()).startUsingItem(any());
            }
        }
    }
}
