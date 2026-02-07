package com.flansmodultimate.hooks;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.item.GunItem;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public interface IClientGunHooks
{
    void meleeGunItem(GunItem gunItem, Player player, InteractionHand hand);

    void shootGunItem(GunItem gunItem, Level level, Player player, PlayerData data, GunAnimations animations, ItemStack gunStack, InteractionHand hand);

    void reloadGunItem(GunItem gunItem, Player player, InteractionHand hand, float reloadTime, int reloadCount, boolean hasMultipleAmmo);

    void cancelReloadGunItem(Player player, InteractionHand hand);

    void tickGunItem(GunItem gunItem, Level level, Player player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean dualWield);

    void accelerateMinigun(Player player, InteractionHand hand, float rotationSpeed);

    void tickDeployedGun(DeployedGun deployedGun);

    @Nullable
    HitResult getClientHitResult();
}
