package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

@NoArgsConstructor
public class PacketGunInput implements IServerPacket
{
    private boolean shootKeyPressed;
    private boolean prevShootKeyPressed;
    private boolean secondaryFunctionKeyPressed;
    InteractionHand hand;

    public PacketGunInput(boolean shootKeyPressed, boolean prevShootKeyPressed, boolean secondaryFunctionKeyPressed, InteractionHand hand)
    {
        this.shootKeyPressed = shootKeyPressed;
        this.prevShootKeyPressed = prevShootKeyPressed;
        this.secondaryFunctionKeyPressed = secondaryFunctionKeyPressed;
        this.hand = hand;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeBoolean(shootKeyPressed);
        data.writeBoolean(prevShootKeyPressed);
        data.writeBoolean(secondaryFunctionKeyPressed);
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        shootKeyPressed = data.readBoolean();
        prevShootKeyPressed = data.readBoolean();
        secondaryFunctionKeyPressed = data.readBoolean();
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        PlayerData data = PlayerData.getInstance(player, LogicalSide.SERVER);
        data.setShootKeyPressed(hand, shootKeyPressed);
        data.setPrevShootKeyPressed(hand, prevShootKeyPressed);
        data.setSecondaryFunctionKeyPressed(secondaryFunctionKeyPressed);
    }
}
