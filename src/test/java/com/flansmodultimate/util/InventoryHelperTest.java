package com.flansmodultimate.util;

import com.flansmodultimate.platform.item.ItemStackData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryHelperTest
{
    @BeforeAll
    static void bootstrapRegistries()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void countsAndConsumesIngredientsWithDifferentStackData()
    {
        ItemStack painted = new ItemStack(Items.IRON_INGOT, 2);
        painted.getOrCreateTag().putInt("Paint", 1);
        painted.getOrCreateTag().putString("ammo", "loaded");
        ItemStack renamed = new ItemStack(Items.IRON_INGOT);
        renamed.getOrCreateTag().putString("custom", "another variant");
        SimpleContainer inventory = new SimpleContainer(painted, renamed, new ItemStack(Items.GOLD_INGOT));
        ItemStack ingredient = new ItemStack(Items.IRON_INGOT);

        assertEquals(3, InventoryHelper.countInInventory(inventory, ingredient));
        assertTrue(InventoryHelper.tryConsumeAll(inventory, List.of(ingredient, ingredient), false));
        assertTrue(inventory.getItem(0).isEmpty());
        assertEquals(1, inventory.getItem(1).getCount());
        assertEquals("another variant", inventory.getItem(1).getOrCreateTag().getString("custom"));
        assertEquals(Items.GOLD_INGOT, inventory.getItem(2).getItem());
    }

    @Test
    void missingDuplicateIngredientDoesNotConsumeAnything()
    {
        ItemStack tagged = new ItemStack(Items.IRON_INGOT);
        tagged.getOrCreateTag().putInt("Paint", 2);
        SimpleContainer inventory = new SimpleContainer(tagged, new ItemStack(Items.GOLD_INGOT));
        ItemStack ingredient = new ItemStack(Items.IRON_INGOT);

        assertFalse(InventoryHelper.canConsumeAll(inventory, List.of(ingredient, ingredient)));
        assertFalse(InventoryHelper.tryConsumeAll(inventory, List.of(ingredient, ingredient), false));
        assertEquals(1, inventory.getItem(0).getCount());
        assertEquals(2, inventory.getItem(0).getOrCreateTag().getInt("Paint"));
        assertEquals(1, inventory.getItem(1).getCount());
    }

    @Test
    void workspaceAndInventoryPayDuplicateDyesTogetherOrRemainUntouched()
    {
        ItemStack dye = new ItemStack(Items.RED_DYE);
        ItemStack canSlot = dye.copy();
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.RED_DYE));
        assertFalse(InventoryHelper.tryConsumeAll(inventory, canSlot, List.of(dye, dye, dye), false));
        assertEquals(1, canSlot.getCount());
        assertEquals(1, inventory.getItem(0).getCount());
        assertTrue(InventoryHelper.tryConsumeAll(inventory, canSlot, List.of(dye, dye), false));
        assertTrue(canSlot.isEmpty());
        assertTrue(inventory.getItem(0).isEmpty());
    }

    @Test
    void workspaceKeepsItsExactDataRuleAndCreativeDoesNotPay()
    {
        ItemStack dye = new ItemStack(Items.RED_DYE);
        ItemStack tagged = dye.copy();
        ItemStackData.update(tagged, tag -> tag.putInt("Paint", 1));
        SimpleContainer inventory = new SimpleContainer(1);
        assertFalse(InventoryHelper.tryConsumeAll(inventory, tagged, List.of(dye), false));
        assertEquals(1, tagged.getCount());
        inventory.setItem(0, tagged.copy());
        assertTrue(InventoryHelper.tryConsumeAll(inventory, tagged, List.of(dye, dye), true));
        assertEquals(1, tagged.getCount());
        assertEquals(1, inventory.getItem(0).getCount());
        assertTrue(InventoryHelper.tryConsumeAll(inventory, tagged, List.of(dye), false));
        assertEquals(1, tagged.getCount());
        assertTrue(inventory.getItem(0).isEmpty());
    }

    @Test
    void mergingStillKeepsDifferentStackDataSeparate()
    {
        ItemStack painted = new ItemStack(Items.IRON_INGOT);
        painted.getOrCreateTag().putInt("Paint", 1);
        SimpleContainer inventory = new SimpleContainer(2);
        inventory.setItem(0, painted);
        ItemStack plain = new ItemStack(Items.IRON_INGOT);

        assertTrue(InventoryHelper.addItemStackToContainer(inventory, plain, false, true, true, 2));
        assertEquals(1, inventory.getItem(0).getCount());
        assertEquals(1, inventory.getItem(0).getOrCreateTag().getInt("Paint"));
        assertEquals(1, inventory.getItem(1).getCount());
        assertNull(inventory.getItem(1).getTag());
    }
}
