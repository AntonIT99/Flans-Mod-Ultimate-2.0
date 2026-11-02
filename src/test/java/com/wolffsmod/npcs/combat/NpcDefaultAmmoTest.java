package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.IDriveableType;
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
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** A vehicle with several default rounds chooses between them, and may be held to the ammunition it was given. */
class NpcDefaultAmmoTest
{
    /** 80 mm of plate: the piercing shell goes through it, the explosive one does not. */
    private static final PartArmor PLATE = new PartArmor(80F, 80F);

    private final EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
    private final IDriveableType tank = mock(IDriveableType.class);
    private final LivingEntity target = mock(LivingEntity.class);
    private final AtomicLong time = new AtomicLong(1000L);
    private final ItemStack piercing = new ItemStack(Items.IRON_NUGGET);
    private final ItemStack explosive = new ItemStack(Items.GOLD_NUGGET);

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    NpcDefaultAmmoTest()
    {
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(new NpcTacticState());
        npc.inventory = new DataInventory(npc);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(new NpcWeaponOptions());
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenAnswer(call -> time.get());
        when(npc.level()).thenReturn(level);
        when(npc.position()).thenReturn(Vec3.ZERO);
        when(npc.getTarget()).thenReturn(target);
        when(npc.distanceTo(target)).thenReturn(40F);
        when(target.getId()).thenReturn(7);
    }

    @Test
    void anEmptyTankPicksTheShellThatSuitsItsTargetAndStopsWhenTheFallbackIsOff()
    {
        try (var flans = mockStatic(FlansEquipment.class);
            var projectiles = mockStatic(FlansProjectiles.class);
            var threats = mockStatic(NpcVehicleThreats.class);
            var equipment = mockStatic(NpcEquipment.class))
        {
            stub(flans, projectiles, equipment);
            threats.when(() -> NpcVehicleThreats.groundVehicle(target)).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.protection(target, Vec3.ZERO)).thenReturn(Optional.of(PLATE));
            assertSame(piercing, NpcAmmunition.bankTemplate(npc, tank, false), "only the piercing shell beats the plate");

            // Another target is scored for at once, rather than waiting out the interval the choice otherwise stands for.
            when(target.getId()).thenReturn(8);
            threats.when(() -> NpcVehicleThreats.groundVehicle(target)).thenReturn(false);
            assertSame(explosive, NpcAmmunition.bankTemplate(npc, tank, false), "infantry calls for the explosive shell");

            equipment.when(() -> NpcEquipment.enabled(npc, Feature.DEFAULT_AMMO)).thenReturn(false);
            assertTrue(NpcAmmunition.bankTemplate(npc, tank, false).isEmpty(), "held to its own supplies, an empty tank has nothing to fire");
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(true);
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.DEFAULT_AMMO)).thenReturn(true);
            assertTrue(NpcAmmunition.bankTemplate(npc, tank, false).isEmpty(), "ammunition that is used up is never created out of nothing");
        }
    }

    @Test
    void aChosenShellStandsForAWhileAndCarriedAmmunitionKeepsItsPlace()
    {
        try (var flans = mockStatic(FlansEquipment.class);
            var projectiles = mockStatic(FlansProjectiles.class);
            var threats = mockStatic(NpcVehicleThreats.class);
            var equipment = mockStatic(NpcEquipment.class))
        {
            stub(flans, projectiles, equipment);
            threats.when(() -> NpcVehicleThreats.groundVehicle(target)).thenReturn(false);
            assertSame(explosive, NpcAmmunition.bankTemplate(npc, tank, false));
            // The same target now calls for the other shell, which only replaces the chosen one once it may be changed.
            threats.when(() -> NpcVehicleThreats.groundVehicle(target)).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.protection(target, Vec3.ZERO)).thenReturn(Optional.of(PLATE));
            assertSame(explosive, NpcAmmunition.bankTemplate(npc, tank, false), "the rounds are scored again at most every ten ticks");
            time.addAndGet(NpcAmmoSelection.CHECK_INTERVAL);
            assertSame(explosive, NpcAmmunition.bankTemplate(npc, tank, false), "a shell just chosen stands while the loader would still be busy");
            time.addAndGet(NpcAmmoSelection.CHANGE_INTERVAL);
            assertSame(piercing, NpcAmmunition.bankTemplate(npc, tank, false));

            // A round the vehicle actually carries keeps its place ahead of a default round that suits the target as well.
            ItemStack carried = new ItemStack(Items.COPPER_INGOT);
            IItemStack entry = mock(IItemStack.class);
            when(entry.getMCItemStack()).thenReturn(carried);
            npc.inventory.drops.put(3, entry);
            flans.when(() -> FlansEquipment.acceptsAmmunition(tank, false, carried)).thenReturn(true);
            projectiles.when(() -> FlansProjectiles.getArmorPerformance(carried, 0)).thenReturn(Optional.of(new AmmunitionArmor(120D, false)));
            time.addAndGet(NpcAmmoSelection.CHECK_INTERVAL + NpcAmmoSelection.CHANGE_INTERVAL);
            assertSame(carried, NpcAmmunition.bankTemplate(npc, tank, false));
            assertEquals(1, carried.getCount(), "a template is never used up");
        }
    }

    private void stub(MockedStatic<FlansEquipment> flans, MockedStatic<FlansProjectiles> projectiles, MockedStatic<NpcEquipment> equipment)
    {
        equipment.when(() -> NpcEquipment.enabled(npc, Feature.TACTICAL_WEAPON_SWITCH)).thenReturn(true);
        equipment.when(() -> NpcEquipment.enabled(npc, Feature.DEFAULT_AMMO)).thenReturn(true);
        equipment.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(false);
        flans.when(() -> FlansEquipment.createDefaultAmmunitionChoices(tank, false)).thenReturn(List.of(piercing, explosive));
        projectiles.when(() -> FlansProjectiles.getArmorPerformance(piercing, 0)).thenReturn(Optional.of(new AmmunitionArmor(120D, false)));
        projectiles.when(() -> FlansProjectiles.getArmorPerformance(explosive, 0)).thenReturn(Optional.of(new AmmunitionArmor(15D, false)));
        projectiles.when(() -> FlansProjectiles.getDetonationRadii(explosive)).thenReturn(Optional.of(new DetonationRadii(4D, 4D, 0D, 0D)));
    }
}
