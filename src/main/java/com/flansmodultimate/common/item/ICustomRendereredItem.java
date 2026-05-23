package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.hooks.ClientHooks;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.world.item.ItemDisplayContext;

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

    default boolean useCustomRenderer(ItemDisplayContext itemDisplayContext)
    {
        return switch (itemDisplayContext)
        {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> useCustomRendererInHand();
            case GROUND -> useCustomRendererOnGround();
            case FIXED -> useCustomRendererInFrame();
            case GUI -> useCustomRendererInGui();
            default -> false;
        };
    }
}
