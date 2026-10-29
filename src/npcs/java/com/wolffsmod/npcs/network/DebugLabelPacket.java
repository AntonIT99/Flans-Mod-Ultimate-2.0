package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.platform.network.NpcPacketIO;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the lines the aim debug switch shows above an NPC. A label the server stops sending fades after a
 * moment.
 */
public record DebugLabelPacket(int entityId, List<Component> lines)
{
    private static final int MAX_LINES = 16;

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(lines.size());
        lines.forEach(line -> NpcPacketIO.writeComponent(buffer, line));
    }

    public static DebugLabelPacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_LINES)
            throw new IllegalArgumentException("Too many debug label lines: " + count);
        List<Component> lines = new ArrayList<>(count);
        for (int index = 0; index < count; index++)
            lines.add(NpcPacketIO.readComponent(buffer));
        return new DebugLabelPacket(entityId, List.copyOf(lines));
    }
}
