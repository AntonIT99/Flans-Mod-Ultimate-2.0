package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

@NoArgsConstructor
public class PacketFlashBang implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketFlashBang> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "flash_bang"));

    public static final StreamCodec<FriendlyByteBuf, PacketFlashBang> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketFlashBang decode(FriendlyByteBuf buf)
        {
            PacketFlashBang packet = new PacketFlashBang();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketFlashBang packet)
        {
            packet.encodeInto(buf);
        }
    };

    private int time = 10;

    public PacketFlashBang(int flashTime)
    {
        time = flashTime;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeInt(time);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        time = data.readInt();
    }

    @Override
    public void handleClientSide()
    {
        ClientHooks.RENDER.updateFlash(true, time);
    }
}