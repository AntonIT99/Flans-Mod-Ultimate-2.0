package com.flansmodultimate.network.client;

import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A mob fired the gun in one of its hands, which raises that gun for a moment in the dynamic aim pose. */
@NoArgsConstructor
public class PacketGunShotPose implements IClientPacket
{
    private int entityId;
    private InteractionHand hand;

    public PacketGunShotPose(int entityId, InteractionHand hand)
    {
        this.entityId = entityId;
        this.hand = hand;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof LivingEntity shooter)
            GunArmPoses.recordShot(shooter, hand);
    }
}
