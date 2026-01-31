package com.flansmodultimate.network.client;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
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
public class PacketGunReloadClient implements IClientPacket
{
    private UUID playerUUID;
    private InteractionHand hand;
    private float reloadTime;
    private int reloadCount;
    private boolean hasMultipleAmmo;

    public PacketGunReloadClient(UUID playerUUID, InteractionHand hand, float reloadTime, int reloadCount, boolean hasMultipleAmmo)
    {
        this.playerUUID = playerUUID;
        this.hand = hand;
        this.reloadTime = reloadTime;
        this.reloadCount = reloadCount;
        this.hasMultipleAmmo = hasMultipleAmmo;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUUID(playerUUID);
        data.writeEnum(hand);
        data.writeFloat(reloadTime);
        data.writeInt(reloadCount);
        data.writeBoolean(hasMultipleAmmo);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
        reloadTime = data.readFloat();
        reloadCount = data.readInt();
        hasMultipleAmmo = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        Player reloadingPlayer = level.getPlayerByUUID(playerUUID);
        if (reloadingPlayer != null && reloadingPlayer.getItemInHand(hand).getItem() instanceof GunItem gunItem)
        {
            PlayerData data = PlayerData.getInstance(reloadingPlayer, LogicalSide.CLIENT);
            GunAnimations animations = ModClient.getGunAnimations(player, hand);
            gunItem.getGunItemHandler().doPlayerReloadClient(data, animations, hand, reloadTime, reloadCount, hasMultipleAmmo);
        }
    }
}

