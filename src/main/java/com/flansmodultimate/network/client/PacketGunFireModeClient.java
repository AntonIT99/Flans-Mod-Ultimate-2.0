package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunFireModeClient implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketGunFireModeClient> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_fire_mode_client"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunFireModeClient> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunFireModeClient decode(FriendlyByteBuf buf)
        {
            PacketGunFireModeClient packet = new PacketGunFireModeClient();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunFireModeClient packet)
        {
            packet.encodeInto(buf);
        }
    };

    private InteractionHand hand;
    private EnumFireMode mode;

    public PacketGunFireModeClient(InteractionHand hand, EnumFireMode mode)
    {
        this.hand = hand;
        this.mode = mode;
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
        data.writeEnum(mode);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
        mode = data.readEnum(EnumFireMode.class);
    }

    @Override
    public void handleClientSide()
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
            gunItem.getConfigType().setFireMode(stack, mode);
    }
}