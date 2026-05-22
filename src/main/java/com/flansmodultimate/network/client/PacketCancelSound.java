package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

@NoArgsConstructor
public class PacketCancelSound implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketCancelSound> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "cancel_sound"));

    public static final StreamCodec<FriendlyByteBuf, PacketCancelSound> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketCancelSound decode(FriendlyByteBuf buf)
        {
            PacketCancelSound packet = new PacketCancelSound();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketCancelSound packet)
        {
            packet.encodeInto(buf);
        }
    };

    private UUID instanceUUID;

    public PacketCancelSound(UUID instanceUUID)
    {
        this.instanceUUID = instanceUUID;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUUID(instanceUUID);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        instanceUUID = data.readUUID();
    }

    @Override
    public void handleClientSide()
    {
        ClientHooks.SOUND.cancelSound(instanceUUID);
    }
}