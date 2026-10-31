package com.wolffsmod.npcs.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Server to client: the Custom NPCs factions friendly to the player, whose NPCs show the player name tags meant for allies only. */
public record AllyFactionsPacket(List<Integer> factions)
{
    private static final int MAX_FACTIONS = 4096;

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(factions.size());
        factions.forEach(buffer::writeVarInt);
    }

    public static AllyFactionsPacket read(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_FACTIONS)
            throw new IllegalArgumentException("Too many factions: " + count);
        List<Integer> factions = new ArrayList<>(count);
        for (int index = 0; index < count; index++)
            factions.add(buffer.readVarInt());
        return new AllyFactionsPacket(List.copyOf(factions));
    }
}
