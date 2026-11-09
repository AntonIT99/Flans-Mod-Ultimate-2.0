package com.wolffsmod.npcs.mixin;

import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Makes the mouse wheel scroll Custom NPCs' lists in both directions: the installed build reads the always-zero horizontal delta, so they only ever scrolled down. */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = GuiWrapper.class, remap = false)
public abstract class GuiWrapperMixin
{
    @Redirect(method = "mouseScrolled", at = @At(value = "INVOKE", target = "Lnoppes/npcs/shared/client/gui/components/GuiCustomScrollNop;mouseScrolled(DDDD)Z"))
    private boolean wolffsmodnpcsScrollVertically(GuiCustomScrollNop scroll, double mouseX, double mouseY, double scrollX, double scrollY)
    {
        return scroll.mouseScrolled(mouseX, mouseY, scrollY, scrollY);
    }
}
