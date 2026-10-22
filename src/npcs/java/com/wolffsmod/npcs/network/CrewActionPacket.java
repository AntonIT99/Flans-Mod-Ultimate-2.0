package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.crew.NpcCrewRequests;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Crew tab's buttons: a client asks the server to crew the empty seats of the vehicle NPC it is editing, or to remove
 * its crew. The crew settings travel with the request, since the tab only saves them with the stats menu when it closes.
 *
 * @param entityId
 *            the NPC being edited, so a request that arrives for another NPC is refused
 * @param settings
 *            the crew settings as edited
 */
public record CrewActionPacket(int entityId, Action action, CompoundTag settings)
{
    /** What the builder asks for. */
    public enum Action
    {
        CREW, REMOVE
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(action);
        buffer.writeNbt(settings);
    }

    public static CrewActionPacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        Action action = buffer.readEnum(Action.class);
        CompoundTag settings = buffer.readNbt();
        return new CrewActionPacket(entityId, action, settings == null ? new CompoundTag() : settings);
    }

    /** Checks that the player may edit the NPC, then crews it or removes its crew. */
    public void handle(ServerPlayer player)
    {
        NpcCrewRequests.request(player, entityId, action == Action.CREW, settings);
    }
}
