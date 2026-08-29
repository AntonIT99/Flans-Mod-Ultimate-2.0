package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunSecondaryModeClient;
import com.flansmodultimate.network.client.PacketPlaySound;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunSecondaryMode implements IServerPacket
{
    private InteractionHand hand;

    public PacketGunSecondaryMode(InteractionHand hand)
    {
        this.hand = hand;
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
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
            return;

        PlayerData data = PlayerData.getInstance(player);
        if (data.getShootTime(hand) > 0F || !gunItem.getConfigType().canToggleSecondaryFire(stack))
            return;

        boolean secondary = !gunItem.getConfigType().getSecondaryFire(stack);
        gunItem.getConfigType().setSecondaryFire(stack, secondary);
        data.setBurstRoundsRemaining(hand, 0);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        PacketHandler.sendTo(new PacketGunSecondaryModeClient(hand, secondary), player);

        String sound = gunItem.getConfigType().getSecondaryFireToggleSound(stack);
        if (StringUtils.isNotBlank(sound))
            PacketPlaySound.sendSoundPacket(player, gunItem.getConfigType().getReloadSoundRange(), sound, true);
    }
}
