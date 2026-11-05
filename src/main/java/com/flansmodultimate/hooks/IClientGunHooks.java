package com.flansmodultimate.hooks;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.item.GunItem;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public interface IClientGunHooks
{
    void secondaryModeSwitch(Player player, InteractionHand hand, int ticks);

    void meleeGunItem(GunItem gunItem, Player player, InteractionHand hand);

    /** Applies recoil, screen shake and the model animation once for a confirmed local shot. */
    void animateLocalShot(Player player, InteractionHand hand);

    /** Plays the shot animation, muzzle flash model included, of a gun fired by a mob or another player. */
    void animateRemoteShot(LivingEntity shooter, InteractionHand hand);

    /** Plays the reload animation of a gun held by a mob, lasting the given number of ticks. */
    void animateRemoteReload(LivingEntity holder, InteractionHand hand, float reloadTicks);

    /** Plays the melee animation of a gun swung by a mob. */
    void animateRemoteMelee(LivingEntity holder, InteractionHand hand);

    /**
     * @param tactical whether the gun still held live rounds when the reload began
     * @param loopRounds rounds loaded one at a time by a round-by-round reload, or 0
     */
    // Mirrors the reload packet fields
    @SuppressWarnings("java:S107")
    void reloadGunItem(GunItem gunItem, Player player, InteractionHand hand, float reloadTime, int reloadCount, boolean hasMultipleAmmo, boolean tactical, int loopRounds);

    void cancelReloadGunItem(Player player, InteractionHand hand);

    void tickGunItem(GunItem gunItem, Level level, Player player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean dualWield);

    void accelerateMinigun(Player player, InteractionHand hand, float rotationSpeed);

    void tickDeployedGun(DeployedGun deployedGun);

    void tickAAGun(AAGun aaGun);

    @Nullable
    HitResult getClientHitResult();
}
