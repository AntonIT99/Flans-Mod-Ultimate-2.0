package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.diag.NpcInspection;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The Diagnosis page of the Wolff's Mod tab talking to the server: the client asks about the NPC it edits, once a second
 * while the page is open, and the server answers with the values the NPC acts on and what it did since the last request.
 *
 * @param request
 *            true from the client, which carries no lines
 */
public record NpcInspectPacket(int entityId, boolean request, List<Component> effective, List<Component> live)
{
    private static final int MAX_LINES = 32;

    public static NpcInspectPacket request(int entityId)
    {
        return new NpcInspectPacket(entityId, true, List.of(), List.of());
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(request);
        writeLines(buffer, effective);
        writeLines(buffer, live);
    }

    public static NpcInspectPacket read(FriendlyByteBuf buffer)
    {
        return new NpcInspectPacket(buffer.readVarInt(), buffer.readBoolean(), readLines(buffer), readLines(buffer));
    }

    private static void writeLines(FriendlyByteBuf buffer, List<Component> lines)
    {
        buffer.writeVarInt(lines.size());
        lines.forEach(buffer::writeComponent);
    }

    private static List<Component> readLines(FriendlyByteBuf buffer)
    {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_LINES)
            throw new IllegalArgumentException("Too many diagnosis lines: " + count);
        List<Component> lines = new ArrayList<>(count);
        for (int index = 0; index < count; index++)
            lines.add(buffer.readComponent());
        return List.copyOf(lines);
    }

    /** Answers a request about the NPC the player is editing, which Custom NPCs only records for a player allowed to open its editor. */
    public void handle(ServerPlayer player)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        if (!request || npc == null || npc.getId() != entityId || !npc.isAlive() || npc.level() != player.level())
            return;
        NpcNetwork.sendTo(player, new NpcInspectPacket(entityId, false, NpcInspection.effective(npc, player.server), NpcInspection.live(npc, player.server)));
    }
}
