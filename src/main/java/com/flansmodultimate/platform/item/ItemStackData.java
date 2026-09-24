package com.flansmodultimate.platform.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

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
        CompoundTag tag = stack.getTag();
        return tag != null && !tag.isEmpty();
    }

    /** Returns a detached copy which can safely be inspected or edited. */
    public static CompoundTag copy(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
    }

    public static void set(ItemStack stack, CompoundTag tag)
    {
        stack.setTag(tag.isEmpty() ? null : tag.copy());
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> update)
    {
        CompoundTag tag = copy(stack);
        update.accept(tag);
        set(stack, tag);
    }

    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries)
    {
        return stack.save(new CompoundTag());
    }

    public static void save(ItemStack stack, HolderLookup.Provider registries, CompoundTag target)
    {
        stack.save(target);
    }

    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag)
    {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(tag);
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
        return ItemStack.isSameItemSameTags(first, second);
    }

    /** Damages the stack, breaking it with the usual effects when it runs out of durability. */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot)
    {
        stack.hurtAndBreak(amount, entity, owner -> owner.broadcastBreakEvent(slot));
    }
}
