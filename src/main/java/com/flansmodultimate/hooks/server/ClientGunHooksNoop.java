package com.flansmodultimate.hooks.server;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.hooks.IClientGunHooks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ClientGunHooksNoop implements IClientGunHooks
{
    @Override
    public void meleeGunItem(GunItem gunItem, Player player, InteractionHand hand)
    {
        /* no-op */
    }

    @Override
    public void shootGunItem(GunItem gunItem, Level level, Player player, PlayerData data, GunAnimations animations, ItemStack gunStack, InteractionHand hand)
    {
        /* no-op */
    }

    @Override
    public void reloadGunItem(GunItem gunItem, Player player, InteractionHand hand, float reloadTime, int reloadCount, boolean hasMultipleAmmo)
    {
        /* no-op */
    }

    @Override
    public void cancelReloadGunItem(Player player, InteractionHand hand)
    {
        /* no-op */
    }

    @Override
    public void tickGunItem(GunItem gunItem, Level level, Player player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean dualWield)
    {
        /* no-op */
    }

    @Override
    public void accelerateMinigun(Player player, InteractionHand hand, float rotationSpeed)
    {
        /* no-op */
    }

    @Override
    public void tickDeployedGun(DeployedGun deployedGun)
    {
        /* no-op */
    }

    @Override
    @Nullable
    public HitResult getClientHitResult()
    {
        return null;
    }
}
