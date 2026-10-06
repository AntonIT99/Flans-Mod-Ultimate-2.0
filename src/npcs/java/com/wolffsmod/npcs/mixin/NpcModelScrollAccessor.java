package com.wolffsmod.npcs.mixin;

import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Preserves the model picker's viewport while Custom NPCs rebuilds its rows. */
@Mixin(value = GuiCustomScrollNop.class, remap = false)
@SuppressWarnings("java:S100") // Accessor names use a module prefix to avoid collisions with other mixins.
public interface NpcModelScrollAccessor
{
    @Accessor("scrollY")
    int wolffsmodnpcs$getScrollY();

    @Accessor("scrollY")
    void wolffsmodnpcs$setScrollY(int scrollY);
}
