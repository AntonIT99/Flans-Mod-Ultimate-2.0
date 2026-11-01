package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.hooks.IClientGunHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A confirmed gun shot: raises the aim pose and plays the shot effects, including recoil for the local player. */
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
        handleShot(player, level, ClientHooks.GUN);
    }

    void handleShot(Player player, Level level, IClientGunHooks hooks)
    {
        if (level.getEntity(entityId) instanceof LivingEntity shooter)
        {
            GunArmPoses.recordShot(shooter, hand);
            if (animate)
            {
                // A shot event cannot be lost when start/stop state updates arrive between client ticks.
                if (shooter == player)
                    hooks.animateLocalShot(player, hand);
                else
                    hooks.animateRemoteShot(shooter, hand);
            }
        }
    }
}
