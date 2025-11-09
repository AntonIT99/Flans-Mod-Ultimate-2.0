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

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = ArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEventHandler
{
    @SubscribeEvent
    public static void onComputeCameraFov(ViewportEvent.ComputeFov event)
    {
        ModelGun.setSmoothing((float) event.getPartialTick());
        ModClient.updateCameraZoom(event);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        ModClient.updateCameraAngles(event);
        //TODO: for driveables
        //updatePlayerView();
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

        if (mainHand.getItem() instanceof GunItem gunItem)
        {
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

    /** Render world-space geometry AFTER particles/translucents so the trail blends nicely. */
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

    /** Set up RenderContext for gun animations and set Aim Pose when GunItem is held by players */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event)
    {
        ModClient.entityRenderContext.set(event.getEntity());

        if (!(event.getEntity() instanceof Player player))
            return;

        var model = event.getRenderer().getModel();
        if (!(model instanceof HumanoidModel<?> humanoid))
            return;

        ItemStack main = event.getEntity().getMainHandItem();
        ItemStack off  = event.getEntity().getOffhandItem();
        boolean mainArmPose = isGunItemWithAiming(main);
        boolean offArmPose = isGunItemWithAiming(off);

        if (mainArmPose && offArmPose)
        {
            humanoid.leftArmPose  = ModClient.bothArmsAim;
            humanoid.rightArmPose = ModClient.bothArmsAim;
        }
        else if (mainArmPose)
        {
            if (player.getMainArm() == HumanoidArm.RIGHT)
                humanoid.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            else
                humanoid.leftArmPose  = HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        else if (offArmPose)
        {
            if (player.getMainArm() == HumanoidArm.RIGHT)
                humanoid.leftArmPose  = HumanoidModel.ArmPose.BOW_AND_ARROW;
            else
                humanoid.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> e)
    {
        ModClient.entityRenderContext.remove();
    }

    private static boolean isGunItemWithAiming(ItemStack s)
    {
        return !s.isEmpty() && s.getItem() instanceof GunItem gunItem && gunItem.useAimingAnimation();
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        KeyMapping attack = mc.options.keyAttack;
        if (event.getKeyMapping() != attack)
            return;

        ItemStack main = mc.player.getMainHandItem();
        ItemStack off  = mc.player.getOffhandItem();
        boolean holdingGun = (main.getItem() instanceof GunItem) || (off.getItem() instanceof GunItem);
        if (!holdingGun)
            return;

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    //TODO:
    //renderItemFrame()
    //renderHeldItem()
    //renderThirdPersonWeapons()
    //renderPlayer()
    //cameraSetup()
}
