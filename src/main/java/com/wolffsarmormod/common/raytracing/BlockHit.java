package com.wolffsarmormod.common.raytracing;

import lombok.Getter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public class BlockHit extends BulletHit
{
    @Getter
    private final HitResult hitResult;
    @Getter
    private final BlockState blockstate;

    public BlockHit(HitResult mop, Float f, BlockState blockstate)
    {
        super(f);
        hitResult = mop;
        this.blockstate = blockstate;
    }

    @Override
    public Entity getEntity()
    {
        return null;
    }
}
