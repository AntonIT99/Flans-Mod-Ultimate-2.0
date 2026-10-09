package com.flansmodultimate.mixin;

import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Hooks into the base entity renderer: name tag range and Teams visibility, and held-item frustum culling.
 *
 * <p>
 * The Teams name tag rules apply wherever a tag is drawn. RenderNameTagEvent only guards
 * {@code EntityRenderer#render}, but some mods draw tags directly: EntityCulling draws them for
 * players hidden behind blocks, which would show enemy tags, and their positions, through walls.
 * </p>
 *
 * <p>
 * Holders of items with oversized parts such as laser beams skip frustum culling but keep the distance check.
 * </p>
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin
{
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateHideTeamsNameTag(Entity entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick, CallbackInfo ci)
    {
        if (entity instanceof Player player && TeamsClientState.shouldHideNameTag(player))
            ci.cancel();
    }

    @SuppressWarnings("UnstableApiUsage") // Redirects and falls back to the NeoForge hook it replaces.
    @Redirect(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;isNameplateInRenderDistance(Lnet/minecraft/world/entity/Entity;D)Z", remap = false))
    private boolean flansmodultimateNameTagRenderDistance(Entity entity, double squareDistance)
    {
        if (!(entity instanceof LivingEntity livingEntity))
            return ClientHooks.isNameplateInRenderDistance(entity, squareDistance);

        float range = ModCommonConfig.nameTagRenderRange(livingEntity.isDiscrete());
        return squareDistance < range * range;
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateDisableCulling(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> result)
    {
        if (entity instanceof LivingEntity living && (flansmodultimateIsCullingDisabled(living.getMainHandItem()) || flansmodultimateIsCullingDisabled(living.getOffhandItem())))
            result.setReturnValue(entity.shouldRender(camX, camY, camZ));
    }

    @Unique
    private static boolean flansmodultimateIsCullingDisabled(ItemStack stack)
    {
        return stack.getItem() instanceof IFlanItem<?> flanItem && flanItem.getConfigType().isFrustumCullingDisabled(stack);
    }
}
