package com.flansmodultimate.platform.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Version boundary for custom item data and item-stack serialization.
 * Business code should not depend directly on Minecraft's item NBT or data-component API.
 */
public final class ItemStackData
{
    private ItemStackData() {}

    public static boolean has(ItemStack stack)
    {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    /** Returns a detached copy which can safely be inspected or edited. */
    public static CompoundTag copy(ItemStack stack)
    {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void set(ItemStack stack, CompoundTag tag)
    {
        if (tag.isEmpty())
            stack.remove(DataComponents.CUSTOM_DATA);
        else
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> update)
    {
        CompoundTag tag = copy(stack);
        update.accept(tag);
        set(stack, tag);
    }

    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries)
    {
        return (CompoundTag) stack.save(registries, new CompoundTag());
    }

    public static void save(ItemStack stack, HolderLookup.Provider registries, CompoundTag target)
    {
        stack.save(registries, target);
    }

    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag)
    {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, tag);
    }

    /** For stacks whose components only reference static built-in registries. */
    public static ItemStack parseBuiltIn(CompoundTag tag)
    {
        return parse(builtInRegistries(), tag);
    }

    /** Registry view for content-pack stacks that only use vanilla/static registry entries. */
    public static HolderLookup.Provider builtInRegistries()
    {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    public static CompoundTag saveBuiltIn(ItemStack stack)
    {
        return save(stack, builtInRegistries());
    }

    /** Whether both stacks hold the same item with the same custom data, ignoring count. */
    public static boolean isSameItemSameData(ItemStack first, ItemStack second)
    {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    /** Damages the stack, breaking it with the usual effects when it runs out of durability. */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot)
    {
        stack.hurtAndBreak(amount, entity, slot);
    }

    /** A stack of the item, such as a potion or splash potion, holding the given potion. */
    public static ItemStack potion(Item item, Holder<Potion> potion)
    {
        return PotionContents.createItemStack(item, potion);
    }

    /** A signed written book with one page per component. */
    public static ItemStack writtenBook(String title, String author, List<Component> pages)
    {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, 0,
            pages.stream().map(Filterable::passThrough).toList(), true));
        return book;
    }
}
