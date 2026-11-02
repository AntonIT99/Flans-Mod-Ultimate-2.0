package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A brief lock-marker lease sent only to the player operating the locking weapon. */
@NoArgsConstructor
public class PacketLockMarker implements IClientPacket
{
    private int target;
    private InteractionHand hand;

    public PacketLockMarker(int target, InteractionHand hand)
    {
        this.target = target;
        this.hand = hand;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeInt(target);
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        target = data.readInt();
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.lockMarker(target, hand);
    }
}
