package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunSpread implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketGunSpread> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_spread"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunSpread> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunSpread decode(FriendlyByteBuf buf)
        {
            PacketGunSpread packet = new PacketGunSpread();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunSpread packet)
        {
            packet.encodeInto(buf);
        }
    };

    private float spread;

    public PacketGunSpread(ItemStack stack, float amount)
    {
        if (stack != null && stack.getItem() instanceof GunItem)
            spread = amount;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(spread);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        spread = data.readFloat();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        ItemStack stack = player.getInventory().getSelected();
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
        {
            GunType type = gunItem.getConfigType();

            if (type.getGrip(stack) != null && type.getSecondaryFire(stack))
                type.getGrip(stack).setSecondarySpread(spread);
            else
                type.setBulletSpread(spread);
        }
    }
}