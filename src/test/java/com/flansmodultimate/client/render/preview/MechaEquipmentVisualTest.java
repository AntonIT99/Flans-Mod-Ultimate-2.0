package com.flansmodultimate.client.render.preview;

import com.flansmodultimate.api.equipment.IMechaEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MechaEquipmentVisualTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void posedMechaReadsToolsFromItsCurrentOwnerRatherThanSharedModelState()
    {
        Entity equipped = mock(Entity.class, withSettings().extraInterfaces(IMechaEquipment.class));
        ItemStack tool = new ItemStack(Items.IRON_SWORD);
        when(((IMechaEquipment) equipped).flansmodultimateMechaTool(false)).thenReturn(tool);
        when(((IMechaEquipment) equipped).flansmodultimateMechaTool(true)).thenReturn(ItemStack.EMPTY);
        var visual = new PosedModelVisual();
        visual.setOwner(equipped, null);
        assertSame(tool, visual.getMechaTool(false));
        assertTrue(visual.getMechaTool(true).isEmpty());
        visual.setOwner(mock(Entity.class), null);
        assertTrue(visual.getMechaTool(false).isEmpty());
    }
}
