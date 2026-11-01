package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.collision.DriveableCollisionWorld;
import com.flansmodultimate.common.driveables.collision.DriveableHullLevel;
import com.flansmodultimate.common.teams.TeamsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Records Teams terrain changes before blocks and their block entities are replaced.
 * Stores the driveable hulls of a level on the level itself, so that the
 * per-move lookup is a field read and the registry is released with the level.
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(Level.class)
public abstract class LevelDriveableHullsMixin implements DriveableHullLevel
{
    @Unique
    private final DriveableCollisionWorld.LevelHulls flansmodultimateDriveableHulls = new DriveableCollisionWorld.LevelHulls();

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    @SuppressWarnings({"DataFlowIssue", "java:S1905"}) // The Object cast is required to test the target Level type from its mixin.
    private void flansmodultimateRecordRoundBlock(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> callback)
    {
        if ((Object) this instanceof ServerLevel level && !level.isOutsideBuildHeight(pos))
            TeamsManager.getInstance().recordRoundBlock(level, pos, state);
    }

    @Override
    public DriveableCollisionWorld.LevelHulls flansmodultimateGetDriveableHulls()
    {
        return flansmodultimateDriveableHulls;
    }
}
