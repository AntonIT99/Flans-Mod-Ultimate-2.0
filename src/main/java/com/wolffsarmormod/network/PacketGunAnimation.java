package com.wolffsarmormod.network;

import com.flansmod.client.model.GunAnimations;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.common.PlayerData;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

@NoArgsConstructor
public class PacketGunAnimation extends PacketBase
{
    public enum AnimationType
    {
        SHOOT, RELOAD, ROTATION, NONE
    }

    private AnimationType type;
    private AnimationType type2 = AnimationType.NONE;
    private Float minigunRotationAddSpeed;
    private Integer pumpdelay;
    private Integer pumptime;
    private Float recoil;
    private Integer reloadtime;
    private InteractionHand hand;

    public PacketGunAnimation(InteractionHand hand, Integer pumpdelay, Integer pumptime, Float recoil)
    {
        this.type = AnimationType.SHOOT;
        this.pumpdelay = pumpdelay;
        this.pumptime = pumptime;
        this.recoil = recoil;
        this.hand = hand;
    }

    public PacketGunAnimation(InteractionHand hand, Integer pumpdelay, Integer pumptime, Float recoil, Float minigunAddSpeed)
    {
        this(hand,pumpdelay,pumptime,recoil);
        this.type2 = AnimationType.ROTATION;
        this.minigunRotationAddSpeed = minigunAddSpeed;
    }

    public PacketGunAnimation(InteractionHand hand, Float minigunAddSpeed)
    {
        this.type = AnimationType.ROTATION;
        this.hand = hand;
        this.minigunRotationAddSpeed = minigunAddSpeed;
    }

    public PacketGunAnimation(InteractionHand hand, Integer reloadtime, Integer pumpdelay, Integer pumptime)
    {
        this.type = AnimationType.RELOAD;
        this.hand = hand;
        this.pumpdelay = pumpdelay;
        this.pumptime = pumptime;
        this.reloadtime = reloadtime;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(type);
        data.writeEnum(type2);
        data.writeInt(hand.equals(InteractionHand.MAIN_HAND) ? 0 : 1);
        encodeInto(data, type);
        encodeInto(data, type2);
    }

    private void encodeInto(FriendlyByteBuf data,AnimationType type)
    {
        switch (type) {
            case NONE -> {}
            case ROTATION -> data.writeFloat(minigunRotationAddSpeed);
            case SHOOT -> {
                data.writeInt(pumpdelay);
                data.writeInt(pumptime);
                data.writeFloat(recoil);
            }
            case RELOAD -> {
                data.writeInt(pumpdelay);
                data.writeInt(pumptime);
                data.writeInt(reloadtime);
            }
        }
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        this.type = data.readEnum(AnimationType.class);
        this.type2 = data.readEnum(AnimationType.class);
        this.hand = (data.readInt() == 0) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        decodeInto(data, type);
        decodeInto(data, type2);
    }

    private void decodeInto(FriendlyByteBuf data, AnimationType type)
    {
        switch (type)
        {
            case NONE -> {}
            case ROTATION -> this.minigunRotationAddSpeed = data.readFloat();
            case SHOOT -> {
                pumpdelay = data.readInt();
                pumptime = data.readInt();
                recoil = data.readFloat();
            }
            case RELOAD -> {
                pumpdelay = data.readInt();
                pumptime = data.readInt();
                reloadtime = data.readInt();
            }
        }
    }

    @Override
    public void handleClientSide(Minecraft mc)
    {
        GunAnimations animations = ModClient.getGunAnimations(mc.player, hand);
        handleAnimation(animations, type, mc.player);
        handleAnimation(animations, type2, mc.player);
    }

    private void handleAnimation(GunAnimations animations, AnimationType type, Player player)
    {
        switch (type)
        {
            case NONE:
                break;

            case RELOAD:
                animations.doReload(reloadtime, pumpdelay, pumptime);
                PlayerData data = PlayerData.getInstance(player);
                data.setShootTimeRight(reloadtime);
                data.setShootTimeLeft(reloadtime);
                data.setBurstRoundsRemaining(hand, 0);
                data.setReloadingLeft(true);
                data.setReloadingRight(true);
                break;

            case SHOOT:
                //TODO lookatstate not send by Server, may cause problems in future
                animations.lookAt = GunAnimations.LookAtState.NONE;
                animations.doShoot(pumpdelay, pumptime);
                ModClient.playerRecoil += recoil;
                animations.recoil += recoil;
                break;

            case ROTATION:
                animations.addMinigunBarrelRotationSpeed(minigunRotationAddSpeed);
                break;
        }
    }
}

