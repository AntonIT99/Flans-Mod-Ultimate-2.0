package com.flansmodultimate.network.server.gun;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.EnumMeleeDirection;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

@NoArgsConstructor
public class PacketGunInput implements IServerPacket
{
    private boolean shootKeyPressed;
    private boolean secondaryFunctionKeyPressed;
    private InteractionHand hand;
    /** The directional melee swing selected by the movement keys held */
    private EnumMeleeDirection meleeDirection = EnumMeleeDirection.FORWARD;

    public PacketGunInput(boolean shootKeyPressed, boolean secondaryFunctionKeyPressed, InteractionHand hand)
    {
        this(shootKeyPressed, secondaryFunctionKeyPressed, hand, EnumMeleeDirection.FORWARD);
    }

    public PacketGunInput(boolean shootKeyPressed, boolean secondaryFunctionKeyPressed, InteractionHand hand, EnumMeleeDirection meleeDirection)
    {
        this.shootKeyPressed = shootKeyPressed;
        this.secondaryFunctionKeyPressed = secondaryFunctionKeyPressed;
        this.hand = hand;
        this.meleeDirection = meleeDirection;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(shootKeyPressed);
        data.writeBoolean(secondaryFunctionKeyPressed);
        data.writeEnum(hand);
        data.writeVarInt(meleeDirection.ordinal());
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        shootKeyPressed = data.readBoolean();
        secondaryFunctionKeyPressed = data.readBoolean();
        hand = data.readEnum(InteractionHand.class);
        meleeDirection = EnumMeleeDirection.byOrdinal(data.readVarInt());
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.receiveGunInput(hand, shootKeyPressed);
        data.setSecondaryFunctionKeyPressed(secondaryFunctionKeyPressed);
        data.setRequestedMeleeDirection(meleeDirection);
    }
}
