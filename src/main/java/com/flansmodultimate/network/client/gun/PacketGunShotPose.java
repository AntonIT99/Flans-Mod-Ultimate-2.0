package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A mob or another player fired the gun in one of its hands:raises that gun for a moment in the dynamic aim pose and plays its shot animation and muzzle flash model. */
@NoArgsConstructor
public class PacketGunShotPose implements IClientPacket
{
    private int entityId;
    private InteractionHand hand;
    /** Whether the gun model plays its shot animation; the aim pose is raised either way. */
    private boolean animate = true;

    public PacketGunShotPose(int entityId, InteractionHand hand)
    {
        this(entityId, hand, true);
    }

    public PacketGunShotPose(int entityId, InteractionHand hand, boolean animate)
    {
        this.entityId = entityId;
        this.hand = hand;
        this.animate = animate;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeEnum(hand);
        data.writeBoolean(animate);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        hand = data.readEnum(InteractionHand.class);
        animate = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof LivingEntity shooter)
        {
            // A player's own shots already reach them through the shooting state and their local shot animation.
            if (shooter == player)
                return;
            GunArmPoses.recordShot(shooter, hand);
            if (animate)
                ClientHooks.GUN.animateRemoteShot(shooter, hand);
        }
    }
}
