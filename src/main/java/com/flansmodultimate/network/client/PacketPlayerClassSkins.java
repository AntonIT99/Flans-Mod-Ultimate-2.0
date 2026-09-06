package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tells clients which player class every player wears, so class skin overrides can be drawn. */
@NoArgsConstructor
public class PacketPlayerClassSkins implements IClientPacket
{
    private Map<UUID, String> playerClasses = Map.of();

    public PacketPlayerClassSkins(Map<UUID, String> playerClasses)
    {
        this.playerClasses = playerClasses;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeVarInt(playerClasses.size());
        playerClasses.forEach((uuid, playerClass) -> {
            data.writeUUID(uuid);
            data.writeUtf(playerClass);
        });
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        int count = data.readVarInt();
        Map<UUID, String> decoded = new HashMap<>(count);
        for (int i = 0; i < count; i++)
            decoded.put(data.readUUID(), data.readUtf());
        playerClasses = decoded;
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.updatePlayerClassSkins(playerClasses);
    }
}
