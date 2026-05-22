package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

@NoArgsConstructor
public class PacketGunMeleeClient implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketGunMeleeClient> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_melee_client"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunMeleeClient> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunMeleeClient decode(FriendlyByteBuf buf)
        {
            PacketGunMeleeClient packet = new PacketGunMeleeClient();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunMeleeClient packet)
        {
            packet.encodeInto(buf);
        }
    };

    private UUID playerUUID;
    private InteractionHand hand;

    public PacketGunMeleeClient(UUID playerUUID, InteractionHand hand)
    {
        this.playerUUID = playerUUID;
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
        data.writeUUID(playerUUID);
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleClientSide()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        Player meleePlayer = level.getPlayerByUUID(playerUUID);
        if (meleePlayer != null && meleePlayer.getItemInHand(hand).getItem() instanceof GunItem gunItem)
        {
            ClientHooks.GUN.meleeGunItem(gunItem, meleePlayer, hand);
        }
    }
}