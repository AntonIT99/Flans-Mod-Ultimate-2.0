package com.flansmodultimate.mixin;

import com.flansmodultimate.client.render.PlayerSkinOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;

/** Lets a team player class replace the skin of the players wearing it. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerSkinOverrideMixin
{
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$overridePlayerSkin(AbstractClientPlayer player,
                                                     CallbackInfoReturnable<ResourceLocation> callback)
    {
        ResourceLocation skin = PlayerSkinOverrides.getSkin(player, (PlayerRenderer)(Object)this);
        if (skin != null)
            callback.setReturnValue(skin);
    }
}
