package com.flansmodultimate.network.client;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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
        data.writeUUID(playerUUID);
        data.writeEnum(hand);
        data.writeBoolean(isShooting);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
        isShooting = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Player shootingPlayer = level.getPlayerByUUID(playerUUID);
        if (shootingPlayer != null)
        {
            PlayerData data = PlayerData.getInstance(shootingPlayer, LogicalSide.CLIENT);
            data.setShooting(hand, isShooting);
        }
    }
}
