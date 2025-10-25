package com.wolffsarmormod.event;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.client.ClientHudOverlays;
import com.wolffsarmormod.client.InstantBulletRenderer;
import com.wolffsarmormod.common.item.GunItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = ArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler
{
    /**
     * Set gui overlay for scopes and armors
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event)
    {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()))
            return;

        // Cancel crosshair when scoped (your old behavior)
        if (ModClient.getCurrentScope() != null)
            event.setCanceled(true);

        // Hit marker here (draws even if we canceled crosshair)
        ClientHudOverlays.renderHitMarker(event.getGuiGraphics(), event.getPartialTick(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }

    /**
     * Aim Pose when GunItem is held by a humanoid entity
     */
    @SubscribeEvent
    public static void onLiving(RenderLivingEvent.Pre<?, ?> event)
    {
        var model = event.getRenderer().getModel();
        if (!(model instanceof HumanoidModel<?> humanoid))
            return;

        ItemStack main = event.getEntity().getMainHandItem();
        ItemStack off  = event.getEntity().getOffhandItem();
        boolean force = isGunItem(main) || isGunItem(off);
        if (!force)
            return;

        // Force the bow-aiming arm pose on both arms
        humanoid.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        humanoid.leftArmPose  = HumanoidModel.ArmPose.BOW_AND_ARROW;
    }

    private static boolean isGunItem(ItemStack s)
    {
        return !s.isEmpty() && s.getItem() instanceof GunItem;
    }

    // Render world-space geometry AFTER particles/translucents so the trail blends nicely.
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        InstantBulletRenderer.renderAllTrails(event.getPoseStack(), event.getPartialTick(), event.getCamera());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            //TODO
            //ModClient.updateFlashlights(Minecraft.getMinecraft());
        }
        else if (event.phase == TickEvent.Phase.END)
        {
            InstantBulletRenderer.updateAllTrails();
            ModClient.tick();
        }
    }
}
