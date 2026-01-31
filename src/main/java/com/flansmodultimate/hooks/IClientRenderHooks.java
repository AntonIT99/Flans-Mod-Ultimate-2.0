package com.flansmodultimate.hooks;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public interface IClientRenderHooks
{
    void initCustomBewlr(Consumer<IClientItemExtensions> consumer);
}
