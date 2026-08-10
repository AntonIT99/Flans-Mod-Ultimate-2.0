package com.flansmodultimate.network.server;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketDeployedGunInput implements IServerPacket
{
    private static final int MAX_SYNCED_BARRELS = 32;
    private static final Vec3[] EMPTY_BARREL_DATA = new Vec3[0];

    private int deployedGunId;
    private boolean shootKeyPressed;
    private boolean prevShootKeyPressed;
    private Vec3[] aaGunBarrelPivots = EMPTY_BARREL_DATA;
    private Vec3[] aaGunBarrelMuzzles = EMPTY_BARREL_DATA;

    public PacketDeployedGunInput(DeployedGun deployedGun, boolean shootKeyPressed, boolean prevShootKeyPressed)
    {
        deployedGunId = deployedGun.getId();
        this.shootKeyPressed = shootKeyPressed;
        this.prevShootKeyPressed = prevShootKeyPressed;
    }

    public PacketDeployedGunInput(AAGun aaGun, boolean shootKeyPressed, boolean prevShootKeyPressed)
    {
        this(aaGun, shootKeyPressed, prevShootKeyPressed, null, null);
    }

    public PacketDeployedGunInput(AAGun aaGun, boolean shootKeyPressed, boolean prevShootKeyPressed, Vec3[] barrelPivots, Vec3[] barrelMuzzles)
    {
        deployedGunId = aaGun.getId();
        this.shootKeyPressed = shootKeyPressed;
        this.prevShootKeyPressed = prevShootKeyPressed;
        setBarrelOriginData(barrelPivots, barrelMuzzles);
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeInt(deployedGunId);
        data.writeBoolean(shootKeyPressed);
        data.writeBoolean(prevShootKeyPressed);
        writeBarrelOriginData(data, aaGunBarrelPivots, aaGunBarrelMuzzles);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        deployedGunId = data.readInt();
        shootKeyPressed = data.readBoolean();
        prevShootKeyPressed = data.readBoolean();
        Vec3[][] barrelOriginData = readBarrelOriginData(data);
        aaGunBarrelPivots = barrelOriginData[0];
        aaGunBarrelMuzzles = barrelOriginData[1];
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (level.getEntity(deployedGunId) instanceof DeployedGun deployedGun && deployedGun.getFirstPassenger() == player)
        {
            deployedGun.setShootKeyPressed(shootKeyPressed);
            deployedGun.setPrevShootKeyPressed(prevShootKeyPressed);
        }
        else if (level.getEntity(deployedGunId) instanceof AAGun aaGun && aaGun.getFirstPassenger() == player)
        {
            if (hasBarrelOriginData())
                aaGun.setModelBarrelOriginData(aaGunBarrelPivots, aaGunBarrelMuzzles);
            aaGun.setShootKeyPressed(shootKeyPressed);
            aaGun.setPrevShootKeyPressed(prevShootKeyPressed);
        }
    }

    private boolean hasBarrelOriginData()
    {
        return aaGunBarrelPivots.length > 0 && aaGunBarrelPivots.length == aaGunBarrelMuzzles.length;
    }

    private void setBarrelOriginData(Vec3[] barrelPivots, Vec3[] barrelMuzzles)
    {
        if (barrelPivots == null || barrelMuzzles == null || barrelPivots.length == 0 || barrelPivots.length != barrelMuzzles.length || barrelPivots.length > MAX_SYNCED_BARRELS)
        {
            aaGunBarrelPivots = EMPTY_BARREL_DATA;
            aaGunBarrelMuzzles = EMPTY_BARREL_DATA;
            return;
        }

        aaGunBarrelPivots = barrelPivots.clone();
        aaGunBarrelMuzzles = barrelMuzzles.clone();
    }

    static void writeBarrelOriginData(RegistryFriendlyByteBuf data, Vec3[] barrelPivots, Vec3[] barrelMuzzles)
    {
        if (barrelPivots == null || barrelMuzzles == null || barrelPivots.length == 0 || barrelPivots.length != barrelMuzzles.length || barrelPivots.length > MAX_SYNCED_BARRELS)
        {
            data.writeBoolean(false);
            return;
        }

        data.writeBoolean(true);
        data.writeVarInt(barrelPivots.length);
        for (int i = 0; i < barrelPivots.length; i++)
        {
            writeVec3(data, barrelPivots[i]);
            writeVec3(data, barrelMuzzles[i]);
        }
    }

    static Vec3[][] readBarrelOriginData(RegistryFriendlyByteBuf data)
    {
        if (!data.readBoolean())
            return new Vec3[][] { EMPTY_BARREL_DATA, EMPTY_BARREL_DATA };

        int count = data.readVarInt();
        if (count <= 0 || count > MAX_SYNCED_BARRELS)
            throw new IllegalArgumentException("Invalid AA gun barrel origin count " + count);

        Vec3[] barrelPivots = new Vec3[count];
        Vec3[] barrelMuzzles = new Vec3[count];
        for (int i = 0; i < count; i++)
        {
            barrelPivots[i] = readVec3(data);
            barrelMuzzles[i] = readVec3(data);
        }
        return new Vec3[][] { barrelPivots, barrelMuzzles };
    }

    private static void writeVec3(RegistryFriendlyByteBuf data, Vec3 vector)
    {
        data.writeDouble(vector.x);
        data.writeDouble(vector.y);
        data.writeDouble(vector.z);
    }

    private static Vec3 readVec3(RegistryFriendlyByteBuf data)
    {
        return new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
    }
}
