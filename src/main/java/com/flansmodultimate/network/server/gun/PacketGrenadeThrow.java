package com.flansmodultimate.network.server.gun;

import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Sent when a player releases the button held on a hold-to-throw grenade, from the Labjac Edition.
 * The server only throws when the held grenade really is a hold-to-throw grenade the player may throw now.
 */
@NoArgsConstructor
@AllArgsConstructor
public class PacketGrenadeThrow implements IServerPacket
{
    /** True for an underhand toss (use button), false for an overhand throw (attack button) */
    private boolean underhand;

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(underhand);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        underhand = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!player.isAlive() || player.isSpectator())
            return;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.getItem() instanceof GrenadeItem grenadeItem)
            grenadeItem.throwByHand(player, stack, underhand);
    }
}
