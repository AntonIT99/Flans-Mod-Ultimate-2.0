package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunAnimation;
import com.flansmodultimate.network.client.PacketPlaySound;
import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * This packet is send by the client to request a reload. The server checks if the player can reload and in this case actually reloads and sends a GunAnimationPacket as response.
 * The GunAnimationPacket plays the reload animation and sets the pumpDelay & pumpTime times to prevent the client from shooting while reloading
 */
@NoArgsConstructor
public class PacketReload implements IServerPacket
{
    private boolean isOffHand;
    private boolean isForced;

    public PacketReload(InteractionHand hand, boolean isForced)
    {
        this.isOffHand = hand == InteractionHand.OFF_HAND;
        this.isForced = isForced;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeBoolean(isOffHand);
        data.writeBoolean(isForced);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        isOffHand = data.readBoolean();
        isForced = data.readBoolean();
    }

    @Override
    public void handleServerSide(ServerPlayer player)
    {
        InteractionHand hand = isOffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        PlayerData data = PlayerData.getInstance(player);
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack stack = isOffHand ? off : main;
        boolean hasOffHand = !main.isEmpty() && !off.isEmpty();
        ItemStack otherHand = isOffHand ? main : off;

        if(data != null && stack.getItem() instanceof GunItem gunItem)
        {
            GunType type = gunItem.getConfigType();

            if (gunItem.getBehavior().reload(stack, player.level(), player, player.getInventory(), hand, hasOffHand, isForced, player.isCreative()))
            {
                //TODO: implement Enchantments
                //float reloadDelay = EnchantmentModule.ModifyReloadTime(type.getReloadTime(), player, otherHand);
                float reloadDelay = type.getReloadTime();

                //Set the reload delay
                data.setShootTimeRight(reloadDelay);
                data.setShootTimeLeft(reloadDelay);

                if (isOffHand)
                    data.setReloadingLeft(true);
                else
                    data.setReloadingRight(true);

                //Play reload sound
                if (type.getReloadSound() != null)
                    PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, type.getReloadSound(), false);

                PacketHandler.sendTo(new PacketGunAnimation(hand, reloadDelay, type.getPumpDelayAfterReload(), type.getPumpTime()), player);
            }
        }
    }
}