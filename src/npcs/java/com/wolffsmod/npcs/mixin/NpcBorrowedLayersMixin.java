package com.wolffsmod.npcs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;

/** Borrowed NPC skin layers read the live owner's held items, throw display, item transforms and animation policy. */
@Mixin(targets = "noppes.npcs.client.renderer.RenderCustomNpc$1", remap = false)
public abstract class NpcBorrowedLayersMixin
{
    // Both complete signatures are needed: the invocation carries the detached model, the enclosing call its live owner.
    @SuppressWarnings({"rawtypes", "unchecked", "java:S107", "DefaultAnnotationParam", "UnresolvedMixinReference"})
    @Redirect(method = {"render",
        "m_6494_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V", remap = true))
    private void wolffsmodnpcsRenderOwner(RenderLayer layer, PoseStack pose, MultiBufferSource buffers, int light, Entity borrowed, float swing, float amount, float partialTick, float age, float yaw,
        float pitch, PoseStack ownerPose, MultiBufferSource ownerBuffers, int ownerLight, Entity owner, float ownerSwing, float ownerAmount, float ownerPartialTick, float ownerAge, float ownerYaw,
        float ownerPitch)
    {
        Entity holder = borrowed instanceof EntityCustomNpc && owner instanceof EntityCustomNpc ? owner : borrowed;
        layer.render(pose, buffers, light, holder, swing, amount, partialTick, age, yaw, pitch);
    }
}
