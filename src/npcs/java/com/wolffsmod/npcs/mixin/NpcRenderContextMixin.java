package com.wolffsmod.npcs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.diag.NpcProfiler;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.platform.render.NpcRenderBuffers;
import com.wolffsmod.npcs.properties.NpcPresentation;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/**
 * Carries render context through borrowed buffers, copies visual policies and model pitch, bank and ground lift to Flan models, immediately
 * hides killed bodies when requested, leaves the body of an invisible NPC undrawn and keeps an operator standing at a deployed gun at a
 * player's height.
 */
@Mixin(value = RenderCustomNpc.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class NpcRenderContextMixin
{
    /** When the NPC being drawn started, while the client benchmark measures; 0 otherwise. */
    @Unique
    private long wolffsmodnpcsRenderStarted;

    @Inject(method = "render(Lnoppes/npcs/entity/EntityCustomNpc;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsPresentation(EntityCustomNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback)
    {
        NpcPresentation settings = NpcPresentation.of(npc);
        if (npc.modelData.getEntity(npc) instanceof FlanModelEntity model)
        {
            model.setVisualEffects(settings.isHurtFlash(), settings.isDeathRotation());
            NpcVehicleState state = NpcVehicleState.of(npc);
            model.setAttitude(state.attitudePitch(partialTick), state.attitudeBank(partialTick), state.attitudeLift(partialTick) / Math.max(0.01F, npc.display.getSize() / 5F));
        }
        if (!settings.isDeathRotation() && npc.stats.hideKilledBody && npc.isKilled())
            callback.cancel();
    }

    @ModifyVariable(method = "render(Lnoppes/npcs/entity/EntityCustomNpc;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MultiBufferSource wolffsmodnpcsWorldModelBuffer(MultiBufferSource original, EntityCustomNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light)
    {
        return NpcRenderBuffers.wrap(original, npc, partialTick, pose, (RenderCustomNpc<?, ?>) (Object) this);
    }

    @Inject(method = "render(Lnoppes/npcs/entity/EntityCustomNpc;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void wolffsmodnpcsRenderStarted(EntityCustomNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback)
    {
        wolffsmodnpcsRenderStarted = NpcProfiler.client() ? System.nanoTime() : 0L;
    }

    /**
     * Custom NPCs gives its humanoid body a render type whether or not the body is visible. An invisible NPC, which is one
     * wearing Flan armor that hides its wearer, is drawn as vanilla draws an invisible mob: only its outline while it glows.
     */
    @SuppressWarnings("unchecked") // The renderer's entity type is erased; every RenderCustomNpc draws an EntityCustomNpc.
    @Inject(method = "getRenderType(Lnoppes/npcs/entity/EntityCustomNpc;ZZZ)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsInvisibleBody(EntityCustomNpc npc, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> callback)
    {
        if (!bodyVisible && !translucent)
            callback.setReturnValue(glowing ? RenderType.outline(((RenderCustomNpc<EntityCustomNpc, ?>) (Object) this).getTextureLocation(npc)) : null);
    }

    /** Custom NPCs lowers every rider into a seat; one standing at a mount that keeps riders upright stays on its feet. */
    @SuppressWarnings("UnresolvedMixinReference") // m_7860_ is the production SRG name; the dev jar only has getRenderOffset.
    @Inject(method = {"getRenderOffset(Lnoppes/npcs/entity/EntityCustomNpc;F)Lnet/minecraft/world/phys/Vec3;",
        "m_7860_(Lnoppes/npcs/entity/EntityCustomNpc;F)Lnet/minecraft/world/phys/Vec3;"}, at = @At("RETURN"), cancellable = true, require = 1)
    private void wolffsmodnpcsStandingRider(EntityCustomNpc npc, float partialTick, CallbackInfoReturnable<Vec3> callback)
    {
        if (npc.isAlive() && NpcDrivers.standsWhileRiding(npc))
            callback.setReturnValue(callback.getReturnValue().add(0D, (0.5F - npc.modelData.getLegsY() * 0.8F) * (npc.display.getSize() / 5F), 0D));
    }

    @Inject(method = "render(Lnoppes/npcs/entity/EntityCustomNpc;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void wolffsmodnpcsRenderFinished(EntityCustomNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback)
    {
        if (wolffsmodnpcsRenderStarted != 0L)
            NpcProfiler.npcRender(System.nanoTime() - wolffsmodnpcsRenderStarted);
        wolffsmodnpcsRenderStarted = 0L;
    }
}
