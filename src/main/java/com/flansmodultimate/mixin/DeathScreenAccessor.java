package com.flansmodultimate.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;

/** Reads the death message, which the Teams deployment map shows before it takes the death screen's place. */
@Mixin(DeathScreen.class)
public interface DeathScreenAccessor
{
    @Accessor("causeOfDeath")
    Component flansmodultimateCauseOfDeath();
}
