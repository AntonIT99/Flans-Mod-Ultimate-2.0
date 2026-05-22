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
public class PacketGunReloadClient implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketGunReloadClient> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_reload_client"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunReloadClient> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunReloadClient decode(FriendlyByteBuf buf)
        {
            PacketGunReloadClient packet = new PacketGunReloadClient();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunReloadClient packet)
        {
            packet.encodeInto(buf);
        }
    };

    private UUID playerUUID;
    private InteractionHand hand;
    private float reloadTime;
    private int reloadCount;
    private boolean hasMultipleAmmo;

    public PacketGunReloadClient(UUID playerUUID, InteractionHand hand, float reloadTime, int reloadCount, boolean hasMultipleAmmo)
    {
        this.playerUUID = playerUUID;
        this.hand = hand;
        this.reloadTime = reloadTime;
        this.reloadCount = reloadCount;
        this.hasMultipleAmmo = hasMultipleAmmo;
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
        data.writeFloat(reloadTime);
        data.writeInt(reloadCount);
        data.writeBoolean(hasMultipleAmmo);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
        reloadTime = data.readFloat();
        reloadCount = data.readInt();
        hasMultipleAmmo = data.readBoolean();
    }

    @Override
    public void handleClientSide()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        Player reloadingPlayer = level.getPlayerByUUID(playerUUID);
        if (reloadingPlayer != null && reloadingPlayer.getItemInHand(hand).getItem() instanceof GunItem gunItem)
        {
            ClientHooks.GUN.reloadGunItem(gunItem, reloadingPlayer, hand, reloadTime, reloadCount, hasMultipleAmmo);
        }
    }
}