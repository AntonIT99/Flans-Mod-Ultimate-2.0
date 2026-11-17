package com.flansmodultimate.mixin;

import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.common.entity.EntityDistancePolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/**
 * Hooks into the world renderer's entity pass.
 *
 * <ul>
 * <li>Draws driveables and their crews in chunks that are not built, which vanilla skips, when their render distance
 * setting reaches beyond the loaded chunks.</li>
 * <li>Tells the GPU model cache when a world render is in progress, which is when it may draw through the buffer source a
 * render mod substitutes.</li>
 * </ul>
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin
{
    /** The entity whose chunk the entity pass is about to check. Render thread only. */
    @Unique
    private Entity flansmodultimateChunkCheckedEntity;

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos flansmodultimateRememberChunkCheckedEntity(Entity entity)
    {
        flansmodultimateChunkCheckedEntity = entity;
        return entity.blockPosition();
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isChunkCompiled(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean flansmodultimateDrawDriveablesBeyondChunks(LevelRenderer renderer, BlockPos pos)
    {
        Entity entity = flansmodultimateChunkCheckedEntity;
        flansmodultimateChunkCheckedEntity = null;
        return renderer.isChunkCompiled(pos) || entity != null && EntityDistancePolicy.rendersBeyondChunks(entity);
    }

    // GPU model cache

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void flansmodultimateBeginLevelPass(CallbackInfo callback)
    {
        GpuModelCache.beginLevelPass();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void flansmodultimateEndLevelPass(CallbackInfo callback)
    {
        GpuModelCache.endLevelPass();
    }
}
