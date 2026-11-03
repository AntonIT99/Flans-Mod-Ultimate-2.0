package com.flansmodultimate.common.driveables;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.equipment.MechaUpgrades;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

/** Passive addon aggregation and protected landing damage shared by model entities and real mechas. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MechaEquipmentSupport
{
    public static MechaUpgrades upgrades(Iterable<ItemStack> stacks)
    {
        boolean fall = false;
        boolean breakBlocks = false;
        boolean dark = false;
        float repair = 0F;
        int light = 0;
        for (ItemStack stack : stacks)
        {
            if (stack.isEmpty() || !(stack.getItem() instanceof MechaAddonItem addon))
                continue;
            var type = addon.getConfigType();
            fall |= type.isStopMechaFallDamage();
            breakBlocks |= type.isForceBlockFallDamage();
            dark |= type.isForceDark();
            if (type.isAutoRepair())
                repair = Math.max(repair, type.getAutoRepairAmount());
            light = Math.max(light, type.getLightLevel());
        }
        return new MechaUpgrades(fall, breakBlocks, Math.max(0F, repair), Mth.clamp(light, 0, 15), dark);
    }

    public static void landing(Entity entity, MechaType type, boolean force, double descent)
    {
        double impact = Math.max(0D, -descent - 0.45D) * type.getBlockDamageFromFalling();
        if (!(entity.level() instanceof ServerLevel level) || !(force || type.isDamageBlocksFromFalling()) || impact <= 0.8D || !FlansMod.teamsManager.isDriveablesBreakBlocks()
            || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
            return;
        int radius = Math.min(3, 1 + Mth.floor(impact));
        BlockPos centre = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY - 0.1D, entity.getZ());
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -1, -radius), centre.offset(radius, 0, radius)))
        {
            if (pos.distSqr(centre) > radius * radius || !level.hasChunkAt(pos))
                continue;
            var state = level.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0F && level.getBlockEntity(pos) == null)
                ModUtils.destroyBlock(level, pos, entity, true);
        }
    }
}
