package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketAllowDebug;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketRequestDebug implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketRequestDebug> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "request_debug"));

    public static final StreamCodec<FriendlyByteBuf, PacketRequestDebug> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketRequestDebug decode(FriendlyByteBuf buf)
        {
            PacketRequestDebug packet = new PacketRequestDebug();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketRequestDebug packet)
        {
            packet.encodeInto(buf);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        if (player.hasPermissions(2))
            PacketHandler.sendTo(new PacketAllowDebug(), player);
    }
}