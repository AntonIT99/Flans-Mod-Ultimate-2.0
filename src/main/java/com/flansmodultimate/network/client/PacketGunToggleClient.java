package com.flansmodultimate.network.client;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketGunToggleClient implements IClientPacket
{
    private InteractionHand hand;
    private boolean on;

    public PacketGunToggleClient(InteractionHand hand, boolean on)
    {
        this.hand = hand;
        this.on = on;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeEnum(hand);
        data.writeBoolean(on);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        hand = data.readEnum(InteractionHand.class);
        on = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
            gunItem.getConfigType().setToggledOn(stack, on);
    }
}
