package com.flansmodultimate.common.raytracing.hits;

import lombok.Getter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// Hits are ordered and compared by intersect time and entity only, as defined in BulletHit.
@SuppressWarnings("java:S2160")
@Getter
public class BlockHit extends BulletHit
{
    private final BlockHitResult hitResult;
    private final BlockState blockstate;

    private final Vec3 worldPosition;
    private final Vec3 worldNormal;

    public BlockHit(BlockHitResult blockHitResult, Float f, BlockState blockstate)
    {
        this(blockHitResult, f, blockstate, blockHitResult.getLocation(), Vec3.atLowerCornerOf(blockHitResult.getDirection().getNormal()));
    }

    public BlockHit(BlockHitResult blockHitResult, Float f, BlockState blockstate, Vec3 worldPosition, Vec3 worldNormal)
    {
        super(f);
        this.worldPosition = worldPosition;
        this.worldNormal = worldNormal;
        hitResult = blockHitResult;
        this.blockstate = blockstate;
    }

    @Override
    public Entity getEntity()
    {
        return null;
    }
}
