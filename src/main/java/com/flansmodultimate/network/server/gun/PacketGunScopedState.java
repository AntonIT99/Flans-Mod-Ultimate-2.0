package com.flansmodultimate.network.server.gun;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
public class PacketGunScopedState implements IServerPacket
{
    private boolean isScoped;

    public PacketGunScopedState(boolean isScoped)
    {
        this.isScoped = isScoped;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(isScoped);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        isScoped = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        PlayerData data = PlayerData.getInstance(player);
        boolean changed = data.isScoped() != isScoped;
        data.setScoped(isScoped);
        // Aiming raises the gun in the dynamic aim pose, which everyone who sees the player has to know
        if (changed)
            GunArmPoses.syncPlayer(player);

        ItemStack stack = player.getInventory().getSelected();
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
        {
            AttachmentType scope = gunItem.getConfigType().getScope(stack);

            // Apply night vision while scoped if gun.allowNightVision = true
            if (gunItem.getConfigType().isAllowNightVision() || (scope != null && scope.isHasNightVision()))
            {
                Set<UUID> scopeNightVision = CommonEventHandler.getNightVisionPlayers();
                if (isScoped)
                {
                    // Night vision from elsewhere, a potion say, is left alone: lowering the scope would end it.
                    if (scopeNightVision.contains(player.getUUID()) || !player.hasEffect(MobEffects.NIGHT_VISION))
                    {
                        // 1200 ticks = 60s, like before. Reapplying is fine.
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false, true));
                        scopeNightVision.add(player.getUUID());
                    }
                }
                else if (scopeNightVision.remove(player.getUUID()))
                {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                }
            }
        }
    }
}
