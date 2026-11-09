package com.flansmodultimate.mixin;

import com.flansmodultimate.platform.client.ClientPlatform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Skips the repeated background call of screens that drew theirs early, so its blur does not cover their content. */
@Mixin(Screen.class)
public abstract class ScreenMixin
{
    @SuppressWarnings("DataFlowIssue") // Mixin: this is the target Screen at runtime
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateSkipRepeatedBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        if (ClientPlatform.consumeBackgroundDrawn((Screen) (Object) this))
            ci.cancel();
    }
}
