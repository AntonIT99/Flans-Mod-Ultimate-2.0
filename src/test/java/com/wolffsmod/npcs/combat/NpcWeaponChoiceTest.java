package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.api.projectiles.AmmunitionArmor;
import com.flansmodultimate.api.projectiles.DetonationRadii;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
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
import net.minecraft.world.phys.Vec3;

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

    @Test
    void aPiercingRoundWinsAgainstPlateAndLosesToBlastWhereThereIsNone()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        when(npc.position()).thenReturn(Vec3.ZERO);
        LivingEntity target = mock(LivingEntity.class);
        when(npc.distanceTo(target)).thenReturn(40F);
        ItemStack piercing = new ItemStack(Items.IRON_NUGGET);
        ItemStack explosive = new ItemStack(Items.GOLD_NUGGET);
        try (var projectiles = mockStatic(FlansProjectiles.class); var threats = mockStatic(NpcVehicleThreats.class))
        {
            projectiles.when(() -> FlansProjectiles.getArmorPerformance(piercing, 0)).thenReturn(Optional.of(new AmmunitionArmor(120D, false)));
            projectiles.when(() -> FlansProjectiles.getArmorPerformance(explosive, 0)).thenReturn(Optional.of(new AmmunitionArmor(15D, false)));
            projectiles.when(() -> FlansProjectiles.getDetonationRadii(explosive)).thenReturn(Optional.of(new DetonationRadii(4D, 4D, 0D, 0D)));
            threats.when(() -> NpcVehicleThreats.groundVehicle(target)).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.protection(target, Vec3.ZERO)).thenReturn(Optional.of(new PartArmor(80F, 80F)));
            assertTrue(NpcWeaponChoice.ammunitionScore(npc, target, piercing) > NpcWeaponChoice.ammunitionScore(npc, target, explosive), "80 mm of plate has to be beaten first");
            threats.when(() -> NpcVehicleThreats.protection(target, Vec3.ZERO)).thenReturn(Optional.of(new PartArmor(0F, 0F)));
            assertTrue(NpcWeaponChoice.ammunitionScore(npc, target, explosive) > NpcWeaponChoice.ammunitionScore(npc, target, piercing),
                "an unarmoured hull calls for the blast, not for penetration no plate needs");
            threats.when(() -> NpcVehicleThreats.protection(target, Vec3.ZERO)).thenReturn(Optional.empty());
            assertTrue(NpcWeaponChoice.ammunitionScore(npc, target, piercing) > NpcWeaponChoice.ammunitionScore(npc, target, explosive), "unknown protection is never treated as immunity");
            when(npc.distanceTo(target)).thenReturn(4F);
            assertEquals(-1000D, NpcWeaponChoice.ammunitionScore(npc, target, explosive), "a round whose blast would catch the NPC is never chosen");
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
