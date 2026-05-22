package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class PacketAllowDebug implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketAllowDebug> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "allow_debug"));

    public static final StreamCodec<FriendlyByteBuf, PacketAllowDebug> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketAllowDebug decode(FriendlyByteBuf buf)
        {
            PacketAllowDebug packet = new PacketAllowDebug();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketAllowDebug packet)
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
    public void handleClientSide()
    {
        ClientHooks.RENDER.setDebugMode(true);
    }
}