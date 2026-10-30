package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.crew.NpcSeatInteraction;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/** Requests a normal NPC interaction at a seat; the server independently traces reach, sight and occupancy. */
public record BoardSeatPacket(int vehicleId, InteractionHand hand)
{
    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(vehicleId);
        buffer.writeEnum(hand);
    }

    public static BoardSeatPacket read(FriendlyByteBuf buffer)
    {
        return new BoardSeatPacket(buffer.readVarInt(), buffer.readEnum(InteractionHand.class));
    }

    public void handle(ServerPlayer player)
    {
        if (!player.isAlive() || player.isSpectator() || player.isSecondaryUseActive() || !(player.level().getEntity(vehicleId) instanceof EntityNPCInterface vehicle) || !vehicle.isAlive()
            || vehicle.isKilled() || player.getVehicle() == vehicle)
            return;
        if (NpcSeatInteraction.pick(vehicle, player).filter(hit -> !NpcCrew.occupied(vehicle, hit.mount().seat())).isPresent())
            player.interactOn(vehicle, hand);
    }
}
