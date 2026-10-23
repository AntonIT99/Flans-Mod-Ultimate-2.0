package com.wolffsmod.npcs.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Comparator;
import java.util.List;

/** A server snapshot for the wand browser, including NPCs outside the client's tracking range. */
public record NpcRemotePacket(List<Entry> entries)
{
    public record Entry(int entityId, String name, double distanceSquared, int factionId, String faction)
    {}

    public NpcRemotePacket
    {
        entries = List.copyOf(entries);
    }

    /** Zero radius and a null faction select all loaded NPCs in the player's dimension. */
    public List<Entry> filtered(int radius, Integer faction)
    {
        double limit = radius <= 0 ? Double.POSITIVE_INFINITY : (double) radius * radius;
        return entries.stream().filter(entry -> entry.distanceSquared() <= limit && (faction == null || entry.factionId() == faction))
            .sorted(Comparator.comparingDouble(Entry::distanceSquared).thenComparingInt(Entry::entityId)).toList();
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeCollection(entries, (out, entry) ->
        {
            out.writeVarInt(entry.entityId());
            out.writeUtf(entry.name());
            out.writeDouble(entry.distanceSquared());
            out.writeVarInt(entry.factionId());
            out.writeUtf(entry.faction());
        });
    }

    public static NpcRemotePacket read(FriendlyByteBuf buffer)
    {
        return new NpcRemotePacket(buffer.readList(in -> new Entry(in.readVarInt(), in.readUtf(), in.readDouble(), in.readVarInt(), in.readUtf())));
    }
}
