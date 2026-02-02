package com.flansmodultimate.network.client;

import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketSyncCommonConfig implements IClientPacket
{
    private CommonConfigSnapshot snapshot;

    public PacketSyncCommonConfig(CommonConfigSnapshot snapshot)
    {
        this.snapshot = snapshot;
    }

    @Override
    public void encodeInto(FriendlyByteBuf buf)
    {
        CommonConfigSnapshot.write(buf, snapshot);
    }

    @Override
    public void decodeInto(FriendlyByteBuf buf)
    {
        snapshot = CommonConfigSnapshot.read(buf);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ModCommonConfig.applyServerSnapshot(snapshot);
    }
}