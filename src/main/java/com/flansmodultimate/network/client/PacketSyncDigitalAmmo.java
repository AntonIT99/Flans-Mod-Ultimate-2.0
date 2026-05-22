package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.digitalammo.LocalBulletManager;
import com.flansmodultimate.common.digitalammo.PlayerBulletStorage;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PacketSyncDigitalAmmo implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketSyncDigitalAmmo> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "sync_digital_ammo"));

    public static final StreamCodec<FriendlyByteBuf, PacketSyncDigitalAmmo> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketSyncDigitalAmmo decode(FriendlyByteBuf buf)
        {
            PacketSyncDigitalAmmo packet = new PacketSyncDigitalAmmo();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketSyncDigitalAmmo packet)
        {
            packet.encodeInto(buf);
        }
    };

    private double[] bullets;

    public PacketSyncDigitalAmmo()
    {
    }

    public PacketSyncDigitalAmmo(double[] bullets)
    {
        this.bullets = bullets;
    }

    public static void syncToClient(ServerPlayer player)
    {
        PlayerBulletStorage.PlayerBulletData data = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());
        double[] bullets = new double[data.getNumTypes()];
        for (int i = 0; i < data.getNumTypes(); i++)
        {
            bullets[i] = data.getBullets(i + 1);
        }
        PacketHandler.sendTo(new PacketSyncDigitalAmmo(bullets), player);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf buf)
    {
        if (bullets == null)
        {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(bullets.length);
        for (double bullet : bullets)
        {
            buf.writeDouble(bullet);
        }
    }

    @Override
    public void decodeInto(FriendlyByteBuf buf)
    {
        int length = buf.readVarInt();
        bullets = new double[length];
        for (int i = 0; i < length; i++)
        {
            bullets[i] = buf.readDouble();
        }
    }

    @Override
    public void handleClientSide()
    {
        if (bullets != null)
        {
            LocalBulletManager.setAllBullets(bullets);
        }
    }
}