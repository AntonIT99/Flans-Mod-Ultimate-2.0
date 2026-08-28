package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.debug.DebugColor;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.input.EnumMouseButton;
import com.flansmodultimate.client.input.GunInputState;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.client.render.ClientHudOverlays;
import com.flansmodultimate.client.render.InstantBulletRenderer;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.guns.EnumFunction;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.raytracing.EnumHitboxType;
import com.flansmodultimate.common.raytracing.PlayerHitbox;
import com.flansmodultimate.common.raytracing.PlayerSnapshot;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketRequestDismount;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEventHandler
{
    @SubscribeEvent
    public static void onComputeCameraFov(ViewportEvent.ComputeFov event)
    {
        ModClient.updateCameraZoom(event);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        var controllable = KeyInputHandler.resolveControllable(player);
        if (controllable == null)
            return;

        float partialTick = (float) event.getPartialTick();
        var driveable = KeyInputHandler.resolveDriveable(player);
        if (driveable != null && player.getVehicle() instanceof Seat seat)
        {
            boolean fixedPlaneView = driveable instanceof Plane && seat.isDriverSeat() && ModClient.isMouseControlEnabled();
            if (fixedPlaneView)
            {
                float cameraYaw = LegacyDriveableCoordinates.planeForwardYaw(
                    Mth.rotLerp(partialTick, driveable.getPrevYaw(), driveable.getYaw()));
                float cameraPitch = Mth.rotLerp(partialTick, driveable.getPrevPitch(), driveable.getPitch());
                if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_FRONT)
                {
                    cameraYaw += 180F;
                    cameraPitch = -cameraPitch;
                }
                event.setYaw(Mth.wrapDegrees(cameraYaw));
                event.setPitch(Mth.clamp(cameraPitch, -89.9F, 89.9F));
            }
        }

        float roll = Mth.rotLerp(partialTick, controllable.getPrevPlayerRoll(), controllable.getPlayerRoll());
        if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_FRONT)
            roll = -roll;
        event.setRoll(event.getRoll() + roll);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        GunInputState.tick();
        ModClient.tick();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

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

        if (ModClient.isDebug())
        {
            for (DebugColor debugEntity : DebugHelper.getActiveDebugEntities())
            {
                if (event.getFrustum().isVisible(debugEntity.getAABB()))
                    debugEntity.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), event.getCamera());
            }
        }
    }

    /** CROSSHAIR: pre = we can cancel vanilla*/
    @SubscribeEvent
    public static void onPreRenderGuiOverlay(RenderGuiOverlayEvent.Pre event)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        // Remove crosshairs for config option, gun config, or if looking down the sights of a gun
        boolean holdingNonMeleeGun = ModUtils.hasGunItemInHands(player) && !ModUtils.getGunItemsInHands(player).stream().allMatch(gunItem -> gunItem.getConfigType().getPrimaryFunction().isMelee());
        boolean gunConfigHidesCrosshair = ModUtils.getGunItemsInHands(player).stream().anyMatch(gunItem -> !gunItem.getConfigType().shouldShowCrosshair());
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()
            && (ModClient.getCurrentScope() != null || gunConfigHidesCrosshair || (ModCommonConfig.get().disableCrosshairForGuns() && holdingNonMeleeGun)))
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

        // Render debug boxes for player snapshots
        if (ModClient.isDebug())
        {
            PlayerData data = PlayerData.getInstance(player , LogicalSide.CLIENT);
            if (data.getSnapshots()[0] != null)
                renderSnapshot(data.getSnapshots()[0]);
        }

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

    private static void renderSnapshot(PlayerSnapshot snapshot)
    {
        for (PlayerHitbox hitbox : snapshot.hitboxes)
            renderHitbox(hitbox, snapshot.pos);
    }

    private static void renderHitbox(PlayerHitbox hitbox, Vector3f pos)
    {
        if (!ModClient.isDebug() || hitbox.type != EnumHitboxType.RIGHTARM)
            return;

        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                for(int k = 0; k < 3; k++)
                {
                    Vector3f point = new Vector3f(hitbox.o.x + hitbox.d.x * i / 2, hitbox.o.y + hitbox.d.y * j / 2, hitbox.o.z + hitbox.d.z * k / 2);
                    point = hitbox.axes.findLocalVectorGlobally(point);
                    DebugHelper.spawnDebugDot(new Vec3(pos.x + hitbox.rP.x + point.x, pos.y + hitbox.rP.y + point.y, pos.z + hitbox.rP.z + point.z), 1, 0F, 1F, 0F);
                }
            }
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

        if (player.getItemInHand(event.getHand()).getItem() instanceof GunItem gunItem && !gunItem.getConfigType().isDeployable())
        {
            EnumMouseButton primaryButton = event.getHand() == InteractionHand.OFF_HAND ? ModClientConfig.get().shootButtonOffhand : ModClientConfig.get().shootButton;
            EnumMouseButton secondaryButton = ModClientConfig.get().aimButton;

            boolean isPrimaryButton = event.getKeyMapping().getKey().getValue() == primaryButton.toGlfw();
            boolean isSecondaryButton = event.getKeyMapping().getKey().getValue() == secondaryButton.toGlfw();
            EnumFunction primaryFunction = gunItem.getConfigType().getPrimaryFunction();
            EnumFunction secondaryFunction = gunItem.getConfigType().getSecondaryFunction();

            if (isSecondaryButton && secondaryFunction != EnumFunction.MELEE)
            {
                if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS)
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

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ModClient.clearTransientLighting();
        DebugHelper.getActiveDebugEntities().clear(); // cleanup on world/connection change
        TeamsClientState.clear();
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
        Vec3 renderedFeet = new Vec3(Mth.lerp((double) partialTick, player.xo, player.getX()),
            Mth.lerp((double) partialTick, player.yo, player.getY()),
            Mth.lerp((double) partialTick, player.zo, player.getZ()));
        Vec3 seatFeet = seat.getDriveable().getInterpolatedRiderWorldPosition(
            seat.getSeatIndex(), seat.getPassengerRidingOffset(player), partialTick);
        Vec3 correction = seatFeet.subtract(renderedFeet);
        event.getPoseStack().translate(correction.x, correction.y, correction.z);
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event)
    {
        if (event.getEntity() instanceof Player player && TeamsClientState.shouldHideNameTag(player))
            event.setResult(Event.Result.DENY);
    }

}
