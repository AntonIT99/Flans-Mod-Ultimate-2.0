package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CraftingPartsCommandTest
{
    @BeforeAll
    static void bootstrapRegistries()
    {
        // NeoForge loads optional feature flags from its discovery list during vanilla bootstrap.
        if (LoadingModList.get() == null)
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void shapedEmptySlotsAreSkippedAndRepeatedSlotsAreKept()
    {
        Ingredient iron = Ingredient.of(Items.IRON_INGOT);
        var stacks = CraftingPartsCommand.resolveIngredients(List.of(iron, Ingredient.EMPTY, iron));
        assertEquals(2, stacks.size());
        assertTrue(stacks.stream().allMatch(stack -> stack.is(Items.IRON_INGOT) && stack.getCount() == 1));
        assertNotSame(stacks.get(0), stacks.get(1));
        assertNotSame(iron.getItems()[0], stacks.get(0));
    }

    @Test
    void alternativesGiveOneItemRatherThanEveryAlternative()
    {
        Ingredient wood = Ingredient.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS);
        var stacks = CraftingPartsCommand.resolveIngredients(List.of(wood));
        assertEquals(1, stacks.size());
        assertTrue(wood.test(stacks.get(0)));
        assertEquals(1, stacks.get(0).getCount());
    }

    @Test
    void onlyOperatorsCanUseCommand()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CraftingPartsCommand.register(dispatcher);
        CommandSourceStack source = new CommandSourceStack(net.minecraft.commands.CommandSource.NULL, net.minecraft.world.phys.Vec3.ZERO, net.minecraft.world.phys.Vec2.ZERO, null, 0, "test",
            net.minecraft.network.chat.Component.literal("test"), null, null);
        assertFalse(dispatcher.getRoot().getChild("craftingparts").canUse(source));
        assertTrue(dispatcher.getRoot().getChild("craftingparts").canUse(source.withPermission(2)));
    }
}
