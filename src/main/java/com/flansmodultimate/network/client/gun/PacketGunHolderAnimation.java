package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A mob reloaded or swung the gun in one of its hands: plays that gun model's reload or melee animation for everyone who sees it. */
@NoArgsConstructor
public class PacketGunHolderAnimation implements IClientPacket
{
    public enum Kind
    {
        RELOAD, MELEE
    }

    private int entityId;
    private InteractionHand hand;
    private Kind kind;
    /** Reload duration in ticks; unused for a melee swing. */
    private float reloadTicks;

    public PacketGunHolderAnimation(int entityId, InteractionHand hand, Kind kind, float reloadTicks)
    {
        this.entityId = entityId;
        this.hand = hand;
        this.kind = kind;
        this.reloadTicks = reloadTicks;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeEnum(hand);
        data.writeEnum(kind);
        data.writeFloat(reloadTicks);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        hand = data.readEnum(InteractionHand.class);
        kind = data.readEnum(Kind.class);
        reloadTicks = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        // Players animate their own guns through the local shooting state.
        if (!(level.getEntity(entityId) instanceof LivingEntity holder) || holder instanceof Player)
            return;
        if (kind == Kind.MELEE)
            ClientHooks.GUN.animateRemoteMelee(holder, hand);
        else if (Float.isFinite(reloadTicks) && reloadTicks > 0F)
            ClientHooks.GUN.animateRemoteReload(holder, hand, reloadTicks);
    }
}
