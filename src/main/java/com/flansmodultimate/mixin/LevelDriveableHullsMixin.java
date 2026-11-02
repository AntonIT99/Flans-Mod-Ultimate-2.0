package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.collision.DriveableCollisionWorld;
import com.flansmodultimate.common.driveables.collision.DriveableHullLevel;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.teams.TeamsObjectProtection;
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
 * Protects Teams infrastructure from indirect removal during rounds and records terrain changes before blocks and their block entities are replaced.
 * Stores the driveable hulls of a level on the level itself, so that the
 * per-move lookup is a field read and the registry is released with the level.
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(Level.class)
public abstract class LevelDriveableHullsMixin implements DriveableHullLevel
{
    @Unique
    private final DriveableCollisionWorld.LevelHulls flansmodultimateDriveableHulls = new DriveableCollisionWorld.LevelHulls();

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"ConstantValue", "java:S1905"}) // Mixin merges this into Level; its ServerLevel subtype check and Object cast are required.
    private void flansmodultimateRecordRoundBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft, CallbackInfoReturnable<Boolean> callback)
    {
        if ((Object) this instanceof ServerLevel level && !level.isOutsideBuildHeight(pPos))
        {
            if (TeamsObjectProtection.preventsRemoval(level, pPos) && !level.getBlockState(pPos).is(pState.getBlock()))
            {
                callback.setReturnValue(false);
                return;
            }
            TeamsManager.getInstance().recordRoundBlock(level, pPos, pState);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"ConstantValue", "java:S1905"}) // Mixin merges this into Level; its ServerLevel subtype check and Object cast are required.
    private void flansmodultimateProtectRoundBlock(BlockPos pPos, boolean pDropBlock, net.minecraft.world.entity.Entity pEntity, int pRecursionLeft, CallbackInfoReturnable<Boolean> callback)
    {
        if ((Object) this instanceof ServerLevel level && TeamsObjectProtection.preventsRemoval(level, pPos))
            callback.setReturnValue(false);
    }

    @Override
    public DriveableCollisionWorld.LevelHulls flansmodultimateGetDriveableHulls()
    {
        return flansmodultimateDriveableHulls;
    }
}
