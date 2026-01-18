package com.flansmodultimate.network.client;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

@NoArgsConstructor
public class PacketDeployableGunMount implements IClientPacket
{
    private UUID playerUUID;
    private int deployedGunId;
    private boolean mount;

    public PacketDeployableGunMount(Player player, DeployedGun deployedGun, boolean mount)
    {
        playerUUID = player.getUUID();
        deployedGunId = deployedGun.getId();
        this.mount = mount;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUtf(playerUUID.toString());
        data.writeInt(deployedGunId);
        data.writeBoolean(mount);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        playerUUID = UUID.fromString(data.readUtf());
        deployedGunId = data.readInt();
        mount = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        Player mountingPlayer = level.getPlayerByUUID(playerUUID);
        if (mountingPlayer != null && level.getEntity(deployedGunId) instanceof DeployedGun deployedGun)
            deployedGun.mountGun(level, mountingPlayer, PlayerData.getInstance(mountingPlayer), mount);
    }
}
