package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

@NoArgsConstructor
public class PacketCancelGunReloadClient implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketCancelGunReloadClient> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "cancel_gun_reload_client"));

    public static final StreamCodec<FriendlyByteBuf, PacketCancelGunReloadClient> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketCancelGunReloadClient decode(FriendlyByteBuf buf)
        {
            PacketCancelGunReloadClient packet = new PacketCancelGunReloadClient();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketCancelGunReloadClient packet)
        {
            packet.encodeInto(buf);
        }
    };

    private InteractionHand hand;

    public PacketCancelGunReloadClient(InteractionHand hand)
    {
        this.hand = hand;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleClientSide()
    {
        Player player = Minecraft.getInstance().player;
        if (player != null)
            ClientHooks.GUN.cancelReloadGunItem(player, hand);
    }
}