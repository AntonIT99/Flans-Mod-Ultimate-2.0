package com.flansmodultimate.common.teams;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.network.PacketIO;

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

    public CompoundTag save(HolderLookup.Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        for (LoadoutSlot slot : LoadoutSlot.values())
            if (!get(slot).isEmpty()) tag.put(tagKey(slot), ItemStackData.save(get(slot), registries));
        return tag;
    }

    public static PlayerLoadout load(CompoundTag tag, HolderLookup.Provider registries)
    {
        PlayerLoadout result = new PlayerLoadout();
        for (LoadoutSlot slot : LoadoutSlot.values())
            if (tag.contains(tagKey(slot))) result.set(slot, ItemStackData.parse(registries, tag.getCompoundOrEmpty(tagKey(slot))));
        return result;
    }

    public void write(RegistryFriendlyByteBuf data)
    {
        for (LoadoutSlot slot : LoadoutSlot.values()) PacketIO.writeItem(data, get(slot));
    }

    public static PlayerLoadout read(RegistryFriendlyByteBuf data)
    {
        PlayerLoadout result = new PlayerLoadout();
        for (LoadoutSlot slot : LoadoutSlot.values()) result.set(slot, PacketIO.readItem(data));
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
