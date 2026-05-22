package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
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
public class PacketGunShootClient implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketGunShootClient> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_shoot_client"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunShootClient> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunShootClient decode(FriendlyByteBuf buf)
        {
            PacketGunShootClient packet = new PacketGunShootClient();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunShootClient packet)
        {
            packet.encodeInto(buf);
        }
    };

    private UUID playerUUID;
    private InteractionHand hand;
    private boolean isShooting;

    public PacketGunShootClient(UUID playerUUID, InteractionHand hand, boolean isShooting)
    {
        this.playerUUID = playerUUID;
        this.hand = hand;
        this.isShooting = isShooting;
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
        data.writeBoolean(isShooting);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
        isShooting = data.readBoolean();
    }

    @Override
    public void handleClientSide()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        Player shootingPlayer = level.getPlayerByUUID(playerUUID);
        if (shootingPlayer != null)
        {
            PlayerData data = PlayerData.getInstance(shootingPlayer, net.neoforged.fml.LogicalSide.CLIENT);
            data.setShooting(hand, isShooting);
        }
    }
}