package com.wolffsmod.npcs.mixin;

import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Preserves the model picker's viewport while Custom NPCs rebuilds its rows. */
@Mixin(value = GuiCustomScrollNop.class, remap = false)
public interface NpcModelScrollAccessor
{
    @Accessor("scrollY")
    int wolffsmodnpcsGetScrollY();

    @Accessor("scrollY")
    void wolffsmodnpcsSetScrollY(int scrollY);
}
