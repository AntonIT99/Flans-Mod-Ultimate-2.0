package com.flansmodultimate.network.server.gun;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@NoArgsConstructor
public class PacketDeployedGunInput implements IServerPacket
{
    private int deployedGunId;
    private boolean shootKeyPressed;

    public PacketDeployedGunInput(Entity gun, boolean shootKeyPressed)
    {
        deployedGunId = gun.getId();
        this.shootKeyPressed = shootKeyPressed;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeInt(deployedGunId);
        data.writeBoolean(shootKeyPressed);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        deployedGunId = data.readInt();
        shootKeyPressed = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (level.getEntity(deployedGunId) instanceof DeployedGun deployedGun && deployedGun.getFirstPassenger() == player)
            deployedGun.receiveShootInput(shootKeyPressed);
        else if (level.getEntity(deployedGunId) instanceof AAGun aaGun && aaGun.getFirstPassenger() == player)
            aaGun.setShootKeyPressed(shootKeyPressed);
    }
}
