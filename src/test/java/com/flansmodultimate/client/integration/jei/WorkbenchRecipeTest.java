package com.flansmodultimate.client.integration.jei;

import com.flansmodultimate.common.types.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.util.ModUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkbenchRecipeTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void combinesRepeatedCostsBeyondOneStackWithoutChangingSourceStacks()
    {
        ItemStack steel = new ItemStack(Items.IRON_INGOT, 64);
        List<ItemStack> materials = WorkbenchRecipe.combineMaterials(List.of(steel, steel, steel, ItemStack.EMPTY, new ItemStack(Items.REDSTONE, 2)));
        assertEquals(2, materials.size());
        assertEquals(192, materials.get(0).getCount());
        assertEquals(2, materials.get(1).getCount());
        assertEquals(64, steel.getCount());
    }

    @Test
    void differentItemDataStaysInSeparateCosts()
    {
        ItemStack plain = new ItemStack(Items.IRON_INGOT);
        ItemStack tagged = plain.copy();
        tagged.getOrCreateTag().putString("variant", "special");
        assertEquals(2, WorkbenchRecipe.combineMaterials(List.of(plain, tagged)).size());
    }

    @Test
    void enginesMustBeCompatibleAndProvidedInTheRequiredQuantity()
    {
        DriveableType driveable = mock(DriveableType.class);
        when(driveable.numEngines()).thenReturn(2);
        when(driveable.getType()).thenReturn(EnumType.VEHICLE);
        when(driveable.getDriveableRecipe()).thenReturn(List.of(new ItemStack(Items.IRON_INGOT, 64)));
        PartType compatible = mock(PartType.class);
        when(compatible.getCategory()).thenReturn(PartType.Category.ENGINE);
        when(compatible.worksWith(EnumType.VEHICLE)).thenReturn(true);
        when(compatible.getShortName()).thenReturn("compatible");
        PartType planeOnly = mock(PartType.class);
        when(planeOnly.getCategory()).thenReturn(PartType.Category.ENGINE);
        PartType wheel = mock(PartType.class);
        when(wheel.getCategory()).thenReturn(PartType.Category.MISC);

        try (var items = mockStatic(ModUtils.class))
        {
            items.when(() -> ModUtils.getItemStack(driveable)).thenReturn(Optional.of(new ItemStack(Items.MINECART)));
            items.when(() -> ModUtils.getItemStack(compatible, 2)).thenReturn(Optional.of(new ItemStack(Items.FURNACE, 2)));
            WorkbenchRecipe recipe = WorkbenchRecipe.driveable(driveable, new ItemStack(Items.CRAFTING_TABLE), List.of(compatible, planeOnly, wheel));
            assertEquals(2, recipe.engineCount());
            assertEquals(1, recipe.engines().size());
            assertEquals(2, recipe.engines().get(0).getCount());
            assertEquals(64, recipe.materials().get(0).getCount());
            assertTrue(recipe.output().is(Items.MINECART));
        }
    }
}
