package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.crew.NpcCrew;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** A player riding a vehicle NPC pressing the seat key, to move to its next free seat. */
// The network registration hands every packet a buffer, which this one has nothing to write to or read from.
@SuppressWarnings("java:S1172")
public record ChangeSeatPacket()
{
    public void write(FriendlyByteBuf buffer)
    {
        // Pressing the key is all there is to say.
    }

    public static ChangeSeatPacket read(FriendlyByteBuf buffer)
    {
        return new ChangeSeatPacket();
    }

    public void handle(ServerPlayer player)
    {
        if (player.getVehicle() instanceof EntityNPCInterface vehicle)
            NpcCrew.changeSeat(vehicle, player);
    }
}
