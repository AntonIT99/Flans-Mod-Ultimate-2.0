package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.hooks.ClientHooks;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public interface ICustomRendereredItem<T extends InfoType> extends IFlanItem<T>
{
    /**
     * Override this method and reuse the default implementation to enable custom item rendering
     */
    default void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        ClientHooks.RENDER.initCustomBewlr(consumer);
    }

    boolean useCustomRendererInHand();

    boolean useCustomRendererOnGround();

    boolean useCustomRendererInFrame();

    boolean useCustomRendererInGui();
}
