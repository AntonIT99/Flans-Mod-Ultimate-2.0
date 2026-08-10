package com.flansmodultimate.platform.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Version boundary for custom item data and item-stack serialization.
 * Business code should not depend directly on Minecraft's data-component API.
 */
public final class ItemStackData
{
    private ItemStackData()
    {
    }

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
}
