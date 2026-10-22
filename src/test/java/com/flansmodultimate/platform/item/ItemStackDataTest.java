package com.flansmodultimate.platform.item;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackDataTest
{
    @BeforeAll
    static void bootstrapRegistries() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void savesAttachmentStackIntoNestedGunTag()
    {
        ItemStack attachment = new ItemStack(Items.SPYGLASS);
        ItemStackData.update(attachment, tag -> tag.putInt("Paint", 2));
        CompoundTag attachmentTag = new CompoundTag();

        ItemStackData.save(attachment, ItemStackData.builtInRegistries(), attachmentTag);
        CompoundTag attachments = new CompoundTag();
        attachments.put("scope", attachmentTag);
        CompoundTag gunTag = new CompoundTag();
        gunTag.put("attachments", attachments);

        ItemStack restored = ItemStackData.parseBuiltIn(gunTag.getCompound("attachments").getCompound("scope"));
        assertFalse(restored.isEmpty());
        assertTrue(ItemStackData.isSameItemSameData(attachment, restored));
        assertEquals(2, ItemStackData.copy(restored).getInt("Paint"));
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
        assertTrue(ItemStackData.isSameItemSameData(stack, restored));
        assertEquals(7, restored.getCount());
        assertEquals(7, stack.getCount());
    }

    @Test
    void replacesExistingSerializedData()
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
        assertTrue(ItemStackData.isSameItemSameData(replacement, restored));
        assertEquals(2, restored.getCount());
        assertFalse(ItemStackData.copy(restored).contains("old"));
    }
}
