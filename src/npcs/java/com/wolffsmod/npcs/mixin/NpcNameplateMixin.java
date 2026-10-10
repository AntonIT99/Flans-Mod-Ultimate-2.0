package com.wolffsmod.npcs.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.client.FlanModelBounds;
import com.wolffsmod.npcs.client.FlanNameplatePlacement;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.properties.NpcPresentation;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;

/** Raises Flan labels above their model, preserves tint with hurt flashes disabled and hides killed bodies without death rotation. */
@Mixin(value = RenderNPCInterface.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class NpcNameplateMixin
{
    @Inject(method = "renderColor", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsUnhurtTint(EntityNPCInterface npc, CallbackInfo callback)
    {
        if (!NpcPresentation.of(npc).isHurtFlash())
        {
            int tint = npc.display.getTint();
            RenderSystem.setShaderColor((tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F, 1F);
            callback.cancel();
        }
    }

    @Inject(method = "render(Lnoppes/npcs/entity/EntityNPCInterface;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsHideKilled(EntityNPCInterface npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback)
    {
        if (!NpcPresentation.of(npc).isDeathRotation() && npc.stats.hideKilledBody && npc.isKilled())
            callback.cancel();
    }

    @SuppressWarnings("unchecked")
    @ModifyVariable(method = "renderLivingLabel", at = @At("STORE"), name = "height")
    private float wolffsmodnpcsNameHeight(float original, EntityNPCInterface npc, PoseStack pose, MultiBufferSource buffers, int light)
    {
        if (!(npc instanceof EntityCustomNpc custom) || !(custom.modelData.getEntity(custom) instanceof FlanModelEntity model))
            return original;
        float scale = custom.modelData.simpleRender ? 1F : custom.display.getSize() / 5F;
        var renderer = (EntityRenderer<EntityNPCInterface>) (Object) this;
        double offset = renderer.getRenderOffset(npc, 0).y;
        return FlanNameplatePlacement.height(original, npc.getBbHeight(), FlanModelBounds.of(model), scale, offset);
    }
}
