package com.flansmodultimate.network.server.gun;

import com.flansmodultimate.common.PlayerData;
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

    public PacketGunInput(boolean shootKeyPressed, boolean secondaryFunctionKeyPressed, InteractionHand hand)
    {
        this.shootKeyPressed = shootKeyPressed;
        this.secondaryFunctionKeyPressed = secondaryFunctionKeyPressed;
        this.hand = hand;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(shootKeyPressed);
        data.writeBoolean(secondaryFunctionKeyPressed);
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        shootKeyPressed = data.readBoolean();
        secondaryFunctionKeyPressed = data.readBoolean();
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.receiveGunInput(hand, shootKeyPressed);
        data.setSecondaryFunctionKeyPressed(secondaryFunctionKeyPressed);
    }
}
