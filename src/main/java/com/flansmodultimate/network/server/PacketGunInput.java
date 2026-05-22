package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

@NoArgsConstructor
public class PacketGunInput implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketGunInput> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_input"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunInput> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunInput decode(FriendlyByteBuf buf)
        {
            PacketGunInput packet = new PacketGunInput();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunInput packet)
        {
            packet.encodeInto(buf);
        }
    };

    private boolean shootKeyPressed;
    private boolean prevShootKeyPressed;
    private boolean secondaryFunctionKeyPressed;
    InteractionHand hand;

    public PacketGunInput(boolean shootKeyPressed, boolean prevShootKeyPressed, boolean secondaryFunctionKeyPressed, InteractionHand hand)
    {
        this.shootKeyPressed = shootKeyPressed;
        this.prevShootKeyPressed = prevShootKeyPressed;
        this.secondaryFunctionKeyPressed = secondaryFunctionKeyPressed;
        this.hand = hand;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeBoolean(shootKeyPressed);
        data.writeBoolean(prevShootKeyPressed);
        data.writeBoolean(secondaryFunctionKeyPressed);
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        shootKeyPressed = data.readBoolean();
        prevShootKeyPressed = data.readBoolean();
        secondaryFunctionKeyPressed = data.readBoolean();
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        PlayerData data = PlayerData.getInstance(player, net.neoforged.fml.LogicalSide.SERVER);
        data.setShootKeyPressed(hand, shootKeyPressed);
        data.setPrevShootKeyPressed(hand, prevShootKeyPressed);
        data.setSecondaryFunctionKeyPressed(secondaryFunctionKeyPressed);
    }
}