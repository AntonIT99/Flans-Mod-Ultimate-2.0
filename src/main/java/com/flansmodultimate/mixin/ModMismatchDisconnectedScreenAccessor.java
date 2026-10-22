package com.flansmodultimate.mixin;

import net.minecraftforge.client.gui.ModMismatchDisconnectedScreen;
import net.minecraftforge.network.ConnectionData.ModMismatchData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Reads the disconnect context before Forge builds its mismatch table, which requires mod metadata for every namespace. */
@Mixin(value = ModMismatchDisconnectedScreen.class, remap = false)
public interface ModMismatchDisconnectedScreenAccessor
{
    @Accessor("parent")
    Screen flansmodultimateParent();

    @Accessor("reason")
    Component flansmodultimateReason();

    @Accessor("modMismatchData")
    ModMismatchData flansmodultimateMismatchData();
}
