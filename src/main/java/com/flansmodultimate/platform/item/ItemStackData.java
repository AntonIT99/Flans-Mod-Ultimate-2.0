package com.flansmodultimate.platform.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
        Tag encoded = ItemStack.OPTIONAL_CODEC
            .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
            .getOrThrow();
        return encoded instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
    }

    public static void save(ItemStack stack, HolderLookup.Provider registries, CompoundTag target)
    {
        target.merge(save(stack, registries));
    }

    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag)
    {
        if (tag.isEmpty())
            return ItemStack.EMPTY;
        return ItemStack.OPTIONAL_CODEC
            .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
            .result()
            .orElse(ItemStack.EMPTY);
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
