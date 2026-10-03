package com.flansmodultimate.platform.client;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** The loader's client item extensions, under a name shared code can use on every loader. */
public interface FlanItemExtensions extends IClientItemExtensions
{
    /** Extensions that change nothing. */
    FlanItemExtensions NONE = new FlanItemExtensions() {};
}
