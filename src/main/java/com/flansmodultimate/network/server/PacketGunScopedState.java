package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunScopedState implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketGunScopedState> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "gun_scoped_state"));

    public static final StreamCodec<FriendlyByteBuf, PacketGunScopedState> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketGunScopedState decode(FriendlyByteBuf buf)
        {
            PacketGunScopedState packet = new PacketGunScopedState();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketGunScopedState packet)
        {
            packet.encodeInto(buf);
        }
    };

    private boolean isScoped;

    public PacketGunScopedState(boolean isScoped)
    {
        this.isScoped = isScoped;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeBoolean(isScoped);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        isScoped = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        ItemStack stack = player.getInventory().getSelected();
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
        {
            AttachmentType scope = gunItem.getConfigType().getScope(stack);

            if (gunItem.getConfigType().isAllowNightVision() || (scope != null && scope.isHasNightVision()))
            {
                if (isScoped)
                {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false, true));
                    CommonEventHandler.getNightVisionPlayers().add(player.getUUID());
                }
                else
                {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                    CommonEventHandler.getNightVisionPlayers().remove(player.getUUID());
                }
            }
        }
    }
}