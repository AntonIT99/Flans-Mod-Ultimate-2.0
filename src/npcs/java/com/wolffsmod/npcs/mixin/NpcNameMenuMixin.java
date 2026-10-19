package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.names.SoldierNames;
import noppes.npcs.api.gui.DisplayMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Adds the same soldier lists to Custom NPCs' server-driven API display menu. */
@Mixin(value = DisplayMenu.class, remap = false)
public abstract class NpcNameMenuMixin
{
    @ModifyArg(method = "openNameSubGui", at = @At(value = "INVOKE", target = "Lnoppes/npcs/api/wrapper/gui/CustomGuiButtonListWrapper;setValues([Ljava/lang/String;)Lnoppes/npcs/api/wrapper/gui/CustomGuiButtonListWrapper;", ordinal = 0), index = 0)
    private String[] wolffsmodnpcsAppendSoldierLists(String[] original)
    {
        return SoldierNames.appendLabels(original);
    }
}
