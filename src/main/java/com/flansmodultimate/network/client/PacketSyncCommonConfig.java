package com.flansmodultimate.network.client;

import com.flansmodultimate.config.ApocalypseConfigSnapshot;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketSyncCommonConfig implements IClientPacket
{
    private CommonConfigSnapshot commonSnapshot;
    private ApocalypseConfigSnapshot apocalypseSnapshot;

    public PacketSyncCommonConfig(CommonConfigSnapshot commonSnapshot, ApocalypseConfigSnapshot apocalypseSnapshot)
    {
        this.commonSnapshot = commonSnapshot;
        this.apocalypseSnapshot = apocalypseSnapshot;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf buf)
    {
        CommonConfigSnapshot.write(buf, commonSnapshot);
        ApocalypseConfigSnapshot.write(buf, apocalypseSnapshot);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf buf)
    {
        commonSnapshot = CommonConfigSnapshot.read(buf);
        apocalypseSnapshot = ApocalypseConfigSnapshot.read(buf);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ModCommonConfig.applyServerSnapshot(commonSnapshot);
        ModApocalypseConfig.applyServerSnapshot(apocalypseSnapshot);
    }
}
