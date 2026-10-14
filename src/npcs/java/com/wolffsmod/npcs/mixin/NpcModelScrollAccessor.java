package com.wolffsmod.npcs.mixin;

import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the model picker's viewport, hovered row and search matching for full-name tooltips. */
@Mixin(value = GuiCustomScrollNop.class, remap = false)
public interface NpcModelScrollAccessor
{
    @Accessor("scrollY")
    int wolffsmodnpcsGetScrollY();

    @Accessor("scrollY")
    void wolffsmodnpcsSetScrollY(int scrollY);

    @Accessor("hover")
    int wolffsmodnpcsGetHover();

    @Accessor("scrollHeight")
    int wolffsmodnpcsGetScrollHeight();

    @Invoker("isSearched")
    boolean wolffsmodnpcsIsSearched(String label);
}
