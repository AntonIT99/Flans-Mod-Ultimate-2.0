package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.common.teams.PlayerClassSkin;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

/** Tells clients which player class every player wears, so class skin overrides can be drawn. */
@NoArgsConstructor
public class PacketPlayerClassSkins implements IClientPacket
{
    private Map<UUID, PlayerClassSkin> playerClasses = Map.of();

    public PacketPlayerClassSkins(Map<UUID, PlayerClassSkin> playerClasses)
    {
        this.playerClasses = playerClasses;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(playerClasses.size());
        playerClasses.forEach((uuid, playerClass) ->
        {
            data.writeUUID(uuid);
            data.writeUtf(playerClass.playerClass());
            data.writeVarInt(playerClass.variant());
            data.writeUtf(playerClass.texture(), 256);
        });
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        int count = data.readVarInt();
        Map<UUID, PlayerClassSkin> decoded = new HashMap<>(count);
        for (int i = 0; i < count; i++)
            decoded.put(data.readUUID(), new PlayerClassSkin(data.readUtf(), data.readVarInt(), data.readUtf(256)));
        playerClasses = decoded;
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.updatePlayerClassSkins(playerClasses);
    }
}
