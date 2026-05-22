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
public class PacketHitMarker implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketHitMarker> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "hit_marker"));

    public static final StreamCodec<FriendlyByteBuf, PacketHitMarker> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketHitMarker decode(FriendlyByteBuf buf)
        {
            PacketHitMarker packet = new PacketHitMarker();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketHitMarker packet)
        {
            packet.encodeInto(buf);
        }
    };

    private float penAmount = 1F;
    private boolean headshot = false;
    private boolean explosionHit = false;

    public PacketHitMarker(boolean head, float pen, boolean explosion)
    {
        headshot = head;
        penAmount = pen;
        explosionHit = explosion;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeBoolean(headshot);
        data.writeFloat(penAmount);
        data.writeBoolean(explosionHit);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        headshot = data.readBoolean();
        penAmount = data.readFloat();
        explosionHit = data.readBoolean();
    }

    @Override
    public void handleClientSide()
    {
        ClientHooks.RENDER.updateHitMarker(20, penAmount, headshot, explosionHit);
    }
}