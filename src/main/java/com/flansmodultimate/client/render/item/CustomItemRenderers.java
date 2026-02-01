package com.flansmodultimate.client.render.item;

import com.flansmod.client.model.ModelGun;
import com.flansmod.client.model.RenderGun;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CustomItemRenderers
{
    public static final ThreadLocal<Boolean> SKIP_BEWLR = ThreadLocal.withInitial(() -> false);

    private static final Map<Class<?>, ICustomItemRenderer> RENDERERS = new ConcurrentHashMap<>();

    public static void registerAll()
    {
        register(GunItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (stack.getItem() instanceof GunItem gunItem && ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun modelGun)
                RenderGun.renderItem(modelGun, stack, ctx, pose, buffer, light, overlay);
            else
            {
                ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
            }

        });
        register(GrenadeItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (stack.getItem() instanceof GrenadeItem grenadeItem)
            {
                IModelBase model = ModelCache.getOrLoadTypeModel(grenadeItem.getConfigType());
                if (model != null)
                {
                    int color = grenadeItem.getConfigType().getColour();
                    float red = (color >> 16 & 255) / 255F;
                    float green = (color >> 8 & 255) / 255F;
                    float blue = (color & 255) / 255F;
                    LegacyTransformApplier.renderModel(model, grenadeItem.getConfigType(), grenadeItem.getConfigType().getTexture(), pose, buffer, light, overlay, red, green, blue, 1F);
                }
                else
                {
                    ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
                }
            }
        });
    }

    public static void register(Class<? extends ICustomRendereredItem<?>> itemClass, ICustomItemRenderer renderer)
    {
        RENDERERS.put(itemClass, renderer);
    }

    public static ICustomItemRenderer get(Item item)
    {
        return RENDERERS.get(item.getClass());
    }
}
