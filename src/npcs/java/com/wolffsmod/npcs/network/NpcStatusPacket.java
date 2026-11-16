package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.diag.NpcStatus;
import com.wolffsmod.npcs.platform.network.NpcPacketIO;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The General tab of the Wolff's Mod tab asking about the NPC it edits, a few times a second while it is shown, and the
 * server answering with what the NPC is doing.
 *
 * @param status
 *            the answer; null in a request
 */
public record NpcStatusPacket(int entityId, @Nullable NpcStatus status)
{
    public static NpcStatusPacket request(int entityId)
    {
        return new NpcStatusPacket(entityId, null);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(status != null);
        if (status == null)
            return;
        buffer.writeEnum(status.activity());
        buffer.writeFloat(status.health());
        buffer.writeFloat(status.maxHealth());
        buffer.writeEnum(status.behaviour());
        writeName(buffer, status.target());
        buffer.writeFloat(status.targetDistance());
        buffer.writeBoolean(status.targetInSight());
        writeName(buffer, status.threat());
        buffer.writeBoolean(status.reloading());
        buffer.writeBoolean(status.outOfAmmunition());
        buffer.writeVarInt(status.crewAboard());
        buffer.writeVarInt(status.crewSeats());
        buffer.writeBoolean(status.repairing());
        buffer.writeVarInt(status.respawnSeconds());
        buffer.writeFloat(status.homeDistance());
        buffer.writeFloat(status.speed());
    }

    public static NpcStatusPacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        if (!buffer.readBoolean())
            return request(entityId);
        return new NpcStatusPacket(entityId,
            new NpcStatus(buffer.readEnum(NpcStatus.Activity.class), buffer.readFloat(), buffer.readFloat(), buffer.readEnum(NpcStatus.Behaviour.class), readName(buffer), buffer.readFloat(), buffer.readBoolean(),
                readName(buffer), buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readFloat(),
                buffer.readFloat()));
    }

    private static void writeName(FriendlyByteBuf buffer, @Nullable Component name)
    {
        buffer.writeBoolean(name != null);
        if (name != null)
            NpcPacketIO.writeComponent(buffer, name);
    }

    @Nullable
    private static Component readName(FriendlyByteBuf buffer)
    {
        return buffer.readBoolean() ? NpcPacketIO.readComponent(buffer) : null;
    }

    /**
     * Answers a request about the NPC the player is editing, which Custom NPCs only records for a player allowed to open its
     * editor. A killed NPC that waits to respawn still answers, with how long it waits.
     */
    public void handle(ServerPlayer player)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        if (status != null || npc == null || npc.getId() != entityId || npc.isRemoved() || npc.level() != player.level())
            return;
        NpcNetwork.sendTo(player, new NpcStatusPacket(entityId, NpcStatus.of(npc)));
    }
}
