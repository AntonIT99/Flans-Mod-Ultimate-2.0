package com.flansmodultimate.platform.event;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.EntityCullingCompat;
import com.flansmodultimate.client.gui.ModMenuScreens;
import com.flansmodultimate.client.gui.options.FlansSettingsHubScreen;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.client.render.hud.ClientHudOverlays;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.platform.client.HudOverlayPlatform;
import com.flansmodultimate.platform.registry.RegistryEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import net.minecraft.world.item.Item;

/**
 * NeoForge client mod-bus subscribers whose events or registration hooks differ between loaders: the mod list config screen,
 * menu screens, compatibility setup, item client extensions, HUD layers and shaders. The shared client setup stays in
 * ModClientEventHandler.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = FlansMod.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents
{
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        // The Config button of the mod list opens the same screen as the pause menu button
        ModList.get().getModContainerById(FlansMod.MOD_ID)
            .ifPresent(container -> container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) (mod, parent) -> new FlansSettingsHubScreen(parent)));
    }

    /** EntityCulling on NeoForge creates its instance during client setup, which is complete by now. */
    @SubscribeEvent
    public static void loadComplete(FMLLoadCompleteEvent event)
    {
        event.enqueueWork(EntityCullingCompat::init);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event)
    {
        ModMenuScreens.register(event::register);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event)
    {
        Item[] customRenderedItems = FlansMod.getItems().stream().map(RegistryEntry::get).filter(ICustomRendereredItem.class::isInstance).toArray(Item[]::new);
        if (customRenderedItems.length > 0)
            event.registerItem(ClientHooks.RENDER.customItemExtensions(), customRenderedItems);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event)
    {
        ClientHudOverlays.register(HudOverlayPlatform.registrar(event));
    }

    @SubscribeEvent
    public static void registerGpuModelShader(RegisterShadersEvent event)
    {
        GpuModelCache.registerShader(event);
    }
}
