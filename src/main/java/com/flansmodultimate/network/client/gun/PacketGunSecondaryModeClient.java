package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketGunSecondaryModeClient implements IClientPacket
{
    private InteractionHand hand;
    private boolean secondary;
    private int switchTicks;

    public PacketGunSecondaryModeClient(InteractionHand hand, boolean secondary, int switchTicks)
    {
        this.hand = hand;
        this.secondary = secondary;
        this.switchTicks = Math.max(0, Math.min(1200, switchTicks));
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeEnum(hand);
        data.writeBoolean(secondary);
        data.writeVarInt(switchTicks);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        hand = data.readEnum(InteractionHand.class);
        secondary = data.readBoolean();
        switchTicks = Math.max(0, Math.min(1200, data.readVarInt()));
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
        {
            gunItem.getConfigType().setSecondaryFire(stack, secondary);
            PlayerData.getInstance(player).beginSecondaryModeSwitch(hand, switchTicks);
            ClientHooks.GUN.secondaryModeSwitch(player, hand, switchTicks);
        }
    }
}
