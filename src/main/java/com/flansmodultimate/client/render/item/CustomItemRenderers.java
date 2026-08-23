package com.flansmodultimate.client.render.item;

import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.item.MechaItem;
import com.flansmodultimate.common.item.PlaneItem;
import com.flansmodultimate.common.item.VehicleItem;
import com.flansmodultimate.client.model.ModelCache;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CustomItemRenderers
{
    private static final Map<Class<?>, ICustomItemRenderer> RENDERERS = new ConcurrentHashMap<>();

    public static void registerAll()
    {
        register(GunItem.class, GunItemRenderer::renderItem);
        register(GrenadeItem.class, GrenadeItemRenderer::renderItem);
        register(PlaneItem.class, DriveableItemRenderer::renderItem);
        register(VehicleItem.class, DriveableItemRenderer::renderItem);
        register(MechaItem.class, DriveableItemRenderer::renderItem);
        register(MechaAddonItem.class, MechaAddonItemRenderer::renderItem);
    }

    public static void register(Class<? extends ICustomRendereredItem<?>> itemClass, ICustomItemRenderer renderer)
    {
        RENDERERS.put(itemClass, renderer);
    }

    public static ICustomItemRenderer get(Item item)
    {
        return RENDERERS.get(item.getClass());
    }

    public static boolean canRender(ItemStack stack, ItemDisplayContext context)
    {
        return stack.getItem() instanceof ICustomRendereredItem<?> item
            && item.useCustomRenderer(context)
            && get(stack.getItem()) != null
            && ModelCache.getOrLoadTypeModel(item.getConfigType()) != null;
    }
}
