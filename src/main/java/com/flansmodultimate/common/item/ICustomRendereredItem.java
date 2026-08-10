package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.InfoType;
import net.minecraft.world.item.ItemDisplayContext;

public interface ICustomRendereredItem<T extends InfoType> extends IFlanItem<T>
{
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
