package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketDeployedGunInput implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketDeployedGunInput> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "deployed_gun_input"));

    public static final StreamCodec<FriendlyByteBuf, PacketDeployedGunInput> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketDeployedGunInput decode(FriendlyByteBuf buf)
        {
            PacketDeployedGunInput packet = new PacketDeployedGunInput();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketDeployedGunInput packet)
        {
            packet.encodeInto(buf);
        }
    };

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
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
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
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        if (world.getEntity(deployedGunId) instanceof DeployedGun deployedGun && deployedGun.getFirstPassenger() == player)
        {
            deployedGun.setShootKeyPressed(shootKeyPressed);
            deployedGun.setPrevShootKeyPressed(prevShootKeyPressed);
        }
    }
}