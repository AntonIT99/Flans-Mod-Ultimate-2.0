package com.wolffsmod.npcs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.crew.NpcCrew;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

/** Skips hidden riders of NPC vehicles completely, including their model, equipment, name tag, fire and shadow. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
{
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("java:S107") // The injector must match vanilla's render arguments to read the entity.
    private void wolffsmodnpcsHideSeatedCrew(Entity entity, double x, double y, double z, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback)
    {
        if (NpcCrew.hiddenAboard(entity))
            callback.cancel();
    }
}
