package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunFireModeClient;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunFireMode implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketGunFireMode> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_fire_mode"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunFireMode> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunFireMode decode(FriendlyByteBuf buf)
        {
            PacketGunFireMode packet = new PacketGunFireMode();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunFireMode packet)
        {
            packet.encodeInto(buf);
        }
    };

    private InteractionHand hand;

    public PacketGunFireMode(InteractionHand hand)
    {
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
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
            return;

        GunType gunType = gunItem.getConfigType();
        EnumFireMode previousMode = gunType.getFireMode(stack);
        EnumFireMode nextMode = gunType.cycleFireMode(stack);
        if (previousMode == nextMode)
            return;

        PlayerData.getInstance(player).setBurstRoundsRemaining(hand, 0);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        PacketHandler.sendTo(new PacketGunFireModeClient(hand, nextMode), player);
    }
}