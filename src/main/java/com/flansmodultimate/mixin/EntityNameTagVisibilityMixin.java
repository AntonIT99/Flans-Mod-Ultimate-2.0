package com.flansmodultimate.mixin;

import com.flansmodultimate.client.teams.TeamsClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Applies the Teams name tag rules wherever a tag is drawn. RenderNameTagEvent only guards
 * {@code EntityRenderer#render}, but some mods draw tags directly: EntityCulling draws them for
 * players hidden behind blocks, which would show enemy tags, and their positions, through walls.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityNameTagVisibilityMixin
{
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$hideTeamsNameTag(Entity entity, Component displayName, PoseStack poseStack,
                                                     MultiBufferSource buffer, int packedLight, float partialTick,
                                                     CallbackInfo ci)
    {
        if (entity instanceof Player player && TeamsClientState.shouldHideNameTag(player))
            ci.cancel();
    }
}
