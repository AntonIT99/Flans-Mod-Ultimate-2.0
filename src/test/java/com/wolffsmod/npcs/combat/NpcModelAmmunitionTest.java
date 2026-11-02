package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcModelAmmunitionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void anAdaptiveCannonUsesItsShellsEvenWhenTheProjectileSlotContainsCrewMgAmmo()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcWeaponOptions options = new NpcWeaponOptions();
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        FlanModelEntity model = mock(FlanModelEntity.class);
        IDriveableType tank = mock(IDriveableType.class);
        when(model.getInfoType()).thenReturn(tank);
        ItemStack mg = new ItemStack(Items.IRON_NUGGET);
        ItemStack shell = new ItemStack(Items.GOLD_NUGGET);
        try (var models = mockStatic(NpcModels.class);
            var throwing = mockStatic(NpcThrowing.class);
            var projectiles = mockStatic(FlansProjectiles.class);
            var equipment = mockStatic(NpcEquipment.class);
            var flans = mockStatic(FlansEquipment.class);
            var ammunition = mockStatic(NpcAmmunition.class, CALLS_REAL_METHODS))
        {
            models.when(() -> NpcModels.flan(npc)).thenReturn(model);
            throwing.when(() -> NpcThrowing.mainProjectile(npc)).thenReturn(mg);
            projectiles.when(() -> FlansProjectiles.isProjectile(mg)).thenReturn(true);
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT)).thenReturn(true);
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.DEFAULT_AMMO)).thenReturn(true);
            flans.when(() -> FlansEquipment.acceptsAmmunition(tank, false, shell)).thenReturn(true);
            flans.when(() -> FlansEquipment.createDefaultAmmunitionChoices(tank, false)).thenReturn(List.of(shell));
            ammunition.when(() -> NpcAmmunition.inventory(npc)).thenReturn(List.of(shell));
            assertSame(shell, NpcAmmunition.modelTemplate(npc), "the selected cannon's compatible inventory ammunition must win over MG slot ammunition");
            ammunition.when(() -> NpcAmmunition.inventory(npc)).thenReturn(List.of());
            assertSame(shell, NpcAmmunition.modelTemplate(npc), "infinite-ammunition vehicles can also use the cannon's default shell");
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(true);
            assertTrue(NpcAmmunition.modelTemplate(npc).isEmpty(), "finite ammunition still cannot create free shells");
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(false);
            flans.when(() -> FlansEquipment.acceptsAmmunition(tank, false, mg)).thenReturn(true);
            assertSame(mg, NpcAmmunition.modelTemplate(npc), "compatible slot ammunition is retained");
            flans.when(() -> FlansEquipment.acceptsAmmunition(tank, false, mg)).thenReturn(false);
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT)).thenReturn(false);
            assertSame(mg, NpcAmmunition.modelTemplate(npc), "opting out retains the legacy arbitrary-slot override");
        }
    }

    @Test
    void emptyVehicleUsesSelectedBanksDefaultOnlyWithoutConsumptionAndDoesNotFillInventory()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.inventory = new DataInventory(npc);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcWeaponOptions options = new NpcWeaponOptions();
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        FlanModelEntity model = mock(FlanModelEntity.class);
        IDriveableType vehicle = mock(IDriveableType.class);
        when(model.getInfoType()).thenReturn(vehicle);
        ItemStack shell = new ItemStack(Items.IRON_NUGGET);
        ItemStack bullet = new ItemStack(Items.GOLD_NUGGET);
        try (var models = mockStatic(NpcModels.class); var equipment = mockStatic(FlansEquipment.class))
        {
            models.when(() -> NpcModels.flan(npc)).thenReturn(model);
            equipment.when(() -> FlansEquipment.createDefaultAmmunitionChoices(vehicle, false)).thenReturn(List.of(shell));
            equipment.when(() -> FlansEquipment.createDefaultAmmunitionChoices(vehicle, true)).thenReturn(List.of(bullet));
            options.set(Feature.SECONDARY_BANK, false);
            assertSame(shell, NpcAmmunition.modelTemplate(npc));
            options.set(Feature.SECONDARY_BANK, true);
            assertSame(bullet, NpcAmmunition.modelTemplate(npc));
            options.set(Feature.CONSUME_AMMO, true);
            assertTrue(NpcAmmunition.modelTemplate(npc).isEmpty());
            options.set(Feature.CONSUME_AMMO, false);
            options.set(Feature.PROJECTILES, false);
            assertTrue(NpcAmmunition.modelTemplate(npc).isEmpty());
            assertTrue(npc.inventory.weapons.isEmpty());
            assertTrue(npc.inventory.drops.isEmpty());
            assertEquals(1, shell.getCount());
            assertEquals(1, bullet.getCount());
        }
    }
}
