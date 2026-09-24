package com.flansmodultimate.network.server;

import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketRequestDismount implements IServerPacket
{
    @Override
    public void encodeInto(PacketBuffer data)
    {
        // No data
    }

    @Override
    public void decodeInto(PacketBuffer data)
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
