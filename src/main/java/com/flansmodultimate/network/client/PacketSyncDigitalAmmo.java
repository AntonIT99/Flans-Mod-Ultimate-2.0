package com.flansmodultimate.network.client;

import com.flansmodultimate.client.digitalammo.LocalBulletManager;
import com.flansmodultimate.common.digitalammo.PlayerBulletStorage;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class PacketSyncDigitalAmmo implements IClientPacket
{
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
    public void encodeInto(RegistryFriendlyByteBuf buf)
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
    public void decodeInto(RegistryFriendlyByteBuf buf)
    {
        int length = buf.readVarInt();
        bullets = new double[length];
        for (int i = 0; i < length; i++)
        {
            bullets[i] = buf.readDouble();
        }
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (bullets != null)
        {
            LocalBulletManager.setAllBullets(bullets);
        }
    }
}
