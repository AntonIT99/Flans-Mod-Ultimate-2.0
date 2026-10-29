package com.flansmodultimate.mixin;

import com.flansmodultimate.client.render.layer.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.minecraft.client.player.*;
import net.minecraft.client.renderer.entity.player.*;
import net.minecraft.resources.*;

/**
 * Hooks into the player renderer: a team player class can replace the skin of the players wearing it.
 */
@Mixin(PlayerRenderer.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class PlayerRendererMixin
{
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateOverridePlayerSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> callback)
    {
        ResourceLocation skin = PlayerSkinOverrides.getSkin(player, (PlayerRenderer) (Object) this);
        if (skin != null)
            callback.setReturnValue(skin);
    }
}
