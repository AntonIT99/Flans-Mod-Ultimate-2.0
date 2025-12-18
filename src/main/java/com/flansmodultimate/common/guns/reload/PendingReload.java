package com.flansmodultimate.common.guns.reload;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record PendingReload(
        ItemStack gunStack,
        InteractionHand hand,
        long applyAtGameTime,
        List<ReloadPlan> plans,
        boolean forceReload,
        boolean creative,
        boolean combineAmmoOnReload,
        boolean ammoToUpperInventory)
{}
