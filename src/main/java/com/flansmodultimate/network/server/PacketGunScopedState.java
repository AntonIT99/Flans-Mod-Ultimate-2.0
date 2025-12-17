package com.flansmodultimate.network.server;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunScopedState implements IServerPacket
{
    private boolean isScoped;

    public PacketGunScopedState(boolean isScoped)
    {
        this.isScoped = isScoped;
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
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ItemStack stack = player.getInventory().getSelected();
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
        {
            AttachmentType scope = gunItem.getConfigType().getScope(stack);

            // Apply night vision while scoped if gun.allowNightVision = true
            if (gunItem.getConfigType().isAllowNightVision() || (scope != null && scope.isHasNightVision()))
            {
                if (isScoped)
                {
                    // 1200 ticks = 60s, like before. Reapplying is fine.
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false, true));
                    CommonEventHandler.getNightVisionPlayers().add(player.getUUID());
                }
                else
                {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                    CommonEventHandler.getNightVisionPlayers().remove(player.getUUID());
                }
            }

            gunItem.setScoped(isScoped);
        }
    }
}