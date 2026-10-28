package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.crew.NpcCrewHolder;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * A player in a gunner's seat of a vehicle NPC pressing or letting go of the attack button. The server only fires for a
 * player who still mans a gun of the vehicle they ride.
 *
 * @param held
 *            whether the button is held
 */
public record GunnerTriggerPacket(boolean held)
{
    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(held);
    }

    public static GunnerTriggerPacket read(FriendlyByteBuf buffer)
    {
        return new GunnerTriggerPacket(buffer.readBoolean());
    }

    public void handle(ServerPlayer player)
    {
        if (player.getVehicle() instanceof EntityNPCInterface vehicle)
            ((NpcCrewHolder) vehicle).wolffsmodnpcsGunners().trigger(player, held);
    }
}
