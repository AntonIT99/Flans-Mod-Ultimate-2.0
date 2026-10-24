package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.platform.network.NpcNetworkPlatform;
import com.wolffsmod.npcs.soldier.FlanSoldierNames;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** A picker naming request, the generated name returned by the server, and acknowledgement of that exact server result. */
public record SoldierNamePacket(int entityId, String team, String playerClass, boolean state, String nameList, String name)
{
    private static final int MAX_LENGTH = 256;

    public static SoldierNamePacket request(int entityId, FlanSoldierSelection selection)
    {
        return new SoldierNamePacket(entityId, selection.team() == null ? "" : selection.team().getShortName(), selection.playerClass().getShortName(), false, "", "");
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeUtf(team, MAX_LENGTH);
        buffer.writeUtf(playerClass, MAX_LENGTH);
        buffer.writeBoolean(state);
        buffer.writeUtf(nameList, MAX_LENGTH);
        buffer.writeUtf(name, MAX_LENGTH);
    }

    public static SoldierNamePacket read(FriendlyByteBuf buffer)
    {
        return new SoldierNamePacket(buffer.readVarInt(), buffer.readUtf(MAX_LENGTH), buffer.readUtf(MAX_LENGTH), buffer.readBoolean(), buffer.readUtf(MAX_LENGTH), buffer.readUtf(MAX_LENGTH));
    }

    public void handle(ServerPlayer player)
    {
        if (state)
            FlanSoldierNames.confirm(player, this);
        else
            FlanSoldierNames.request(player, entityId, team, playerClass).ifPresent(packet -> NpcNetworkPlatform.sendToPlayer(player, packet));
    }
}
