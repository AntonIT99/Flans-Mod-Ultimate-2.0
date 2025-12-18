package com.flansmodultimate;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.client.debug.DebugColor;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.client.input.MouseInputHandler;
import com.flansmodultimate.client.render.InstantBulletRenderer;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.IScope;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketGunScopedState;
import com.flansmodultimate.network.server.PacketGunSpread;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModClient
{
    public static final ThreadLocal<LivingEntity> entityRenderContext = new ThreadLocal<>();

    public static final HumanoidModel.ArmPose bothArmsAim = HumanoidModel.ArmPose.create("both_arms_aim", true,
        (model, entity, arm) -> {
            model.rightArm.xRot = -Mth.PI / 2F;
            model.rightArm.yRot = -0.05F;
            model.rightArm.zRot = 0F;
            model.leftArm.xRot = -Mth.PI / 2F;
            model.leftArm.yRot = 0.05F;
            model.leftArm.zRot = 0F;
        });

    @Getter
    private static boolean isDebug;

    // Plane / Vehicle control handling
    /** Whether the player has received the vehicle tutorial text */
    private static boolean doneTutorial;
    /** Whether the player is in mouse control mode */
    private static boolean controlModeMouse = true;
    /** A delayer on the mouse control switch */
    private static int controlModeSwitchTimer = 20;
    /** The delay between switching slots */
    @Getter @Setter
    private static float switchTime;

    // Recoil variables
    /** The recoil applied to the player view by shooting */
    @Getter @Setter
    private static float playerRecoil;
    /** The amount of compensation to apply to the recoil in order to bring it back to normal */
    private static float antiRecoil;
    /** equals the old "minecraft.player.rotationPitch" delta */
    private static float recoilOffset;
    /** For interpolation */
    private static float recoilOffsetPrev;
    @Getter @Setter
    private static int lastBulletReload;
    @Getter @Setter
    private static int shotState = -1;

    // Gun animations
    /** Gun animation variables for each entity holding a gun. Currently only applicable to the player */
    private static final HashMap<LivingEntity, GunAnimations> gunAnimationsRight = new HashMap<>();
    private static final HashMap<LivingEntity, GunAnimations> gunAnimationsLeft = new HashMap<>();

    // Scope variables
    /** A delayer on the scope button to avoid repeat presses */
    private static int scopeTime;
    /** The scope that is currently being looked down */
    @Getter
    private static IScope currentScope;
    /** The transition variable for zooming in / out with a smoother. 0 = unscoped, 1 = scoped */
    @Getter
    private static float zoomProgress;
    @Getter
    private static float lastZoomProgress;
    /** The zoom level of the last scope used for transitioning out of being scoped, even after the scope is forgotten */
    private static float lastZoomLevel = 1F;
    private static float lastFOVZoomLevel = 1F;
    private static double currentFOV = 1.0;

    // Variables to hold the state of some settings so that after being hacked for scopes, they may be restored
    /** The player's mouse sensitivity setting, as it was before being hacked by my mod */
    private static double originalMouseSensitivity = 0.5;
    /** The player's original FOV */
    private static double originalFOV = 70.0;
    /** The original CameraType */
    private static CameraType originalCameraType = CameraType.FIRST_PERSON;
    private static boolean changedCameraEntity;

    //TODO: FMU Hitmarker logic
    @Getter @Setter
    private static int hitMarkerTime;
    @Getter @Setter
    private static boolean hitMarkerHeadshot;
    @Getter @Setter
    private static float hitMarkerPenAmount = 1F;
    @Getter @Setter
    private static boolean hitMarkerExplosion;

    //TODO: implement
    @Getter @Setter
    private static boolean isInFlash;
    @Getter @Setter
    private static int flashTime = 10;

    /** Lighting */
    private static final List<BlockPos> blockLightOverrides = new ArrayList<>();
    private static int lightOverrideRefreshRate = 5;

    public static void setDebug(boolean value)
    {
        isDebug = value;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.gui.getChat().addMessage(Component.literal("[Flan's Mod Ultimate] Debug Mode " + (isDebug ? "On" : "Off")).withStyle(ChatFormatting.RED));
    }

    @NotNull
    @OnlyIn(Dist.CLIENT)
    public static GunAnimations getGunAnimations(LivingEntity living, InteractionHand hand)
    {
        Map<LivingEntity, GunAnimations> map = (hand == InteractionHand.OFF_HAND) ? gunAnimationsLeft : gunAnimationsRight;
        return map.computeIfAbsent(living, k -> new GunAnimations());
    }

    @NotNull
    @OnlyIn(Dist.CLIENT)
    public static GunAnimations getGunAnimations(ItemDisplayContext context)
    {
        LivingEntity living;
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            living = Minecraft.getInstance().player;
        else
            living = entityRenderContext.get();

        if (living == null)
            return new GunAnimations();

        GunAnimations animations = null;

        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
        {
            if (gunAnimationsLeft.containsKey(living))
                animations = gunAnimationsLeft.get(living);
            else
            {
                animations = new GunAnimations();
                gunAnimationsLeft.put(living, animations);
            }
        }
        else if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
        {
            if (gunAnimationsRight.containsKey(living))
                animations = gunAnimationsRight.get(living);
            else
            {
                animations = new GunAnimations();
                gunAnimationsRight.put(living, animations);
            }
        }

        return Objects.requireNonNullElse(animations, new GunAnimations());
    }

    @OnlyIn(Dist.CLIENT)
    public static void updateScope(@Nullable IScope desiredScope, ItemStack gunStack, GunItem gunItem)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Options opts = mc.options;

        if (scopeTime > 0 || player == null || mc.screen != null || currentScope == desiredScope)
            return;

        if (currentScope == null)
        {
            // entering scope
            currentScope = desiredScope;
            lastZoomLevel = desiredScope.getZoomFactor();
            lastFOVZoomLevel = desiredScope.getFovFactor();

            // save originals
            originalMouseSensitivity = opts.sensitivity().get();
            originalCameraType = opts.getCameraType();

            // adjust sensitivity by sqrt(zoom)
            double newSensitivity = originalMouseSensitivity / Math.sqrt(desiredScope.getZoomFactor());
            opts.sensitivity().set(newSensitivity);

            // force first-person while scoped
            opts.setCameraType(CameraType.FIRST_PERSON);

            //Send ads spread packet to server
            sendADSSpreadToServer(gunStack, gunItem, player.isCrouching(), player.isSprinting());

            gunItem.setScoped(true);
            PacketHandler.sendToServer(new PacketGunScopedState(true));
        }
        else
        {
            // exiting scope
            currentScope = null;

            // restore
            opts.sensitivity().set(originalMouseSensitivity);
            opts.setCameraType(originalCameraType);

            //Send default spread packet to server
            PacketHandler.sendToServer(new PacketGunSpread(gunStack, gunItem.getConfigType().getDefaultSpread(gunStack)));

            gunItem.setScoped(false);
            PacketHandler.sendToServer(new PacketGunScopedState(false));
        }
        scopeTime = 10;
    }

    private static void sendADSSpreadToServer(ItemStack gunStack, GunItem gunItem, boolean sneaking, boolean sprinting)
    {
        float spread = gunItem.getConfigType().getSpread(gunStack, sneaking, sprinting);

        if (gunItem.getConfigType().getNumBullets() == 1)
            spread *= gunItem.getConfigType().getAdsSpreadModifier() == -1F ? ModCommonConfigs.defaultADSSpreadMultiplier.get().floatValue() : gunItem.getConfigType().getAdsSpreadModifier();
        else
            spread *= gunItem.getConfigType().getAdsSpreadModifierShotgun() == -1F ? ModCommonConfigs.defaultADSSpreadMultiplierShotgun.get().floatValue() : gunItem.getConfigType().getAdsSpreadModifierShotgun();

        PacketHandler.sendToServer(new PacketGunSpread(gunStack, spread));
    }

    @OnlyIn(Dist.CLIENT)
    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level  == null)
            return;

        updateFlashlights(mc, level);
        InstantBulletRenderer.updateAllTrails();
        updateTimers();
        updateRecoil();
        updateGunAnimations();
        updateScopeState(mc, player);
        updateZoom();

        if (changedCameraEntity && (mc.getCameraEntity() == null || !mc.getCameraEntity().isAlive()))
        {
            mc.setCameraEntity(player);
            changedCameraEntity = false;
        }

        KeyInputHandler.checkKeys();
        double dx = Minecraft.getInstance().mouseHandler.getXVelocity();
        double dy = Minecraft.getInstance().mouseHandler.getYVelocity();

        // Only handle if the velocity vector is not too small
        if (dx * dx + dy * dy > 0.001)
            MouseInputHandler.handleMouseMove(dx, dy);

        for (DebugColor debugEntity : DebugHelper.activeDebugEntities)
            debugEntity.tick();
    }

    /** Handle flashlight block light override */
    private static void updateFlashlights(Minecraft mc, ClientLevel level)
    {
        if (!shouldRunFlashlightUpdate(mc))
            return;

        updateRefreshRate(mc);
        clearOldLightBlocks(level);
        handlePlayerFlashlights(level);
        handleDynamicEntityLights(level);
    }

    private static boolean shouldRunFlashlightUpdate(Minecraft mc)
    {
        return mc.level != null && CommonEventHandler.getTicker() % lightOverrideRefreshRate == 0;
    }

    private static void updateRefreshRate(Minecraft mc)
    {
        GraphicsStatus graphics = mc.options.graphicsMode().get();
        boolean fancy = (graphics == GraphicsStatus.FANCY || graphics == GraphicsStatus.FABULOUS);
        lightOverrideRefreshRate = fancy ? 10 : 20;
    }

    private static void clearOldLightBlocks(ClientLevel level)
    {
        for (BlockPos pos : blockLightOverrides)
        {
            if (level.getBlockState(pos).is(Blocks.LIGHT))
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        blockLightOverrides.clear();
    }

    /** Handle lights from player-held flashlights. */
    private static void handlePlayerFlashlights(ClientLevel level)
    {
        for (Player player : level.players())
        {
            AttachmentType grip = getFlashlightGrip(player);
            if (grip != null)
                placeFlashlightLightsForPlayer(level, player, grip);
        }
    }

    /** Handle lights from bullets and mechas. */
    private static void handleDynamicEntityLights(ClientLevel level)
    {
        for (Entity entity : level.entitiesForRendering())
        {
            if (entity instanceof Shootable shootable)
                handleShootableLight(level, shootable);
            else if (entity instanceof Mecha mecha)
                handleMechaLight(level, mecha);
        }
    }

    @Nullable
    private static AttachmentType getFlashlightGrip(Player player)
    {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem itemGun))
            return null;

        GunType gunType = itemGun.getConfigType();
        AttachmentType grip = gunType.getGrip(stack);
        if (grip != null && grip.isFlashlight())
            return grip;

        return null;
    }

    private static void placeFlashlightLightsForPlayer(ClientLevel level, Player player, AttachmentType grip)
    {
        for (int i = 0; i < 2; i++)
        {
            float partialTicks = 1.0F;
            double distance = grip.getFlashlightRange() / 2F * (i + 1);

            HitResult hit = player.pick(distance, partialTicks, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK)
            {
                BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());
                placeLightIfAir(level, targetPos, 12);
            }
        }
    }

    private static void handleShootableLight(ClientLevel level, Shootable shootable)
    {
        if (shootable.isRemoved() || !shootable.getConfigType().isHasDynamicLight())
            return;

        BlockPos pos = shootable.blockPosition();
        placeLightIfAir(level, pos, 15);
    }

    private static void handleMechaLight(ClientLevel level, Mecha mecha)
    {
        BlockPos mechaPos = mecha.blockPosition();

        // Mecha light
        int mechaLight = mecha.lightLevel();
        if (mechaLight > 0)
        {
            int existing = level.getBrightness(LightLayer.BLOCK, mechaPos);
            int lightLevel = Math.max(existing, mechaLight);
            placeLightIfAir(level, mechaPos, lightLevel);
        }

        if (mecha.forceDark())
            applyForceDarkApproximation(mechaPos);
    }

    private static void placeLightIfAir(ClientLevel level, BlockPos pos, int lightLevel)
    {
        if (!level.getBlockState(pos).isAir())
            return;

        int clamped = Mth.clamp(lightLevel, 0, 15);
        BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, clamped);
        level.setBlock(pos, lightState, Block.UPDATE_CLIENTS);
        blockLightOverrides.add(pos.immutable());
    }

    /**
     * Placeholder for the old EnumSkyBlock.SKY "dark bubble" behavior.
     * 1.20.1 does not expose a simple setSkyLight API, so this is just
     * a hook where you can implement mixin/light-engine logic if needed.
     */
    private static void applyForceDarkApproximation(BlockPos center)
    {
        for (int i = -3; i <= 3; i++)
        {
            for (int j = -3; j <= 3; j++)
            {
                for (int k = -3; k <= 3; k++)
                {
                    BlockPos pos = center.offset(i, j, k);
                    blockLightOverrides.add(pos.immutable());
                    // TODO: Mecha forceDark(): sky light override has no clean public API in 1.20.1
                    //mc.world.setLightFor(EnumSkyBlock.SKY, blockPos, Math.abs(i) + Math.abs(j) + Math.abs(k));
                }
            }
        }
    }

    private static void updateTimers()
    {
        if (switchTime > 0)
            switchTime--;
        if (scopeTime > 0)
            scopeTime--;
        if (hitMarkerTime > 0)
            hitMarkerTime--;
        if (controlModeSwitchTimer > 0)
            controlModeSwitchTimer--;
    }

    private static void updateRecoil()
    {
        if (playerRecoil > 0)
            playerRecoil *= 0.8F;

        recoilOffsetPrev = recoilOffset;

        recoilOffset -= playerRecoil;
        antiRecoil += playerRecoil;

        recoilOffset += antiRecoil * 0.2F;
        antiRecoil *= 0.8F;
    }

    private static void updateGunAnimations()
    {
        for (GunAnimations g : gunAnimationsRight.values())
            g.update();
        for (GunAnimations g : gunAnimationsLeft.values())
            g.update();
    }

    private static void updateScopeState(Minecraft mc, LocalPlayer player)
    {
        if (currentScope != null)
        {
            ItemStack stackInHand = player.getMainHandItem();
            Item itemInHand = stackInHand.getItem();

            // If the currently held item is not a gun or is the wrong gun, unscope
            // If we've opened a GUI page, or we switched weapons, close the current scope
            boolean guiOpen = mc.screen != null;
            boolean notAGun = !(itemInHand instanceof GunItem);
            boolean differentScope = itemInHand instanceof GunItem gun && gun.getConfigType().getCurrentScope(stackInHand) != currentScope;

            if (guiOpen || notAGun || differentScope)
            {
                currentScope = null;
                mc.options.sensitivity().set(originalMouseSensitivity);
                mc.options.setCameraType(originalCameraType);
            }
        }
    }

    private static void updateZoom()
    {
        lastZoomProgress = zoomProgress;
        if (currentScope == null)
            zoomProgress *= 0.66F;
        else
            zoomProgress = 1F - (1F - zoomProgress) * 0.66F;
    }

    @OnlyIn(Dist.CLIENT)
    public static void updateCameraZoom(ViewportEvent.ComputeFov event)
    {
        originalFOV = event.getFOV();

        // If the zoom has changed sufficiently, update it
        if (Math.abs(zoomProgress - lastZoomProgress) > 0.0001F)
        {
            float actualZoomProgress = lastZoomProgress + (zoomProgress - lastZoomProgress) * (float) event.getPartialTick();
            float botchedZoomProgress = zoomProgress > 0.8F ? 1F : 0F;
            float zoomLevel = botchedZoomProgress * lastZoomLevel + (1 - botchedZoomProgress);
            float fovZoomLevel = actualZoomProgress * lastFOVZoomLevel + (1 - actualZoomProgress);
            if (Math.abs(zoomLevel - 1F) < 0.01F)
                zoomLevel = 1.0F;

            currentFOV = originalFOV / Math.max(fovZoomLevel, zoomLevel);
            event.setFOV(currentFOV);
        }
        else if (currentScope != null)
        {
            currentFOV = originalFOV / Math.max(lastZoomLevel, lastFOVZoomLevel);
            event.setFOV(currentFOV);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void updateCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        float angle = Mth.lerp((float) event.getPartialTick(), recoilOffsetPrev, recoilOffset);
        event.setPitch(Mth.clamp(event.getPitch() + angle, -90F, 90F));
    }
}
