package com.flansmodultimate.network.server;

import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketDeployedGunInput implements IServerPacket
{
    private int deployedGunId;
    private boolean shootKeyPressed;
    private boolean prevShootKeyPressed;

    public PacketDeployedGunInput(DeployedGun deployedGun, boolean shootKeyPressed, boolean prevShootKeyPressed)
    {
        deployedGunId = deployedGun.getId();
        this.shootKeyPressed = shootKeyPressed;
        this.prevShootKeyPressed = prevShootKeyPressed;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeInt(deployedGunId);
        data.writeBoolean(shootKeyPressed);
        data.writeBoolean(prevShootKeyPressed);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        deployedGunId = data.readInt();
        shootKeyPressed = data.readBoolean();
        prevShootKeyPressed = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (level.getEntity(deployedGunId) instanceof DeployedGun deployedGun && deployedGun.getFirstPassenger() == player)
        {
            deployedGun.setShootKeyPressed(shootKeyPressed);
            deployedGun.setPrevShootKeyPressed(prevShootKeyPressed);
        }
    }
}
