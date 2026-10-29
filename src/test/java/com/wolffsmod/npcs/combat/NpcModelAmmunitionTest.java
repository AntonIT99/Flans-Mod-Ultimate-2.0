package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.equipment.FlansEquipment;
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

import java.util.Optional;

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
            equipment.when(() -> FlansEquipment.createDefaultAmmunition(vehicle, false)).thenReturn(Optional.of(shell));
            equipment.when(() -> FlansEquipment.createDefaultAmmunition(vehicle, true)).thenReturn(Optional.of(bullet));
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
