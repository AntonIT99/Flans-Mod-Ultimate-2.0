package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.IMechaEquipment;
import com.flansmodultimate.common.driveables.EnumMechaItemType;
import com.flansmodultimate.common.driveables.EnumMechaToolType;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.types.MechaItemType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import noppes.npcs.ModelData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcMechaToolsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void swordsUsePlayerMechaStatisticsAndOtherItemsCannotGrantMelee()
    {
        MechaType mecha = mock(MechaType.class);
        when(mecha.getReach()).thenReturn(4F);
        ItemStack sword = tool(EnumMechaToolType.SWORD, 2F, 3F);
        var properties = FlansEquipment.getMechaMeleeProperties(mecha, sword).orElseThrow();
        assertEquals(12F, properties.damage());
        assertEquals(5, properties.delay());
        assertEquals(12D, properties.reach());
        assertTrue(FlansEquipment.getMechaMeleeProperties(mecha, tool(EnumMechaToolType.PICKAXE, 10F, 10F)).isEmpty());
        assertTrue(FlansEquipment.getMechaMeleeProperties(mecha, new ItemStack(Items.DIAMOND_SWORD)).isEmpty());
        assertTrue(FlansEquipment.getMechaMeleeProperties(mecha, ItemStack.EMPTY).isEmpty());
        ItemStack upgrade = tool(EnumMechaToolType.SWORD, 1F, 1F);
        MechaItemType upgradeType = ((MechaAddonItem) upgrade.getItem()).getConfigType();
        when(upgradeType.getMechaItemType()).thenReturn(EnumMechaItemType.UPGRADE);
        assertTrue(FlansEquipment.getMechaMeleeProperties(mecha, upgrade).isEmpty());
    }

    @Test
    void inventorySwordEnablesMeleeAndSelectionTracksRemovalWithoutMovingItems()
    {
        EntityCustomNpc npc = mock(EntityCustomNpc.class, withSettings().extraInterfaces(IMechaEquipment.class));
        npc.modelData = mock(ModelData.class);
        npc.inventory = new DataInventory(npc);
        FlanModelEntity model = mock(FlanModelEntity.class);
        MechaType mecha = mock(MechaType.class);
        when(mecha.getDriveableKind()).thenReturn(DriveableKind.MECHA);
        when(mecha.getReach()).thenReturn(2F);
        when(model.getInfoType()).thenReturn(mecha);
        when(npc.modelData.getEntity(npc)).thenReturn(model);
        when(npc.getMainHandItem()).thenReturn(new ItemStack(Items.DIAMOND_SWORD));
        when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        IMechaEquipment equipment = (IMechaEquipment) npc;
        // Editor copies have inventory data before world entity data has supplied the mounted tool.
        when(equipment.flansmodultimateMechaTool(false)).thenReturn(ItemStack.EMPTY);
        assertTrue(NpcModels.meleeless(npc));
        ItemStack first = tool(EnumMechaToolType.SWORD, 1F, 2F);
        ItemStack second = tool(EnumMechaToolType.SWORD, 2F, 3F);
        npc.inventory.drops.put(4, wrapped(first));
        npc.inventory.drops.put(7, wrapped(second));
        assertSame(first, NpcMechaTools.findSword(npc));
        assertEquals(6F, NpcMechaTools.properties(npc).orElseThrow().damage());
        assertFalse(NpcModels.meleeless(npc));
        assertFalse(NpcEquipment.meleeAuthority(npc));
        npc.inventory.drops.remove(4);
        assertSame(second, NpcMechaTools.findSword(npc));
        assertEquals(1, second.getCount());
        npc.inventory.drops.clear();
        assertTrue(NpcModels.meleeless(npc));
        // Without a sword, a melee damage of its own above 0 lets it strike with its arms.
        npc.stats = mock(DataStats.class);
        npc.stats.melee = mock(DataMelee.class);
        when(npc.stats.melee.getStrength()).thenReturn(8);
        assertFalse(NpcModels.meleeless(npc));
        when(npc.stats.melee.getStrength()).thenReturn(0);
        assertTrue(NpcModels.meleeless(npc));
    }

    private static ItemStack tool(EnumMechaToolType function, float speed, float reach)
    {
        MechaItemType type = mock(MechaItemType.class);
        when(type.getFunction()).thenReturn(function);
        when(type.getMechaItemType()).thenReturn(EnumMechaItemType.TOOL);
        when(type.getSpeed()).thenReturn(speed);
        when(type.getReach()).thenReturn(reach);
        MechaAddonItem item = mock(MechaAddonItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        when(stack.getCount()).thenReturn(1);
        return stack;
    }

    private static IItemStack wrapped(ItemStack stack)
    {
        IItemStack wrapped = mock(IItemStack.class);
        when(wrapped.getMCItemStack()).thenReturn(stack);
        return wrapped;
    }
}
