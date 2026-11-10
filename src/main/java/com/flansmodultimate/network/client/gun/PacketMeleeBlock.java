package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.common.guns.MeleeBlocking;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A melee block raised for the given number of ticks, or lowered for zero, so that clients show the guard stance and arm pose */
@NoArgsConstructor
public class PacketMeleeBlock implements IClientPacket
{
    private int entityId;
    private InteractionHand hand;
    private int ticks;

    public PacketMeleeBlock(int entityId, InteractionHand hand, int ticks)
    {
        this.entityId = entityId;
        this.hand = hand;
        this.ticks = ticks;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeEnum(hand);
        data.writeVarInt(Math.max(0, ticks));
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        hand = data.readEnum(InteractionHand.class);
        ticks = data.readVarInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof LivingEntity holder)
        {
            MeleeBlocking.applyClient(holder, hand, ticks);
            ClientHooks.GUN.animateMeleeBlock(holder, hand, ticks);
        }
    }
}
