package com.flansmodultimate.platform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Stable packet helpers for payload values whose Minecraft codec changes between versions. */
public final class PacketIO
{
    private PacketIO() {}

    public static void writeItem(FriendlyByteBuf buffer, ItemStack stack)
    {
        buffer.writeItem(stack);
    }

    public static ItemStack readItem(FriendlyByteBuf buffer)
    {
        return buffer.readItem();
    }

    public static void writeItems(FriendlyByteBuf buffer, List<ItemStack> stacks)
    {
        buffer.writeVarInt(stacks.size());
        stacks.forEach(stack -> writeItem(buffer, stack));
    }

    public static List<ItemStack> readItems(FriendlyByteBuf buffer)
    {
        int size = buffer.readVarInt();
        List<ItemStack> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(readItem(buffer));
        return result;
    }

    public static void writeComponent(FriendlyByteBuf buffer, Component component)
    {
        buffer.writeComponent(component);
    }

    public static Component readComponent(FriendlyByteBuf buffer)
    {
        return buffer.readComponent();
    }
}
