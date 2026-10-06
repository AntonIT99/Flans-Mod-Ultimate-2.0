package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.NpcArmorControls;
import noppes.npcs.client.gui.SubGuiNpcResistanceProperties;
import noppes.npcs.shared.client.gui.components.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaves the stored NPC resistance values intact while equipped armor governs the damage pipeline. */
@Mixin(value = SubGuiNpcResistanceProperties.class, remap = false)
public abstract class NpcArmorControlsMixin extends GuiBasic
{
    @Unique
    private boolean wolffsmodnpcsArmorReadOnly;

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsArmorReadouts(CallbackInfo callback)
    {
        wolffsmodnpcsArmorReadOnly = NpcArmorControls.apply(this);
    }

    @Inject(method = {"mouseDragged", "mouseReleased"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsRetainResistance(GuiSliderNop slider, CallbackInfo callback)
    {
        if (wolffsmodnpcsArmorReadOnly)
            callback.cancel();
    }
}
