package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
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
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunReload implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketGunReload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_reload"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunReload> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunReload decode(FriendlyByteBuf buf)
        {
            PacketGunReload packet = new PacketGunReload();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunReload packet)
        {
            packet.encodeInto(buf);
        }
    };

    private InteractionHand hand;

    public PacketGunReload(InteractionHand hand)
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
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
        {
            PlayerData data = PlayerData.getInstance(player, net.neoforged.fml.LogicalSide.SERVER);
            gunItem.getGunItemHandler().doPlayerReload(world, player, data, stack, hand, true);
        }
    }
}