package com.flansmodultimate.network.server.effects;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.GunSoundPermissions.Kind;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Reports an animation boundary, never a client-selected sound, position or range. */
@NoArgsConstructor
public class PacketRequestPlaySound implements IServerPacket
{
    private InteractionHand hand;
    private Kind kind;

    public PacketRequestPlaySound(InteractionHand hand, Kind kind)
    {
        this.hand = hand;
        this.kind = kind;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeEnum(hand);
        data.writeEnum(kind);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        hand = data.readEnum(InteractionHand.class);
        kind = data.readEnum(Kind.class);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isAlive() || !(stack.getItem() instanceof GunItem gun))
            return;
        String sound = kind == Kind.ACTION ? gun.getConfigType().getActionSound() : gun.getConfigType().getBulletInsert();
        if (StringUtils.isBlank(sound) || !PlayerData.getInstance(player).getGunSoundPermissions().take(hand, kind, stack, level.getGameTime()))
            return;
        double range = kind == Kind.ACTION ? ModCommonConfig.get().gunActionSoundRange() : ModCommonConfig.get().reloadSoundRange();
        PacketHandler.sendToAllExcept(level.dimension(), player.position(), range, player, new PacketPlaySound(player.position(), range, sound, false, false, player));
    }
}
