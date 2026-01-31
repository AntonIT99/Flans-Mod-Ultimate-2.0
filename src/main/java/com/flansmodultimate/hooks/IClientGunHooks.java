package com.flansmodultimate.hooks;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IClientGunHooks
{
    void tickGunClient(GunItem gunItem, Level level, Player player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean dualWield);
}
