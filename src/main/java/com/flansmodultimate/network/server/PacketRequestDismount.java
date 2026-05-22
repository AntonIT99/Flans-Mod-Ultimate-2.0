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
public class PacketRequestDismount implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketRequestDismount> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "request_dismount"));

    public static final StreamCodec<FriendlyByteBuf, PacketRequestDismount> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketRequestDismount decode(FriendlyByteBuf buf)
        {
            PacketRequestDismount packet = new PacketRequestDismount();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketRequestDismount packet)
        {
            packet.encodeInto(buf);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        if (player.getVehicle() instanceof DeployedGun)
            player.stopRiding();
    }
}