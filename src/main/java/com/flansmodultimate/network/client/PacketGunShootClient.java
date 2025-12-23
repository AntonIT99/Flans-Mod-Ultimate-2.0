package com.flansmodultimate.network.client;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

@NoArgsConstructor
public class PacketGunShootClient implements IClientPacket
{
    private UUID playerUUID;
    private InteractionHand hand;
    private boolean isShooting;

    public PacketGunShootClient(UUID playerUUID, InteractionHand hand, boolean isShooting)
    {
        this.playerUUID = playerUUID;
        this.hand = hand;
        this.isShooting = isShooting;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUtf(playerUUID.toString());
        data.writeEnum(hand);
        data.writeBoolean(isShooting);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        playerUUID = UUID.fromString(data.readUtf());
        hand = data.readEnum(InteractionHand.class);
        isShooting = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        Player reloadingPlayer = level.getPlayerByUUID(playerUUID);
        if (reloadingPlayer != null)
        {
            PlayerData data = PlayerData.getInstance(reloadingPlayer, LogicalSide.CLIENT);
            data.setShooting(hand, isShooting);
        }
    }
}
