package com.wolffsmod.npcs.mixin;

import noppes.npcs.constants.EnumGuiType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Keeps the cursor in place when the NPC editor's inventory tab reopens over a menu the server still holds from an earlier visit to that tab.
 * Closing that menu on the client would grab the mouse and recentre it when the new screen opens; the server still closes it.
 */
@SuppressWarnings({"UnresolvedMixinReference", "AddedMixinMembersNamePattern", "java:S116"}) // Custom NPCs' anonymous class fixes the captured field's name.
@Mixin(targets = "noppes.npcs.NoppesUtilServer$3", remap = false)
public abstract class NoppesUtilServerMenuProviderMixin
{
    @Shadow
    @Final
    EnumGuiType val$gui;

    /** Overrides NeoForge's {@code IMenuProviderExtension} default, which is {@code true}. */
    public boolean shouldTriggerClientSideContainerClosingOnOpen()
    {
        return val$gui != EnumGuiType.MainMenuInv;
    }
}
