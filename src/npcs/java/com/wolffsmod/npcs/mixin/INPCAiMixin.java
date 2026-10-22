package com.wolffsmod.npcs.mixin;

import noppes.npcs.api.entity.data.INPCAi;
import org.spongepowered.asm.mixin.Mixin;

/** Restores the upstream tactical scripting signatures on the older NeoForge AI interface. */
@Mixin(value = INPCAi.class, remap = false)
public interface INPCAiMixin
{
    // These are upstream API signatures, retained verbatim so existing Custom NPCs scripts can call them.
    int getTacticalType();

    void setTacticalType(int type);

    int getTacticalRange();

    void setTacticalRange(int range);
}
