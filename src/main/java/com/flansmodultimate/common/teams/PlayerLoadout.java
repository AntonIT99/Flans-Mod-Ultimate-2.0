package com.flansmodultimate.common.teams;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/** A compact, deep-copying value object for one custom ranked loadout. */
public final class PlayerLoadout
{
    private static final String NBT_PRIMARY = "primary";
    private static final String NBT_SECONDARY = "secondary";
    private static final String NBT_SPECIAL = "special";
    private static final String NBT_MELEE = "melee";
    private static final String NBT_ARMOUR = "armour";

    private final ItemStack[] slots = new ItemStack[LoadoutSlot.values().length];

    public PlayerLoadout() { Arrays.fill(slots, ItemStack.EMPTY); }

    public ItemStack get(LoadoutSlot slot) { return slots[slot.ordinal()]; }

    public void set(LoadoutSlot slot, ItemStack stack)
    {
        slots[slot.ordinal()] = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public List<ItemStack> getSlots()
    {
        return Arrays.stream(slots).map(ItemStack::copy).toList();
    }

    public PlayerLoadout copy()
    {
        PlayerLoadout result = new PlayerLoadout();
        for (LoadoutSlot slot : LoadoutSlot.values())
            result.set(slot, get(slot));
        return result;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        for (LoadoutSlot slot : LoadoutSlot.values())
            if (!get(slot).isEmpty()) tag.put(tagKey(slot), get(slot).save(new CompoundTag()));
        return tag;
    }

    public static PlayerLoadout load(CompoundTag tag)
    {
        PlayerLoadout result = new PlayerLoadout();
        for (LoadoutSlot slot : LoadoutSlot.values())
            if (tag.contains(tagKey(slot))) result.set(slot, ItemStack.of(tag.getCompound(tagKey(slot))));
        return result;
    }

    public void write(FriendlyByteBuf data)
    {
        for (LoadoutSlot slot : LoadoutSlot.values()) data.writeItem(get(slot));
    }

    public static PlayerLoadout read(FriendlyByteBuf data)
    {
        PlayerLoadout result = new PlayerLoadout();
        for (LoadoutSlot slot : LoadoutSlot.values()) result.set(slot, data.readItem());
        return result;
    }

    private static String tagKey(LoadoutSlot slot)
    {
        return switch (slot)
        {
            case PRIMARY -> NBT_PRIMARY;
            case SECONDARY -> NBT_SECONDARY;
            case SPECIAL -> NBT_SPECIAL;
            case MELEE -> NBT_MELEE;
            case ARMOUR -> NBT_ARMOUR;
        };
    }
}
