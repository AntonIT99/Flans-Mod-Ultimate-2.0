package com.wolffsmod.npcs.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.renderer.MultiBufferSource;

/** NeoForge 1.21.1 / Custom NPCs boundary: carry context through the borrowed model's buffer. */
@Mixin(value = RenderCustomNpc.class, remap = false)
public abstract class NpcRenderContextMixin
{
    @ModifyVariable(method = "render(Lnoppes/npcs/entity/EntityCustomNpc;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MultiBufferSource wolffsmodnpcs$worldModelBuffer(MultiBufferSource original, EntityCustomNpc npc,
        float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light)
    {
        return NpcRenderBuffers.wrap(original, npc, partialTick, pose, (RenderCustomNpc<?, ?>)(Object)this);
    }
}
