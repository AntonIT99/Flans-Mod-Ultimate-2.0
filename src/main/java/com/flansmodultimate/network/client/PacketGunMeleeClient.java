package com.flansmodultimate.network.client;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

@NoArgsConstructor
public class PacketGunMeleeClient implements IClientPacket
{
    private UUID playerUUID;
    private InteractionHand hand;

    public PacketGunMeleeClient(UUID playerUUID, InteractionHand hand)
    {
        this.playerUUID = playerUUID;
        this.hand = hand;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUUID(playerUUID);
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        Player meleePlayer = level.getPlayerByUUID(playerUUID);
        if (meleePlayer != null && meleePlayer.getItemInHand(hand).getItem() instanceof GunItem gunItem)
        {
            PlayerData data = PlayerData.getInstance(player);
            data.doMelee(player, gunItem.getConfigType().getMeleeTime(), gunItem.getConfigType());
            GunAnimations anim = ModClient.getGunAnimations(meleePlayer, hand);
            anim.doMelee(gunItem.getConfigType().getMeleeTime());
        }
    }
}
