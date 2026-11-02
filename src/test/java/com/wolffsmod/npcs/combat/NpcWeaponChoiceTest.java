package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcWeaponChoiceTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void anAutomaticallySelectedLauncherUsesCompatibleSuppliesDespiteTheRifleProjectileSlot()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        NpcTacticState state = new NpcTacticState();
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(state);
        npc.inventory = new DataInventory(npc);
        ItemStack launcher = new ItemStack(Items.BLAZE_ROD);
        ItemStack rifleAmmo = new ItemStack(Items.IRON_NUGGET);
        ItemStack rocket = new ItemStack(Items.GOLD_NUGGET);
        IItemStack projectile = mock(IItemStack.class);
        IItemStack supply = mock(IItemStack.class);
        when(projectile.getMCItemStack()).thenReturn(rifleAmmo);
        when(supply.getMCItemStack()).thenReturn(rocket);
        npc.inventory.weapons.put(1, projectile);
        npc.inventory.drops.put(3, supply);
        when(npc.getMainHandItem()).thenReturn(launcher);
        when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        state.getSafety().setSelectedWeapon(launcher);
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> FlansEquipment.acceptsAmmunition(launcher, rocket)).thenReturn(true);
            // Legacy explicit slot overrides remain available for manually equipped weapons.
            equipment.when(() -> FlansEquipment.acceptsAmmunition(launcher, rifleAmmo, true)).thenReturn(true);
            assertSame(rifleAmmo, npc.inventory.getProjectile().getMCItemStack());
            assertSame(rocket, NpcWeaponChoice.ammunition(npc, launcher));
            assertSame(rocket, NpcAmmunition.infiniteTemplate(npc, launcher).orElseThrow());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void anOrdinaryNpcExchangesActualWeaponsForItsTargetWithEitherAmmunitionPolicy(boolean consume)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        NpcTacticState state = new NpcTacticState();
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(state);
        ServerLevel level = mock(ServerLevel.class);
        AtomicLong time = new AtomicLong(100L);
        when(level.getGameTime()).thenAnswer(call -> time.get());
        when(npc.level()).thenReturn(level);
        npc.inventory = new DataInventory(npc);
        ItemStack rifle = new ItemStack(Items.STICK);
        ItemStack launcher = new ItemStack(Items.BLAZE_ROD);
        IItemStack rifleEntry = mock(IItemStack.class);
        IItemStack launcherEntry = mock(IItemStack.class);
        when(rifleEntry.getMCItemStack()).thenReturn(rifle);
        when(launcherEntry.getMCItemStack()).thenReturn(launcher);
        npc.inventory.weapons.put(0, rifleEntry);
        npc.inventory.drops.put(2, launcherEntry);
        when(npc.getMainHandItem()).thenAnswer(call -> npc.inventory.weapons.get(0).getMCItemStack());
        when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        LivingEntity target = mock(LivingEntity.class);
        when(npc.distanceTo(target)).thenReturn(40F);
        EquippedWeaponProperties rifleStats = mock(EquippedWeaponProperties.class);
        when(rifleStats.damage()).thenReturn(20D);
        when(rifleStats.shotDelay()).thenReturn(2D);
        EquippedWeaponProperties launcherStats = mock(EquippedWeaponProperties.class);
        when(launcherStats.damage()).thenReturn(100D);
        when(launcherStats.shotDelay()).thenReturn(40D);
        try (var equipment = mockStatic(FlansEquipment.class); var options = mockStatic(NpcEquipment.class); var vehicles = mockStatic(NpcVehicleThreats.class))
        {
            options.when(() -> NpcEquipment.enabled(npc, Feature.TACTICAL_WEAPON_SWITCH)).thenReturn(true);
            options.when(() -> NpcEquipment.enabled(npc, Feature.ITEM_WEAPONS)).thenReturn(true);
            options.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(consume);
            options.when(() -> NpcEquipment.ranged(rifle)).thenReturn(true);
            options.when(() -> NpcEquipment.ranged(launcher)).thenReturn(true);
            equipment.when(() -> FlansEquipment.hasLoadedRound(rifle)).thenReturn(true);
            equipment.when(() -> FlansEquipment.hasLoadedRound(launcher)).thenReturn(true);
            equipment.when(() -> FlansEquipment.getWeaponProperties(rifle, ItemStack.EMPTY)).thenReturn(Optional.of(rifleStats));
            equipment.when(() -> FlansEquipment.getWeaponProperties(launcher, ItemStack.EMPTY)).thenReturn(Optional.of(launcherStats));
            equipment.when(() -> FlansEquipment.getArmorPenetration(rifle, ItemStack.EMPTY)).thenReturn(OptionalDouble.of(5D));
            equipment.when(() -> FlansEquipment.getArmorPenetration(launcher, ItemStack.EMPTY)).thenReturn(OptionalDouble.of(100D));
            vehicles.when(() -> NpcVehicleThreats.groundVehicle(target)).thenReturn(true);
            assertTrue(NpcWeaponChoice.select(npc, target));
            assertSame(launcherEntry, npc.inventory.weapons.get(0));
            assertSame(rifleEntry, npc.inventory.drops.get(2));
            vehicles.when(() -> NpcVehicleThreats.groundVehicle(target)).thenReturn(false);
            assertFalse(NpcWeaponChoice.select(npc, target), "changing targets cannot bypass the switch cooldown");
            time.set(141L);
            assertTrue(NpcWeaponChoice.select(npc, target));
            assertSame(rifleEntry, npc.inventory.weapons.get(0));
            assertSame(launcherEntry, npc.inventory.drops.get(2));
        }
    }
}
