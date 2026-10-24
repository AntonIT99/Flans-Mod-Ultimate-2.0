package com.wolffsmod.npcs.mixin;

import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Removes persistent mouse focus from Custom NPCs buttons while retaining keyboard focus and hover highlighting. */
@SuppressWarnings({"UnresolvedMixinReference", "DataFlowIssue"}) // Mixin merges this class into GuiButtonNop, making the self-cast valid.
@Mixin(value = GuiButtonNop.class, remap = false)
public abstract class GuiButtonNopMixin
{
    @Inject(method = {"renderWidget", "m_88315_"}, at = @At("HEAD"))
    private void wolffsmodnpcsClearMouseFocus(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback)
    {
        if (!Minecraft.getInstance().getLastInputType().isKeyboard())
            ((GuiButtonNop) (Object) this).setFocused(false);
    }
}
