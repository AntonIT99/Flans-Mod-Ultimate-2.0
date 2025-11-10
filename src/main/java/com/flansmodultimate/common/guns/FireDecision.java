package com.flansmodultimate.common.guns;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.PacketPlaySound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record FireDecision(boolean shouldShoot, boolean needsReload)
{
    public static FireDecision computeFireDecision(GunItem gunItem, ItemStack gunstack, InteractionHand hand, PlayerData data, boolean hold, boolean held, GunAnimations anim)
    {
        GunType type = gunItem.getConfigType();

        boolean needsToReload = needsToReload(gunItem, gunstack);
        boolean shouldShoot = false;

        switch (type.getFireMode(gunstack))
        {
            case BURST -> {
                // continue burst if rounds remain
                if (data.getBurstRoundsRemaining(hand) > 0)
                    shouldShoot = true;

                // then behave like SEMIAUTO for edge press
                if (hold && !held)
                    shouldShoot = true;
                else
                    needsToReload = false;
            }
            case SEMIAUTO -> {
                if (hold && !held)
                    shouldShoot = true;
                else
                    needsToReload = false;
            }
            case MINIGUN -> {
                // if empty, only reload while holding
                if (needsToReload)
                    return new FireDecision(false, hold);

                // spin-up
                if (hold) {
                    accelerateMinigun(type, data, anim);
                    if (data.getMinigunSpeed() < type.getMinigunStartSpeed())
                    {
                        playMinigunWarmupIfNeeded(type, data);
                        return new FireDecision(false, false); // still warming up
                    }
                }
                // fall-through into FULLAUTO behavior
                shouldShoot = hold;
                handleMinigunLoopingSounds(type, data);
            }
            case FULLAUTO -> {
                shouldShoot = hold;
                if (!shouldShoot)
                    needsToReload = false;
                handleMinigunLoopingSounds(type, data);
            }
            default -> needsToReload = false;
        }

        return new FireDecision(shouldShoot, needsToReload);
    }

    private static boolean needsToReload(GunItem gunItem, ItemStack stack)
    {
        for (int i = 0; i < gunItem.getConfigType().getNumAmmoItemsInGun(); i++)
        {
            ItemStack bulletStack = gunItem.getBulletItemStack(stack, i);
            if (bulletStack != null && !bulletStack.isEmpty() && bulletStack.getDamageValue() < bulletStack.getMaxDamage())
            {
                return false;
            }
        }
        return true;
    }

    private static void accelerateMinigun(GunType type, PlayerData data, GunAnimations anim)
    {
        if (data.getMinigunSpeed() < type.getMinigunMaxSpeed())
        {
            data.setMinigunSpeed(2.0F);
            anim.addMinigunBarrelRotationSpeed(2.0F);
        }
    }

    private static void playMinigunWarmupIfNeeded(GunType type, PlayerData data)
    {
        if (!type.isUseLoopingSounds())
            return;
        if (data.getLoopedSoundDelay() > 0)
            return;
        if (data.getMinigunSpeed() <= 0.1F)
            return;
        if (data.isReloadingRight() || data.isSpinning())
            return;

        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);

        data.setLoopedSoundDelay(type.getWarmupSoundLength());
        PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, type.getWarmupSound(), false);
        data.setSpinning(true);
    }

    private static void handleMinigunLoopingSounds(GunType type, PlayerData data)
    {
        if (!type.isUseLoopingSounds())
            return;

        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);

        // play loop when above start speed
        if (data.getLoopedSoundDelay() <= 0 && data.getMinigunSpeed() > type.getMinigunStartSpeed())
        {
            data.setLoopedSoundDelay(type.getLoopedSoundLength());
            PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, type.getLoopedSound(), false);
            data.setSpinning(true);
        }

        // cooldown when we drop below start speed
        if (data.isSpinning() && data.getMinigunSpeed() < type.getMinigunStartSpeed())
        {
            PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, type.getCooldownSound(), false);
            data.setSpinning(true);
        }
    }
}
