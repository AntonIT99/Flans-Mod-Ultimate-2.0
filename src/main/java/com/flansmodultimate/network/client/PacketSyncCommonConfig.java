package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

@NoArgsConstructor
public class PacketSyncCommonConfig implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketSyncCommonConfig> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "sync_common_config"));

    public static final StreamCodec<FriendlyByteBuf, PacketSyncCommonConfig> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketSyncCommonConfig decode(FriendlyByteBuf buf)
        {
            PacketSyncCommonConfig packet = new PacketSyncCommonConfig();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketSyncCommonConfig packet)
        {
            packet.encodeInto(buf);
        }
    };

    private CommonConfigSnapshot snapshot;

    public PacketSyncCommonConfig(CommonConfigSnapshot snapshot)
    {
        this.snapshot = snapshot;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
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
    public void handleClientSide()
    {
        ModCommonConfig.applyServerSnapshot(snapshot);
    }
}