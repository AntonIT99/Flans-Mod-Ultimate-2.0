package com.flansmodultimate.network.client;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.ProneSupport;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Whether a player lies prone. Sent to the player and everyone who sees them whenever it changes, and to anyone who starts
 * seeing them, since every client works out a player's pose again each tick.
 */
@NoArgsConstructor
public class PacketProneState implements IClientPacket
{
    private UUID playerUUID;
    private boolean prone;

    public PacketProneState(UUID playerUUID, boolean prone)
    {
        this.playerUUID = playerUUID;
        this.prone = prone;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(playerUUID);
        data.writeBoolean(prone);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        playerUUID = data.readUUID();
        prone = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        PlayerData.getClientInstance(playerUUID).setProne(prone);
        Player target = level.getPlayerByUUID(playerUUID);
        if (target != null)
            ProneSupport.apply(target, prone);
    }
}
