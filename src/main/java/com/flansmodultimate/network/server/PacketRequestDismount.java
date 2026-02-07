package com.flansmodultimate.network.server;

import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketRequestDismount implements IServerPacket
{
    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        // No data
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        // No data
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (player.getVehicle() instanceof DeployedGun)
            player.stopRiding();
    }
}
