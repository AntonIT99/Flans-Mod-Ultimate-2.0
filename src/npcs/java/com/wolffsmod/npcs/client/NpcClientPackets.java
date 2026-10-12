package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.ServerControlPacket;
import com.wolffsmod.npcs.network.VehicleStatePacket;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.client.Minecraft;

/** Client handlers of the module's own packets. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcClientPackets
{
    public static void vehicleState(VehicleStatePacket packet)
    {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof EntityNPCInterface npc)
            NpcVehicleState.of(npc).receive(packet);
    }

    public static void serverSettings(ServerControlPacket packet)
    {
        ClientServerSettings.receive(packet);
        if (Minecraft.getInstance().screen instanceof GuiWolffsMod tab)
            tab.init();
    }
}
