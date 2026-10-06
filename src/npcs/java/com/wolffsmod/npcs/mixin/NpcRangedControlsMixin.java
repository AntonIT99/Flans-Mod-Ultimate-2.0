package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.NpcRangedControls;
import com.wolffsmod.npcs.client.RangedControlLocks;
import noppes.npcs.client.gui.SubGuiNpcProjectiles;
import noppes.npcs.client.gui.SubGuiNpcRangeProperties;
import noppes.npcs.shared.client.gui.components.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes only inactive ranged defaults read-only, retaining the ordinary editor save path. */
@Mixin(value = {SubGuiNpcRangeProperties.class, SubGuiNpcProjectiles.class}, remap = false)
public abstract class NpcRangedControlsMixin extends GuiBasic
{
    @Unique
    private RangedControlLocks wolffsmodnpcsLocks;

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsMarkOverrides(CallbackInfo callback)
    {
        wolffsmodnpcsLocks = NpcRangedControls.apply(this);
    }

    @Inject(method = "unFocused", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsKeepStoredValue(GuiTextFieldNop field, CallbackInfo callback)
    {
        if (wolffsmodnpcsLocks != null && wolffsmodnpcsLocks.textFields().containsKey(field.id))
            callback.cancel();
    }

    @Inject(method = "buttonEvent", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsKeepStoredSelection(GuiButtonNop button, CallbackInfo callback)
    {
        if (wolffsmodnpcsLocks != null && wolffsmodnpcsLocks.buttons().containsKey(button.id))
            callback.cancel();
    }
}
