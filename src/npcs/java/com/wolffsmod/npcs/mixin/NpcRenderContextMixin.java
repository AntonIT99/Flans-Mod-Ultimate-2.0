package com.wolffsmod.npcs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.platform.render.NpcRenderBuffers;
import com.wolffsmod.npcs.properties.NpcPresentation;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;

/** Carries render context through borrowed buffers, copies visual policies to Flan models and immediately hides killed bodies when requested. */
@Mixin(value = RenderCustomNpc.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class NpcRenderContextMixin
{
    @Inject(method = "render(Lnoppes/npcs/entity/EntityCustomNpc;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsPresentation(EntityCustomNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback)
    {
        NpcPresentation settings = NpcPresentation.of(npc);
        if (npc.modelData.getEntity(npc) instanceof FlanModelEntity model)
            model.setVisualEffects(settings.isHurtFlash(), settings.isDeathRotation());
        if (!settings.isDeathRotation() && npc.stats.hideKilledBody && npc.isKilled())
            callback.cancel();
    }

    @ModifyVariable(method = "render(Lnoppes/npcs/entity/EntityCustomNpc;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MultiBufferSource wolffsmodnpcsWorldModelBuffer(MultiBufferSource original, EntityCustomNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light)
    {
        return NpcRenderBuffers.wrap(original, npc, partialTick, pose, (RenderCustomNpc<?, ?>) (Object) this);
    }
}
