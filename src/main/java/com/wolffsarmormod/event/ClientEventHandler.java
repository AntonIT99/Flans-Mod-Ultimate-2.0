package com.wolffsarmormod.event;

import com.flansmod.client.model.ModelGun;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.client.input.KeyInputHandler;
import com.wolffsarmormod.client.input.MouseInputHandler;
import com.wolffsarmormod.client.render.ClientHudOverlays;
import com.wolffsarmormod.client.render.InstantBulletRenderer;
import com.wolffsarmormod.common.item.GunItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = ArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEventHandler
{
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeFov event)
    {
        ModelGun.setSmoothing((float) event.getPartialTick());
        ModClient.updateCameraZoom(event);
        //TODO: for driveables
        //renderHooks.updatePlayerView();
    }

    //TODO: complete commented code
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            //ModClient.updateFlashlights(Minecraft.getMinecraft());
        }
        else if (event.phase == TickEvent.Phase.END)
        {
            InstantBulletRenderer.updateAllTrails();
            //renderHooks.update();
            //RenderFlag.angle += 2F;
            ModClient.tick();
            KeyInputHandler.checkKeys();

            double dx = Minecraft.getInstance().mouseHandler.getXVelocity();
            double dy = Minecraft.getInstance().mouseHandler.getYVelocity();
            if (dx * dx + dy * dy > 0.001)
            {
                // Only handle if the velocity vector is not too small
                MouseInputHandler.handleMouseMove(dx, dy);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.getItem() instanceof GunItem gunItem) {

            boolean isOneHanded = gunItem.getConfigType().isOneHanded();
            boolean isSneakingKeyDown = Minecraft.getInstance().options.keyShift.isDown();
            double scrollDelta = event.getScrollDelta();

            if (isOneHanded && isSneakingKeyDown && Math.abs(scrollDelta) > 0.0D)
            {
                // Block vanilla handling (e.g. prevent hotbar slot scroll) when sneaking with a gun
                event.setCanceled(true);
            }
        }
    }

    /**
     * Render world-space geometry AFTER particles/translucents so the trail blends nicely.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        InstantBulletRenderer.renderAllTrails(event.getPoseStack(), event.getPartialTick(), event.getCamera());
    }

    /** CROSSHAIR: pre = we can cancel vanilla*/
    @SubscribeEvent
    public static void onPreRenderGuiOverlay(RenderGuiOverlayEvent.Pre event)
    {
        Minecraft mc = Minecraft.getInstance();

        // Remove crosshairs if looking down the sights of a gun
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() && ModClient.getCurrentScope() != null)
        {
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            ClientHudOverlays.renderHitMarker(event.getGuiGraphics(), event.getPartialTick(), w, h);
            event.setCanceled(true);
        }
    }

    /** CROSSHAIR: post = draw hit marker overlay */
    @SubscribeEvent
    public static void onPostRenderGuiOverlay(RenderGuiOverlayEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();

        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type())
        {
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            ClientHudOverlays.renderHitMarker(event.getGuiGraphics(), event.getPartialTick(), w, h);
        }
    }

    /**
     * HELMET overlay (post) → scope overlay
     * HOTBAR overlay (post) → ammo, team info, killfeed, vehicle debug
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        /*if (event.getOverlay() == VanillaGuiOverlay.HELMET.type())
        {
            ClientHudOverlays.renderScopeOverlay(guiGraphics, w, h);
        }*/
        //TODO: check if it can be done via registration
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type())
        {
            ClientHudOverlays.renderPlayerAmmo(guiGraphics, w, h);
            ClientHudOverlays.renderTeamInfo(guiGraphics, w, h);
            ClientHudOverlays.renderKillMessages(guiGraphics, w, h);
            ClientHudOverlays.renderVehicleDebug(guiGraphics, w, h);
        }
    }

    /**
     * Aim Pose when GunItem is held by players
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event)
    {
        if (!(event.getEntity() instanceof Player))
            return;

        var model = event.getRenderer().getModel();
        if (!(model instanceof HumanoidModel<?> humanoid))
            return;

        ItemStack main = event.getEntity().getMainHandItem();
        ItemStack off  = event.getEntity().getOffhandItem();
        boolean force = isGunItemWithAiming(main) || isGunItemWithAiming(off);
        if (!force)
            return;

        // Force the bow-aiming arm pose on both arms
        humanoid.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        humanoid.leftArmPose  = HumanoidModel.ArmPose.BOW_AND_ARROW;
    }

    private static boolean isGunItemWithAiming(ItemStack s)
    {
        return !s.isEmpty() && s.getItem() instanceof GunItem gunItem && gunItem.useAimingAnimation();
    }

    //TODO:
    //renderItemFrame()
    //renderHeldItem()
    //renderThirdPersonWeapons()
    //renderPlayer()
    //cameraSetup()
}
