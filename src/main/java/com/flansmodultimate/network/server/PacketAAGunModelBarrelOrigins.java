package com.flansmodultimate.network.server;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketAAGunModelBarrelOrigins implements IServerPacket
{
    private int aaGunId;
    private Vec3[] barrelPivots = new Vec3[0];
    private Vec3[] barrelMuzzles = new Vec3[0];

    public PacketAAGunModelBarrelOrigins(AAGun aaGun, Vec3[] barrelPivots, Vec3[] barrelMuzzles)
    {
        aaGunId = aaGun.getId();
        this.barrelPivots = barrelPivots == null ? new Vec3[0] : barrelPivots.clone();
        this.barrelMuzzles = barrelMuzzles == null ? new Vec3[0] : barrelMuzzles.clone();
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeInt(aaGunId);
        PacketDeployedGunInput.writeBarrelOriginData(data, barrelPivots, barrelMuzzles);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        aaGunId = data.readInt();
        Vec3[][] barrelOriginData = PacketDeployedGunInput.readBarrelOriginData(data);
        barrelPivots = barrelOriginData[0];
        barrelMuzzles = barrelOriginData[1];
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!(level.getEntity(aaGunId) instanceof AAGun aaGun))
            return;

        double syncRange = ModCommonConfig.aaGunTrackingRange() + 16D;
        if (player.distanceToSqr(aaGun) > syncRange * syncRange)
            return;

        aaGun.setModelBarrelOriginData(barrelPivots, barrelMuzzles);
    }
}
