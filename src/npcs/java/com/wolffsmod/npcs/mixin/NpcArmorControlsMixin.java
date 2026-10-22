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

import java.util.Set;

/** Leaves the stored NPC resistance values intact for the resistances equipped armor governs in the damage pipeline. */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(value = SubGuiNpcResistanceProperties.class, remap = false)
public abstract class NpcArmorControlsMixin extends GuiBasic
{
    @Unique
    private Set<Integer> wolffsmodnpcsArmorReadOnly = Set.of();

    @Inject(method = "init", at = @At("TAIL"))
    private void wolffsmodnpcsArmorReadouts(CallbackInfo callback)
    {
        wolffsmodnpcsArmorReadOnly = NpcArmorControls.apply(this);
    }

    @Inject(method = {"mouseDragged", "mouseReleased"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsRetainResistance(GuiSliderNop slider, CallbackInfo callback)
    {
        if (wolffsmodnpcsArmorReadOnly.contains(slider.id))
            callback.cancel();
    }
}
