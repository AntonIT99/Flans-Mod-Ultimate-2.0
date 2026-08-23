package com.flansmodultimate.network.server;

import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketAllowDebug;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

@NoArgsConstructor
public class PacketRequestDebug implements IServerPacket
{
    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        // No data
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        // No data
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            PacketHandler.sendTo(new PacketAllowDebug(), player);
    }
}
