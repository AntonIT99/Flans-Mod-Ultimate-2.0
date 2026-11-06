package com.flansmodultimate.platform.event;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.EntityCullingCompat;
import com.flansmodultimate.client.gui.ModMenuScreens;
import com.flansmodultimate.client.gui.options.FlansSettingsHubScreen;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.client.render.hud.ClientHudOverlays;
import com.flansmodultimate.platform.client.HudOverlayPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Forge client mod-bus subscribers whose events or registration hooks differ between loaders: the mod list config screen,
 * menu screens, compatibility setup, HUD overlays and shaders. The shared client setup stays in ModClientEventHandler.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents
{
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        // The Config button of the mod list opens the same screen as the pause menu button
        ModList.get().getModContainerById(FlansMod.MOD_ID).ifPresent(container -> container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new FlansSettingsHubScreen(parent))));

        event.enqueueWork(() ->
        {
            EntityCullingCompat.init();
            ModMenuScreens.register(MenuScreens::register);
        });
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        ClientHudOverlays.register(HudOverlayPlatform.registrar(event));
    }

    @SubscribeEvent
    public static void registerGpuModelShader(RegisterShadersEvent event)
    {
        GpuModelCache.registerShader(event);
    }
}
