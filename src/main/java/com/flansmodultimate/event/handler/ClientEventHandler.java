package com.flansmodultimate.event.handler;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.debug.DebugColor;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.input.EnumMouseButton;
import com.flansmodultimate.client.input.GunInputState;
import com.flansmodultimate.client.render.ClientHudOverlays;
import com.flansmodultimate.client.render.InstantBulletRenderer;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.DeployedGun;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@net.neoforged.fml.common.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientEventHandler
{
    private static boolean resourcePacksAutoSelected = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        autoSelectContentPacks();
        GunInputState.tick();
        ModClient.tick();
    }

    private static void autoSelectContentPacks()
    {
        if (resourcePacksAutoSelected)
            return;
        resourcePacksAutoSelected = true;

        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        if (repo == null)
            return;

        List<String> selectedIds = new ArrayList<>(repo.getSelectedIds());
        boolean changed = false;

        for (Pack pack : repo.getAvailablePacks())
        {
            try
            {
                Set<String> namespaces = pack.open().getNamespaces(PackType.CLIENT_RESOURCES);
                if (namespaces != null && namespaces.contains("flansmod"))
                {
                    if (!selectedIds.contains(pack.getId()))
                    {
                        selectedIds.add(pack.getId());
                        changed = true;
                    }
                }
            }
            catch (Exception e)
            {
                FlansMod.log.warn("Failed to open content pack {} for namespace detection", pack.getId(), e);
            }
        }

        if (changed)
        {
            repo.setSelected(selectedIds);
            mc.reloadResourcePacks();
        }
    }

    @SubscribeEvent
    public static void onComputeCameraFov(ViewportEvent.ComputeFov event)
    {
        ModClient.updateCameraZoom(event);
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
            double scrollDelta = event.getScrollDeltaY();

            if (isOneHanded && isSneakingKeyDown && Math.abs(scrollDelta) > 0.0D)
            {
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
        InstantBulletRenderer.renderAllTrails(event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(true), event.getCamera());

        if (ModClient.isDebug())
        {
            for (DebugColor debugEntity : DebugHelper.getActiveDebugEntities())
            {
                if (event.getFrustum().isVisible(debugEntity.getAABB()))
                    debugEntity.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), event.getCamera());
            }
        }
    }

    /** Set up RenderContext for gun animations and set Aim Pose when GunItem is held by players */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event)
    {
        ModClient.entityRenderContext.set(event.getEntity());

        if (!(event.getEntity() instanceof Player player))
            return;

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

            if (isSecondaryButton && gunItem.getConfigType().getSecondaryFunction() != EnumFunction.MELEE)
            {
                if (mc.hitResult == null || mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS)
                {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                }
            }
            else if (isPrimaryButton && gunItem.getConfigType().getPrimaryFunction() != EnumFunction.MELEE)
            {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        DebugHelper.getActiveDebugEntities().clear();
    }

    //TODO: Handle player hiding / name tag removal for teams (1.12.2)
    //renderPlayer()
    //TODO: implement this code from 1.12.2
    //renderHooks.update();
    //RenderFlag.angle += 2F;
    //TODO: implement commented out code for driveables (1.12.2)
    //updatePlayerView();
}