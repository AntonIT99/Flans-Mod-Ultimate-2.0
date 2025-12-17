package com.flansmodultimate.network.client;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelGun;
import com.flansmodultimate.ModClient;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

@NoArgsConstructor
public class PacketGunReloadClient implements IClientPacket
{
    private InteractionHand hand;
    private float reloadTime;
    private int reloadCount;
    private boolean hasMultipleAmmo;

    public PacketGunReloadClient(InteractionHand hand, float reloadTime, int reloadCount, boolean hasMultipleAmmo)
    {
        this.hand = hand;
        this.reloadTime = reloadTime;
        this.reloadCount = reloadCount;
        this.hasMultipleAmmo = hasMultipleAmmo;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(hand);
        data.writeFloat(reloadTime);
        data.writeInt(reloadCount);
        data.writeBoolean(hasMultipleAmmo);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
        reloadTime = data.readFloat();
        reloadCount = data.readInt();
        hasMultipleAmmo = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        if (player.getItemInHand(hand).getItem() instanceof GunItem gunItem)
        {
            PlayerData data = PlayerData.getInstance(player);

            // Set player shoot delay to be the reload delay - Set both gun delays to avoid reloading two guns at once
            data.setShootTimeRight(reloadTime);
            data.setShootTimeLeft(reloadTime);
            data.setReloading(hand,true);
            data.setBurstRoundsRemaining(hand,0);

            GunAnimations animations = ModClient.getGunAnimations(player, hand);

            int pumpDelay = 0;
            int pumpTime = 1;
            int chargeDelay = 0;
            int chargeTime = 1;

            if (ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun modelGun)
            {
                pumpDelay = modelGun.getPumpDelayAfterReload();
                pumpTime = modelGun.getPumpTime();
                chargeDelay = modelGun.getChargeDelayAfterReload();
                chargeTime = modelGun.getChargeTime();
            }

            animations.doReload(reloadTime, pumpDelay, pumpTime, chargeDelay, chargeTime, reloadCount, hasMultipleAmmo);
        }
    }
}

