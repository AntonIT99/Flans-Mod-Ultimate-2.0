package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

@NoArgsConstructor
public class PacketGunReloadClient implements IClientPacket
{
    private UUID playerUUID;
    private InteractionHand hand;
    private float reloadTime;
    private int reloadCount;
    private boolean hasMultipleAmmo;
    private boolean tactical;
    private int loopRounds;

    public PacketGunReloadClient(UUID playerUUID, InteractionHand hand, float reloadTime, int reloadCount, boolean hasMultipleAmmo, boolean tactical, int loopRounds)
    {
        this.playerUUID = playerUUID;
        this.hand = hand;
        this.reloadTime = reloadTime;
        this.reloadCount = reloadCount;
        this.hasMultipleAmmo = hasMultipleAmmo;
        this.tactical = tactical;
        this.loopRounds = loopRounds;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(playerUUID);
        data.writeEnum(hand);
        data.writeFloat(reloadTime);
        data.writeInt(reloadCount);
        data.writeBoolean(hasMultipleAmmo);
        data.writeBoolean(tactical);
        data.writeInt(loopRounds);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        playerUUID = data.readUUID();
        hand = data.readEnum(InteractionHand.class);
        reloadTime = data.readFloat();
        reloadCount = data.readInt();
        hasMultipleAmmo = data.readBoolean();
        tactical = data.readBoolean();
        loopRounds = data.readInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Player reloadingPlayer = level.getPlayerByUUID(playerUUID);
        if (reloadingPlayer != null && reloadingPlayer.getItemInHand(hand).getItem() instanceof GunItem gunItem)
        {
            ClientHooks.GUN.reloadGunItem(gunItem, reloadingPlayer, hand, reloadTime, reloadCount, hasMultipleAmmo, tactical, loopRounds);
        }
    }
}
