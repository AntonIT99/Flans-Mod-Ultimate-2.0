package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.*;
import com.flansmodultimate.client.debug.*;
import com.flansmodultimate.client.distant.DistantBoxRenderer;
import com.flansmodultimate.client.distant.DistantHorizonsClient;
import com.flansmodultimate.client.gui.options.FlansOptionsScreen;
import com.flansmodultimate.client.gui.options.MenuButtonPlacement;
import com.flansmodultimate.client.input.*;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.client.render.OpStickConnectionRenderer;
import com.flansmodultimate.client.render.effects.*;
import com.flansmodultimate.client.render.entity.DriveableImpostorCache;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.client.render.hud.*;
import com.flansmodultimate.client.render.layer.PlayerSkinOverrides;
import com.flansmodultimate.client.render.thermal.ThermalHotParticles;
import com.flansmodultimate.client.render.thermal.VehicleThermalRenderer;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.driveables.collision.DriveableCollisionBypass;
import com.flansmodultimate.common.entity.*;
import com.flansmodultimate.common.guns.EnumFunction;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.config.*;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.driveable.PacketDriveableDamage;
import com.flansmodultimate.network.server.driveable.PacketRequestDismount;
import com.flansmodultimate.platform.client.ClientPlatform;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEventHandler
{
    /**
     * The mob model whose arm poses {@link #applyGunArmPoses} changed for the entity being rendered. A mob
     * renderer shares one model between all its entities and, unlike the player renderer, does not set its
     * arm poses every frame, so they are put back once the entity is drawn.
     */
    @Nullable
    private static HumanoidModel<?> posedMobModel;
    private static HumanoidModel.ArmPose savedRightArmPose;
    private static HumanoidModel.ArmPose savedLeftArmPose;

    @SubscribeEvent
    public static void onComputeCameraFov(ViewportEvent.ComputeFov event)
    {
        double fov = ModClient.cameraFov(event.getFOV(), event.getPartialTick());
        ModClientConfig config = ModClientConfig.get();
        // The firing punch narrows the world view only, as 1.7.10 left the hand at its own fixed field of view
        if (event.usedConfiguredFov() && config != null && config.gunScreenShake)
            fov = Math.max(1D, fov + GunScreenShake.fovOffset((float) event.getPartialTick()));
        event.setFOV(fov);
    }

    /** Adds the mod's options button to the vanilla options screen and pause menu, as configured. */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event)
    {
        ModClientConfig config = ModClientConfig.get();
        if (config == null)
            return;

        Screen screen = event.getScreen();
        boolean wanted = screen instanceof OptionsScreen && config.optionsButtonPlacement.inOptionsScreen() || screen instanceof PauseScreen && config.optionsButtonPlacement.inPauseMenu();
        if (!wanted)
            return;

        List<MenuButtonPlacement.Rect> buttons = event.getListenersList().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .map(button -> new MenuButtonPlacement.Rect(button.getX(), button.getY(), button.getWidth(), button.getHeight())).toList();

        // The pause screen shown while the game is still loading has no menu to hang the button on
        MenuButtonPlacement.Placement placement = MenuButtonPlacement.compute(buttons, screen.width, screen.height);
        if (placement == null)
            return;

        // The row the button takes over, and everything below it, moves down to make room
        for (GuiEventListener listener : event.getListenersList())
        {
            if (listener instanceof AbstractWidget widget && widget.getY() >= placement.shiftFromY())
                widget.setY(widget.getY() + placement.shiftBy());
        }

        event.addListener(Button.builder(Component.translatable("gui.flansmodultimate.options.menu_button"), button -> FlansOptionsScreen.open())
            .bounds(placement.x(), placement.y(), placement.width(), placement.height()).build());
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        Entity cameraEntity = event.getCamera().getEntity();
        var view = MountedCameraView.resolve(cameraEntity, (float) event.getPartialTick());
        if (view != null)
        {
            // The reversed third person view turns the camera around, which swaps
            // which way the driveable's roll leans on screen.
            boolean frontView = Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_FRONT;
            event.setYaw(Mth.wrapDegrees(frontView ? view.yaw() + 180F : view.yaw()));
            event.setPitch(Mth.clamp(frontView ? -view.pitch() : view.pitch(), -89.9F, 89.9F));
            event.setRoll(event.getRoll() + (frontView ? -view.roll() : view.roll()));
        }
        applyVehicleScreenShake(event);
        applyGunScreenShake(event);
    }

    /** Adds the camera kick of a screen-shaking gun the player just fired. 1.7.10 applied it through view bobbing. */
    private static void applyGunScreenShake(ViewportEvent.ComputeCameraAngles event)
    {
        ModClientConfig config = ModClientConfig.get();
        if (config == null || !config.gunScreenShake || !Boolean.TRUE.equals(Minecraft.getInstance().options.bobView().get()))
            return;
        float pitch = GunScreenShake.pitchOffset((float) event.getPartialTick());
        if (pitch != 0F)
            event.setPitch(Mth.clamp(event.getPitch() + pitch, -89.9F, 89.9F));
    }

    /** Adds the camera kick of a nearby firing driveable on top of whatever angles the view already has. */
    private static void applyVehicleScreenShake(ViewportEvent.ComputeCameraAngles event)
    {
        ModClientConfig config = ModClientConfig.get();
        if (config == null || !config.vehicleScreenShake)
            return;
        long now = Util.getMillis();
        float pitch = VehicleScreenShake.pitchOffset(now);
        float roll = VehicleScreenShake.rollOffset(now);
        if (pitch == 0F && roll == 0F)
            return;
        event.setPitch(Mth.clamp(event.getPitch() + pitch, -89.9F, 89.9F));
        event.setRoll(event.getRoll() + roll);
    }

    /**
     * Runs at the start of every client tick. Vanilla consumes its own key clicks later in the same tick,
     * so a driveable bind has to claim a shared key before that happens.
     */
    public static void onClientTickStart()
    {
        KeyInputHandler.claimConflictingVanillaKeys();
    }

    /** Runs at the end of every client tick. */
    public static void onClientTick()
    {
        PacketDriveableDamage.applyPending(Minecraft.getInstance().level);
        GunInputState.tick();
        GrenadeThrowInput.tick();
        GunScreenShake.tick();
        ModClient.tick();
        ParticleHelper.tick();
        DistantHorizonsClient.tick();
    }

    /** Runs once per rendered frame, before the frame, with the main render target bound. */
    public static void onRenderFrameStart()
    {
        GpuModelCache.beginFrame();
        DriveableImpostorCache.capturePending();
    }

    /** Runs once per rendered frame, after the frame. */
    public static void onRenderTick()
    {
        ModClient.renderTick();
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
            double scrollDelta = ClientPlatform.scrollDelta(event);

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
        VehicleThermalRenderer.render(event);
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            DistantBoxRenderer.render(event.getPoseStack(), event.getCamera(), ClientPlatform.partialTick(event));
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        float partialTick = ClientPlatform.partialTick(event);
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        InstantBulletRenderer.renderAllTrails(event.getPoseStack(), buffers, partialTick, event.getCamera());
        OpStickConnectionRenderer.render(event.getPoseStack(), buffers, event.getCamera(), partialTick);

        if (ModClient.isDebug())
        {
            for (DebugColor debugEntity : DebugHelper.getActiveDebugEntities())
            {
                if (event.getFrustum().isVisible(debugEntity.getAABB()))
                    debugEntity.render(event.getPoseStack(), buffers, event.getCamera());
            }
            // Flush now, while everything drawn so far (entities included) is already on screen to draw over
            buffers.endBatch(CustomRenderType.debugFilledBoxSeeThrough());
            DriveableHitboxRenderer.renderAll(event.getPoseStack(), buffers, event.getCamera(), event.getFrustum(), partialTick);
            PlayerHitboxRenderer.renderAll(event.getPoseStack(), buffers, event.getCamera(), event.getFrustum(), partialTick);
        }
    }

    /** Whether the vanilla crosshair is replaced by the hit marker alone for the current frame. */
    public static boolean hidesCrosshair()
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return false;

        // Remove crosshairs for config option, gun config, or if looking down the sights of a gun
        boolean holdingNonMeleeGun = ModUtils.hasGunItemInHands(player) && !ModUtils.getGunItemsInHands(player).stream().allMatch(gunItem -> gunItem.getConfigType().getPrimaryFunction().isMelee());
        boolean gunConfigHidesCrosshair = ModUtils.getGunItemsInHands(player).stream().anyMatch(gunItem -> !gunItem.getConfigType().shouldShowCrosshair());
        return VehicleOpticsClient.activeSeat() != null && !VehicleOpticsClient.activeSeat().getOptics().isShowCrosshair() || ModClient.getCurrentScope() != null || gunConfigHidesCrosshair
            || ((ModCommonConfig.get().disableCrosshairForGuns() || ModClientConfig.get().hideCrosshairForGuns) && holdingNonMeleeGun);
    }

    /** Draws the hit marker where the crosshair is, whether or not the crosshair itself is shown. */
    public static void renderCrosshairHitMarker(GuiGraphics graphics, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        ClientHudOverlays.renderHitMarker(graphics, partialTick, w, h);
    }

    /** Set up RenderContext for gun animations and set the gun arm poses of players and humanoid mobs */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event)
    {
        // Vanilla invisibility only hides the body and still draws armor and held
        // items. Canceling here skips every layer; Post is not fired, so do it
        // before the render context is set.
        if (Driveable.isRiderHiddenByDriveable(event.getEntity()))
        {
            event.setCanceled(true);
            return;
        }

        ModClient.entityRenderContext.set(event.getEntity());

        LivingEntity entity = event.getEntity();
        if (event.getRenderer().getModel() instanceof HumanoidModel<?> humanoid && !(entity instanceof ArmorStand))
            applyGunArmPoses(entity, humanoid);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> e)
    {
        ModClient.entityRenderContext.remove();

        if (posedMobModel != null && e.getRenderer().getModel() == posedMobModel)
        {
            posedMobModel.rightArmPose = savedRightArmPose;
            posedMobModel.leftArmPose = savedLeftArmPose;
        }
        posedMobModel = null;
    }

    /** Replaces the arm pose of each arm holding a raised gun or shield, leaving the others to vanilla. */
    private static void applyGunArmPoses(LivingEntity entity, HumanoidModel<?> humanoid)
    {
        GunArmPoses.Result poses = GunArmPoses.resolve(entity);
        HumanoidModel.ArmPose mainPose = armPose(poses.mainHand());
        HumanoidModel.ArmPose offPose = armPose(poses.offHand());
        if (mainPose == null && offPose == null)
            return;

        if (!(entity instanceof Player))
        {
            posedMobModel = humanoid;
            savedRightArmPose = humanoid.rightArmPose;
            savedLeftArmPose = humanoid.leftArmPose;
        }

        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        if (mainPose != null)
        {
            if (rightHanded)
                humanoid.rightArmPose = mainPose;
            else
                humanoid.leftArmPose = mainPose;
        }
        if (offPose != null)
        {
            if (rightHanded)
                humanoid.leftArmPose = offPose;
            else
                humanoid.rightArmPose = offPose;
        }
    }

    @Nullable
    private static HumanoidModel.ArmPose armPose(GunArmPoses.Arm arm)
    {
        return switch (arm)
        {
            case NONE -> null;
            case ONE_ARM -> ModClient.oneArmAim;
            case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
            case BOTH -> ModClient.bothArmsAim;
            case THROW -> ModClient.oneArmThrow;
            case SUPPORT -> ModClient.bowSupport;
        };
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        // Driveable actions are sampled into one compact, server-validated input
        // packet. Prevent vanilla attack/use/pick handling from firing in parallel.
        if (KeyInputHandler.resolveDriveable(player) != null)
        {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }

        // AA guns read the fire button directly in ClientGunHooksImpl, so canceling the
        // vanilla attack here only suppresses the player's hand swing and melee attack.
        if (player.getVehicle() instanceof AAGun && event.isAttack())
        {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }

        // Block all interactions unless it is 'use item' to dismount deployable guns
        if (player.getVehicle() instanceof DeployedGun)
        {
            if (event.isUseItem())
            {
                player.stopRiding();
                PacketHandler.sendToServer(new PacketRequestDismount());
            }
            else
            {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
            return;
        }

        // Hold-to-throw grenades read both buttons themselves in GrenadeThrowInput and throw on release
        if ((event.isAttack() || event.isUseItem()) && GrenadeThrowInput.isHoldingHoldToThrowGrenade(player))
        {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }

        // An off-hand gun held alone may aim with the free main hand's button, whose attack is reported for the main hand
        boolean mainHandEmpty = player.getMainHandItem().isEmpty();
        InteractionHand hand = event.isAttack() && mainHandEmpty && player.getOffhandItem().getItem() instanceof GunItem ? InteractionHand.OFF_HAND : event.getHand();

        if (player.getItemInHand(hand).getItem() instanceof GunItem gunItem && !gunItem.getConfigType().isDeployable())
        {
            // Aiming is a right-click, so a player lining up a shot at a chest opens it instead.
            // This suppresses the block, not the aim, which is read from the key itself.
            if (event.isUseItem() && isBlockUseSuppressed(player, mc.hitResult))
            {
                event.setCanceled(true);
                event.setSwingHand(false);
                return;
            }

            EnumMouseButton primaryButton = hand == InteractionHand.OFF_HAND ? ModClientConfig.get().shootButtonOffhand : ModClientConfig.get().shootButton;
            EnumMouseButton secondaryButton = GunInputState.getSecondaryButton(hand, mainHandEmpty);

            boolean isPrimaryButton = event.getKeyMapping().getKey().getValue() == primaryButton.toGlfw();
            boolean isSecondaryButton = event.getKeyMapping().getKey().getValue() == secondaryButton.toGlfw();
            EnumFunction primaryFunction = gunItem.getConfigType().getPrimaryFunction();
            EnumFunction secondaryFunction = gunItem.getConfigType().getSecondaryFunction();

            // A throw is charged through the vanilla use action, so it must reach the item. Canceling the
            // main hand's use also stops vanilla before it tries the off hand, so an off-hand throwable
            // behind a main-hand gun or shield must let the use through as well.
            if (event.isUseItem() && hand == InteractionHand.MAIN_HAND && isThrowable(player.getOffhandItem()))
                return;

            if (isSecondaryButton && secondaryFunction != EnumFunction.MELEE && secondaryFunction != EnumFunction.THROW)
            {
                // Aiming with the attack button must not break the block or hit the entity under the crosshair
                if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS || event.isAttack())
                {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                }
            }
            else if (isPrimaryButton && primaryFunction != EnumFunction.MELEE)
            {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }
    }

    private static boolean isThrowable(ItemStack stack)
    {
        return stack.getItem() instanceof GunItem gunItem && gunItem.getConfigType().isThrowable();
    }

    /**
     * Whether the player's own preference says to leave the block they are looking at alone while
     * they are armed.
     *
     * <p>
     * Sneaking is the way through {@link EnumGunBlockInteraction#NO_CONTAINERS}, so a chest can
     * still be opened without putting the gun away; that is what {@code GunItem.doesSneakBypassUse}
     * already allows for. {@link EnumGunBlockInteraction#NONE} has no way through on purpose: a
     * player who asked for nothing to be used while armed means it.
     */
    private static boolean isBlockUseSuppressed(Player player, @Nullable HitResult hitResult)
    {
        EnumGunBlockInteraction policy = ModClientConfig.get().gunBlockInteraction;
        if (policy == EnumGunBlockInteraction.ALLOW || !(hitResult instanceof BlockHitResult blockHit))
            return false;

        if (policy == EnumGunBlockInteraction.NONE)
            return true;

        if (player.isShiftKeyDown())
            return false;

        Level level = player.level();
        BlockPos pos = blockHit.getBlockPos();
        return level.getBlockState(pos).getMenuProvider(level, pos) != null;
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event)
    {
        ReloadPreferencesSync.sendToServer();
        AimPoseClient.sendToServer();
        // Fetched up front so the options screen can show the server settings without a visible delay
        CommonConfigMirror.request();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        com.flansmodultimate.network.client.debug.PacketDebugHitboxes.clearSession();
        DriveableCollisionBypass.reset();
        VehicleOpticsClient.reset();
        VehicleThermalRenderer.reset();
        ThermalHotParticles.reset();
        VehicleScreenShake.reset();
        GunScreenShake.reset();
        GrenadeThrowInput.reset();
        DistantHorizonsClient.reset();
        ModClient.clearTransientLighting();
        GpuModelCache.clear();
        DebugHelper.getActiveDebugEntities().clear(); // cleanup on world/connection change
        TeamsClientState.clear();
        PlayerSkinOverrides.clear();
        KillMessageFeed.clear();
        CommonConfigMirror.clear();
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event)
    {
        if (TeamsClientState.shouldHidePlayer(event.getEntity()))
        {
            event.setCanceled(true);
            return;
        }

        Player player = event.getEntity();
        if (!(player.getVehicle() instanceof Seat seat) || seat.getDriveable() == null)
            return;

        float partialTick = event.getPartialTick();
        Vec3 renderedFeet = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()), Mth.lerp(partialTick, player.yo, player.getY()), Mth.lerp(partialTick, player.zo, player.getZ()));
        Vec3 seatFeet = seat.getDriveable().getInterpolatedRiderWorldPosition(seat.getSeatIndex(), seat.getPassengerRidingOffset(player), partialTick);
        Vec3 correction = seatFeet.subtract(renderedFeet);
        event.getPoseStack().translate(correction.x, correction.y, correction.z);
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event)
    {
        if (VehicleThermalRenderer.isRenderingMask())
        {
            ClientPlatform.hideNameTag(event);
            return;
        }
        if (event.getEntity() instanceof Player player && TeamsClientState.shouldHideNameTag(player))
            ClientPlatform.hideNameTag(event);
    }

    @SubscribeEvent
    public static void onRenderScopedHand(RenderHandEvent event)
    {
        if (VehicleOpticsClient.activeSeat() != null)
            event.setCanceled(true);
    }
}
