package com.flansmodultimate.common.block;

import com.flansmodultimate.common.types.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BoxPurchaseAccountingTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    // Plain JUnit must open the block registry that NeoForge's loader normally fills; NeoForge keeps its intrusive holders.
    @SuppressWarnings("deprecation")
    void gunAndArmorBoxesDoNotAwardOutputsUntilTheWholeDuplicateRecipeIsPaid()
    {
        for (boolean armor : new boolean[]{false, true})
        {
            ServerPlayer player = mock(ServerPlayer.class);
            Inventory inventory = new Inventory(player);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getAbilities()).thenReturn(new Abilities());
            ItemStack ingredient = new ItemStack(Items.IRON_INGOT);
            List<ItemStack> recipe = List.of(ingredient, ingredient);
            ArmorType outputType = mock(ArmorType.class);
            GunBoxType gunType = mock(GunBoxType.class);
            GunBoxType.GunBoxEntry gunEntry = mock(GunBoxType.GunBoxEntry.class);
            when(gunType.findEntry(outputType)).thenReturn(gunEntry);
            when(gunEntry.getType()).thenReturn(outputType);
            when(gunEntry.getRequiredParts()).thenReturn(recipe);
            ArmorBoxType armorType = mock(ArmorBoxType.class);
            ArmorBoxType.ArmourBoxEntry armorEntry = mock(ArmorBoxType.ArmourBoxEntry.class);
            when(armorType.getPages()).thenReturn(List.of(armorEntry));
            when(armorEntry.getArmorType(0)).thenReturn(outputType);
            when(armorEntry.getRequiredStacks()).thenReturn(List.of(recipe));
            MappedRegistry<Block> registry = (MappedRegistry<Block>) BuiltInRegistries.BLOCK;
            registry.unfreeze();
            GunBoxBlock gunBox;
            ArmorBoxBlock armorBox;
            try
            {
                gunBox = new GunBoxBlock(gunType);
                armorBox = new ArmorBoxBlock(armorType);
                Registry.register(registry, ResourceLocation.fromNamespaceAndPath("flansmodultimate", "test_gun_box_" + armor), gunBox);
                Registry.register(registry, ResourceLocation.fromNamespaceAndPath("flansmodultimate", "test_armor_box_" + armor), armorBox);
            }
            finally
            {
                registry.freeze();
            }
            try (var utils = mockStatic(ModUtils.class))
            {
                utils.when(() -> ModUtils.getItemStack(outputType)).thenAnswer(call -> Optional.of(new ItemStack(Items.STICK)));
                inventory.setItem(0, ingredient.copy());
                purchase(armor, player, outputType, gunBox, armorBox);
                assertEquals(1, InventoryHelper.countInInventory(inventory, ingredient));
                assertEquals(0, InventoryHelper.countInInventory(inventory, new ItemStack(Items.STICK)));
                inventory.getItem(0).grow(1);
                purchase(armor, player, outputType, gunBox, armorBox);
                assertEquals(0, InventoryHelper.countInInventory(inventory, ingredient));
                assertEquals(1, InventoryHelper.countInInventory(inventory, new ItemStack(Items.STICK)));
            }
        }
    }

    private static void purchase(boolean armor, ServerPlayer player, ArmorType output, GunBoxBlock gunBox, ArmorBoxBlock armorBox)
    {
        if (armor)
            armorBox.buyArmorServer(0, 0, player);
        else
            gunBox.buyGunServer(output, player);
    }
}
