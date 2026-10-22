package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.names.SoldierNames;
import noppes.npcs.client.gui.SubGuiNpcName;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Appends soldier lists to the standard client NPC name selector while retaining its gender and Generate controls. */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = SubGuiNpcName.class, remap = false)
public abstract class NpcNameControlsMixin
{
    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnoppes/npcs/shared/client/gui/components/GuiButtonBiDirectional;<init>(Lnoppes/npcs/shared/client/gui/listeners/IGuiInterface;IIIII[Ljava/lang/String;I)V", ordinal = 0), index = 6)
    private String[] wolffsmodnpcsAppendSoldierLists(String[] original)
    {
        return SoldierNames.appendLabels(original);
    }
}
