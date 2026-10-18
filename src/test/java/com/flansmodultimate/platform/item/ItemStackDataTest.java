package com.flansmodultimate.platform.item;

import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackDataTest
{
    @BeforeAll
    @SuppressWarnings("UnstableApiUsage") // Vanilla bootstrap needs NeoForge's discovery list outside a running loader.
    static void bootstrapRegistries()
    {
        // NeoForge loads optional feature flags from its discovery list during vanilla bootstrap.
        if (LoadingModList.get() == null)
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void savesAttachmentStackIntoNestedGunTag()
    {
        ItemStack attachment = new ItemStack(Items.SPYGLASS);
        attachment.set(DataComponents.CUSTOM_NAME, Component.literal("Scope"));
        ItemStackData.update(attachment, tag -> tag.putInt("Paint", 2));
        CompoundTag attachmentTag = new CompoundTag();

        ItemStackData.save(attachment, ItemStackData.builtInRegistries(), attachmentTag);
        CompoundTag attachments = new CompoundTag();
        attachments.put("scope", attachmentTag);
        CompoundTag gunTag = new CompoundTag();
        gunTag.put("attachments", attachments);

        ItemStack restored = ItemStackData.parseBuiltIn(gunTag.getCompound("attachments").getCompound("scope"));
        assertFalse(restored.isEmpty());
        assertTrue(ItemStack.isSameItemSameComponents(attachment, restored));
        assertEquals(attachment.getCount(), restored.getCount());
    }

    @Test
    void preservesInventorySlotMetadataAndStackCount()
    {
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 7);
        CompoundTag entry = new CompoundTag();
        entry.putInt("slot", 3);

        ItemStackData.save(stack, ItemStackData.builtInRegistries(), entry);

        assertEquals(3, entry.getInt("slot"));
        ItemStack restored = ItemStackData.parseBuiltIn(entry);
        assertTrue(ItemStack.isSameItemSameComponents(stack, restored));
        assertEquals(7, restored.getCount());
        assertEquals(7, stack.getCount());
    }

    @Test
    void replacesExistingSerializedComponents()
    {
        ItemStack previous = new ItemStack(Items.IRON_INGOT);
        ItemStackData.update(previous, tag -> tag.putBoolean("old", true));
        CompoundTag entry = ItemStackData.saveBuiltIn(previous);
        entry.putInt("slot", 3);
        ItemStack replacement = new ItemStack(Items.GOLD_INGOT, 2);
        ItemStackData.update(replacement, tag -> tag.putBoolean("new", true));

        ItemStackData.save(replacement, ItemStackData.builtInRegistries(), entry);

        assertEquals(3, entry.getInt("slot"));
        ItemStack restored = ItemStackData.parseBuiltIn(entry);
        assertTrue(ItemStack.isSameItemSameComponents(replacement, restored));
        assertEquals(2, restored.getCount());
        assertFalse(ItemStackData.copy(restored).contains("old"));
    }
}
