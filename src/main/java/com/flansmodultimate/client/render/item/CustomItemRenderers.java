package com.flansmodultimate.client.render.item;

import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.item.MechaItem;
import com.flansmodultimate.common.item.PlaneItem;
import com.flansmodultimate.common.item.VehicleItem;
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
}
