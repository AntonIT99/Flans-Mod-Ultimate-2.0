package com.flansmodultimate.network.client;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * How a player holds their guns: their own aim pose choice, and whether they are aiming right now. Sent to
 * the player and everyone who sees them whenever either changes, and to anyone who starts seeing them.
 */
@NoArgsConstructor
public class PacketAimPoseState implements IClientPacket
{
    private UUID playerUUID;
    private boolean dynamicAimPose;
    private boolean aiming;

    public PacketAimPoseState(UUID playerUUID, boolean dynamicAimPose, boolean aiming)
    {
        this.playerUUID = playerUUID;
        this.dynamicAimPose = dynamicAimPose;
        this.aiming = aiming;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(playerUUID);
        data.writeBoolean(dynamicAimPose);
        data.writeBoolean(aiming);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        playerUUID = data.readUUID();
        dynamicAimPose = data.readBoolean();
        aiming = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        PlayerData data = PlayerData.getClientInstance(playerUUID);
        data.setDynamicAimPose(dynamicAimPose);
        data.setShownAiming(aiming);
    }
}
