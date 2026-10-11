package com.flansmodultimate.common.raytracing.hits;

import lombok.Getter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Hits are ordered and compared by intersect time and entity only, as defined in BulletHit.
@SuppressWarnings("java:S2160")
@Getter
public class BlockHit extends BulletHit
{
    private final BlockHitResult hitResult;
    private final BlockState blockstate;

    public BlockHit(BlockHitResult blockHitResult, Float f, BlockState blockstate)
    {
        super(f);
        hitResult = blockHitResult;
        this.blockstate = blockstate;
    }

    @Override
    public Entity getEntity()
    {
        return null;
    }
}
