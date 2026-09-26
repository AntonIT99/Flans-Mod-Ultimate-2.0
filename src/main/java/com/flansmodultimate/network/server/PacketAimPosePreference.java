package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Announces the player's own aim pose choice to the server, on login and whenever it changes. The server
 * relays it to everyone who sees the player; its playerAimPose setting may still override it for everyone.
 */
@NoArgsConstructor
public class PacketAimPosePreference implements IServerPacket
{
    private boolean dynamicAimPose;

    public PacketAimPosePreference(boolean dynamicAimPose)
    {
        this.dynamicAimPose = dynamicAimPose;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(dynamicAimPose);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        dynamicAimPose = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        PlayerData.getInstance(player).setDynamicAimPose(dynamicAimPose);
        GunArmPoses.syncPlayer(player);
    }
}
