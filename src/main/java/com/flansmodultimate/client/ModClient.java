package com.flansmodultimate.client;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.model.RenderGun;
import com.flansmodultimate.client.debug.DebugColor;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.client.input.MouseInputHandler;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.InstantBulletRenderer;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.guns.GunRecoil;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.IScope;
import com.flansmodultimate.config.ModServerConfig;
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
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.lang3.StringUtils;
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

@OnlyIn(Dist.CLIENT)
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
    private static boolean isShooting;

    // Recoil variables
    /** Fancy Recoil System */
    @Getter
    private static GunRecoil playerRecoil = new GunRecoil();
    /** The recoil applied to the player view by shooting */
    @Getter @Setter
    private static float playerRecoilPitch;
    @Getter @Setter
    private static float playerRecoilYaw;
    /** The amount of compensation to apply to the recoil in order to bring it back to normal */
    private static float antiRecoilPitch;
    private static float antiRecoilYaw;

    // tick impulses computed by recoil logic (degrees)
    private static float impulsePitch;
    private static float impulseYaw;
    // accumulator that will be drained across render frames (degrees)
    private static float pendingPitch;
    private static float pendingYaw;
    // tuning: higher = snappier (less “floaty”), lower = smoother
    private static final float RECOIL_SMOOTH_PER_SECOND = 35.0F;
    // deadzone to kill micro jitter
    private static final float DEADZONE = 0.0015f;

    @Getter @Setter
    private static int lastBulletReload;
    @Getter @Setter
    private static int shotState = -1;

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
    /** The player's mouse sensitivity setting, as it was before being hacked by my mod */
    private static double originalMouseSensitivity = 0.5;
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

    // Gun animations
    /** Gun animation variables for each entity holding a gun. Currently only applicable to the player */
    private static final HashMap<LivingEntity, GunAnimations> gunAnimationsRight = new HashMap<>();
    private static final HashMap<LivingEntity, GunAnimations> gunAnimationsLeft = new HashMap<>();

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
            spread *= gunItem.getConfigType().getAdsSpreadModifier() == -1F ? (float) ModServerConfig.get().defaultADSSpreadMultiplier : gunItem.getConfigType().getAdsSpreadModifier();
        else
            spread *= gunItem.getConfigType().getAdsSpreadModifierShotgun() == -1F ? (float) ModServerConfig.get().defaultADSSpreadMultiplierShotgun : gunItem.getConfigType().getAdsSpreadModifierShotgun();

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

        PlayerData data = PlayerData.getInstance(player, LogicalSide.CLIENT);

        updateFlashlights(mc, level);
        InstantBulletRenderer.updateAllTrails();
        updateTimers();

        isShooting = data.isShooting(InteractionHand.MAIN_HAND) || data.isShooting(InteractionHand.OFF_HAND);

        updateRecoil(player);
        updateGunAnimations();
        updateScopeState(mc, player);
        updateZoom();
        playGunItemSounds(player, InteractionHand.MAIN_HAND);
        playGunItemSounds(player, InteractionHand.OFF_HAND);
        SoundHelper.tickClient();

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

        DebugHelper.getActiveDebugEntities().forEach(DebugColor::tick);
    }

    /** Handle flashlight block light override */
    private static void updateFlashlights(Minecraft mc, ClientLevel level)
    {
        if (!shouldRunFlashlightUpdate(mc))
            return;

        updateRefreshRate();
        clearOldLightBlocks(level);
        handlePlayerFlashlights(level);
        handleDynamicEntityLights(level);
    }

    private static boolean shouldRunFlashlightUpdate(Minecraft mc)
    {
        return mc.level != null && CommonEventHandler.getTicker() % lightOverrideRefreshRate == 0;
    }

    private static void updateRefreshRate()
    {
        lightOverrideRefreshRate = hasFancyGraphics() ? 10 : 20;
    }

    public static boolean hasFancyGraphics()
    {
        GraphicsStatus graphics = Minecraft.getInstance().options.graphicsMode().get();
        return graphics != GraphicsStatus.FAST;
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

    private static void updateRecoil(LocalPlayer player)
    {
        impulsePitch = 0.0F;
        impulseYaw = 0.0F;

        if (isFancyRecoilEnabled(player))
        {
            computeImpulseForFancyRecoil(player);
        }
        else
        {
            computeImpulseForLegacyRecoil(player);
        }

        // deadzone tiny noise
        impulsePitch = deadzone(impulsePitch);
        impulseYaw = deadzone(impulseYaw);

        // accumulate; will be applied smoothly during render frames
        pendingPitch += impulsePitch;
        pendingYaw += impulseYaw;
    }

    private static float deadzone(float v)
    {
        return (Math.abs(v) < DEADZONE) ? 0.0f : v;
    }

    private static boolean isFancyRecoilEnabled(LocalPlayer player)
    {
        return (player.getMainHandItem().getItem() instanceof GunItem mainHandGunItem
            && mainHandGunItem.getConfigType().isUseFancyRecoil())
            || (player.getOffhandItem().getItem() instanceof GunItem offhandGunItem
            && offhandGunItem.getConfigType().isUseFancyRecoil());
    }

    private static void computeImpulseForFancyRecoil(LocalPlayer player)
    {
        float recoilToAdd = playerRecoil.update(player.isCrouching(), currentScope != null, (float) player.getDeltaMovement().length());

        if (player.getVehicle() instanceof Seat seat && seat.getSeatInfo() != null)
        {
            //TODO: uncomment for Seats
            /*EntitySeat s = (EntitySeat) p.ridingEntity;
            float newPlayerPitch = s.playerLooking.getPitch() + recoilToAdd;
            float horizontal = playerRecoil.horizontal;
            float newPlayerYaw = s.playerLooking.getYaw() + horizontal;
            if (newPlayerPitch > -s.seatInfo.minPitch) {
                newPlayerPitch = -s.seatInfo.minPitch;
            }
            if (newPlayerPitch < -s.seatInfo.maxPitch) {
                newPlayerPitch = -s.seatInfo.maxPitch;
            }
            s.playerLooking.setAngles(newPlayerYaw, newPlayerPitch, 0);*/
            impulsePitch = 0.0F;
            impulseYaw = 0.0F;
            return;
        }

        float oldPitch = player.getXRot();
        float newPitch = Mth.clamp(oldPitch + recoilToAdd, -90.0F, 90.0F);

        impulsePitch = newPitch - oldPitch;
        impulseYaw = (newPitch > -90.0F && newPitch < 90.0F) ? playerRecoil.getHorizontal() : 0.0F;
    }

    private static void computeImpulseForLegacyRecoil(LocalPlayer player)
    {
        dampenRecoilPitch(player);

        float oldPitch = player.getXRot();
        float oldYaw = player.getYRot();
        float newPitch = Mth.clamp(oldPitch - playerRecoilPitch, -90.0F, 90.0F);
        float newYaw = oldYaw - playerRecoilYaw;

        antiRecoilPitch += playerRecoilPitch;
        antiRecoilYaw += playerRecoilYaw;

        // No anti-recoil if realistic recoil is on, and no anti-recoil if firing and enable sight downward movement is off
        if (ModServerConfig.get().realisticRecoil
            && ((!isShooting) || ModServerConfig.get().enableSightDownwardMovement))
        {
            newPitch = Mth.clamp(newPitch + antiRecoilPitch * 0.2F, -90.0F, 90.0F);
        }

        newYaw = newYaw + antiRecoilYaw * 0.2F;

        impulsePitch = newPitch - oldPitch;
        impulseYaw = Mth.wrapDegrees(newYaw - oldYaw);

        antiRecoilPitch *= 0.8F;
        antiRecoilYaw *= 0.8F;
    }

    private static void dampenRecoilPitch(LocalPlayer player)
    {
        if (playerRecoilPitch <= 0.0F)
            return;

        float recoilControl = 0.8F;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();

        if (mainHand.getItem() instanceof GunItem mainHandGunItem && offhand.getItem() instanceof GunItem offhandGunItem)
        {
            recoilControl = Math.max(
                    mainHandGunItem.getConfigType().getRecoilControl(mainHand, player.isSprinting(), player.isCrouching()),
                    offhandGunItem.getConfigType().getRecoilControl(offhand, player.isSprinting(), player.isCrouching())
            );
        }
        else if (mainHand.getItem() instanceof GunItem gunItem)
        {
            recoilControl = gunItem.getConfigType().getRecoilControl(mainHand, player.isSprinting(), player.isCrouching());
        }
        else if (offhand.getItem() instanceof GunItem gunItem)
        {
            recoilControl = gunItem.getConfigType().getRecoilControl(offhand, player.isSprinting(), player.isCrouching());
        }

        playerRecoilPitch *= recoilControl;
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

    private static void playGunItemSounds(Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof GunItem gunItem) || !(ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun modelGun))
            return;

        GunType type = gunItem.getConfigType();
        GunAnimations animations = getGunAnimations(player, hand);
        AttachmentType pump = type.getPump(stack);

        if (shotState != -1
            && (((1F - Math.abs(animations.getLastPumped())) * modelGun.getBoltCycleDistance() != 0F) || (pump != null && (1F - Math.abs(animations.getLastPumped())) * modelGun.getPumpHandleDistance() != 0F)))
        {
            ModClient.setShotState(-1);
            SoundHelper.playSoundLocalAndBroadcast(type.getActionSound(), player.position(), ModServerConfig.get().soundRange);
        }

        EnumAnimationType anim = modelGun.getAnimationType();
        if (anim == EnumAnimationType.CUSTOMRIFLE || anim == EnumAnimationType.SHOTGUN || anim == EnumAnimationType.STRIKER || anim == EnumAnimationType.CUSTOMSHOTGUN || anim == EnumAnimationType.CUSTOMSTRIKER)
        {
            float clipPosition = RenderGun.getClipPosition(modelGun, stack, animations.getLastReloadAnimationProgress());
            float maxBullets = RenderGun.getNumBulletsInReload(modelGun, animations);
            float ammoPosition = clipPosition * maxBullets;
            int bulletNum = Mth.floor(ammoPosition);
            float bulletProgress = ammoPosition - bulletNum;

            if ((anim == EnumAnimationType.CUSTOMRIFLE || maxBullets > 1) && type.getNumAmmoItemsInGun(stack) > 1 && StringUtils.isNotBlank(type.getBulletInsert()) && ModClient.getLastBulletReload() != -2)
            {
                if (maxBullets == 2 && ModClient.getLastBulletReload() != -1)
                {
                    int time = (int) (animations.getReloadAnimationTime() / maxBullets);
                    SoundHelper.playSoundDelayedLocalAndBroadcast(type.getBulletInsert(), player.position(), ModServerConfig.get().soundRange, time);
                    ModClient.setLastBulletReload(-1);
                }
                else if ((bulletNum == (int) maxBullets || bulletNum == ModClient.lastBulletReload - 1))
                {
                    ModClient.setLastBulletReload(bulletNum);
                    SoundHelper.playSoundLocalAndBroadcast(type.getBulletInsert(), player.position(), ModServerConfig.get().soundRange);
                }

                if ((ammoPosition < 0.03 && bulletProgress > 0))
                {
                    ModClient.setLastBulletReload(-2);
                    SoundHelper.playSoundLocalAndBroadcast(type.getBulletInsert(), player.position(), ModServerConfig.get().soundRange);
                }
            }
        }


    }

    @OnlyIn(Dist.CLIENT)
    public static void updateCameraZoom(ViewportEvent.ComputeFov event)
    {
        // If the zoom has changed sufficiently, update it
        if (Math.abs(zoomProgress - lastZoomProgress) > 0.0001F)
        {
            float actualZoomProgress = lastZoomProgress + (zoomProgress - lastZoomProgress) * (float) event.getPartialTick();
            float botchedZoomProgress = zoomProgress > 0.8F ? 1F : 0F;
            float zoomLevel = botchedZoomProgress * lastZoomLevel + (1 - botchedZoomProgress);
            float fovZoomLevel = actualZoomProgress * lastFOVZoomLevel + (1 - actualZoomProgress);
            if (Math.abs(zoomLevel - 1F) < 0.01F)
                zoomLevel = 1.0F;

            event.setFOV(event.getFOV() / Math.max(fovZoomLevel, zoomLevel));
        }
        else if (currentScope != null)
        {
            event.setFOV(event.getFOV() / Math.max(lastZoomLevel, lastFOVZoomLevel));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderTick()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        // Frame delta in seconds (approx). getDeltaFrameTime() is in ticks.
        float dtTicks = mc.getDeltaFrameTime();
        float dtSeconds = dtTicks / 20.0F;

        // frame-rate independent smoothing
        float t = 1.0F - (float) Math.exp(-RECOIL_SMOOTH_PER_SECOND * dtSeconds);

        float applyPitch = pendingPitch * t;
        float applyYaw = pendingYaw * t;

        pendingPitch -= applyPitch;
        pendingYaw -= applyYaw;

        // Apply smoothly
        float newPitch = Mth.clamp(player.getXRot() + applyPitch, -90.0F, 90.0F);
        player.setXRot(newPitch);

        float newYaw = player.getYRot() + Mth.wrapDegrees(applyYaw);
        player.setYRot(newYaw);

        player.yHeadRot = newYaw;
        player.yBodyRot = newYaw;
        player.xRotO = newPitch;
        player.yRotO = newYaw;
        player.yHeadRotO = newYaw;
        player.yBodyRotO = newYaw;
    }
}
