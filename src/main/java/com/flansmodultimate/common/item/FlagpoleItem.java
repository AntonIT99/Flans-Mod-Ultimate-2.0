package com.flansmodultimate.common.item;

import com.flansmodultimate.common.entity.Flagpole;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class FlagpoleItem extends Item
{
    public FlagpoleItem()
    {
        super(new Properties().stacksTo(16));
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context)
    {
        if (!(context.getLevel() instanceof ServerLevel level))
            return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Flagpole pole = new Flagpole(level, Vec3.atBottomCenterOf(pos));
        if (!level.noCollision(pole))
            return InteractionResult.FAIL;
        level.addFreshEntity(pole);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
