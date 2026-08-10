package com.flansmodultimate.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Stable packet helpers for payload values whose Minecraft codec changes between versions. */
public final class PacketIO
{
    private PacketIO() {}

    public static void writeItem(RegistryFriendlyByteBuf buffer, ItemStack stack)
    {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
    }

    public static ItemStack readItem(RegistryFriendlyByteBuf buffer)
    {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    public static void writeItems(RegistryFriendlyByteBuf buffer, List<ItemStack> stacks)
    {
        buffer.writeVarInt(stacks.size());
        stacks.forEach(stack -> writeItem(buffer, stack));
    }

    public static List<ItemStack> readItems(RegistryFriendlyByteBuf buffer)
    {
        int size = buffer.readVarInt();
        List<ItemStack> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(readItem(buffer));
        return result;
    }
}
