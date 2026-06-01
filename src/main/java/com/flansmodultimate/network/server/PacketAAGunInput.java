package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.AAGun;
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
public class PacketAAGunInput implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketAAGunInput> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "aa_gun_input"));

    public static final StreamCodec<FriendlyByteBuf, PacketAAGunInput> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketAAGunInput decode(FriendlyByteBuf buf)
        {
            PacketAAGunInput packet = new PacketAAGunInput();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketAAGunInput packet)
        {
            packet.encodeInto(buf);
        }
    };

    private int aaGunId;
    private boolean shootKeyPressed;
    private boolean prevShootKeyPressed;

    public PacketAAGunInput(AAGun aaGun, boolean shootKeyPressed, boolean prevShootKeyPressed)
    {
        aaGunId = aaGun.getId();
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
        data.writeInt(aaGunId);
        data.writeBoolean(shootKeyPressed);
        data.writeBoolean(prevShootKeyPressed);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        aaGunId = data.readInt();
        shootKeyPressed = data.readBoolean();
        prevShootKeyPressed = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        if (world.getEntity(aaGunId) instanceof AAGun aaGun && aaGun.getFirstPassenger() == player)
        {
            aaGun.setShootKeyPressed(shootKeyPressed);
            aaGun.setPrevShootKeyPressed(prevShootKeyPressed);
        }
    }
}