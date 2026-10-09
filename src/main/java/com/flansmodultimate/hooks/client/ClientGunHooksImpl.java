package com.flansmodultimate.hooks.client;

import com.flansmod.client.model.*;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.input.EnumAimType;
import com.flansmodultimate.client.input.GunInputState;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.effects.GunScreenShake;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.geometry.AAGunBarrelGeometry;
import com.flansmodultimate.common.guns.EnumFunction;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.GunItemHandler;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.hooks.IClientGunHooks;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.gun.*;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ClientGunHooksImpl implements IClientGunHooks
{
    private static final int AA_GUN_BARREL_ORIGIN_SYNC_INTERVAL = 100;
    private static final Map<Integer, Long> aaGunBarrelOriginSyncTicks = new HashMap<>();
    /** Last held gun per hand and last selected hotbar slot, used to detect weapon switches client side */
    private static final Map<InteractionHand, GunItem> lastHeldGuns = new EnumMap<>(InteractionHand.class);
    private static int lastSelectedSlot = -1;

    @Override
    public void meleeGunItem(GunItem gunItem, Player player, InteractionHand hand)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.doMelee(player, gunItem.getConfigType().getMeleeTime(), gunItem.getConfigType());
        GunAnimations anim = ModClient.getGunAnimations(player, hand);
        anim.doMelee(gunItem.getConfigType().getMeleeTime());
    }

    @Override
    public void shootGunItem(GunItem gunItem, Level level, Player player, PlayerData data, GunAnimations animations, ItemStack gunStack, InteractionHand hand)
    {
        int pumpDelay = 0;
        int pumpTime = 1;
        int hammerDelay = 0;
        int casingDelay = 0;
        float hammerAngle = 0;
        float althammerAngle = 0;

        if (ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun modelGun)
        {
            pumpDelay = modelGun.getPumpDelay();
            pumpTime = modelGun.getPumpTime();
            hammerDelay = modelGun.getHammerDelay();
            casingDelay = modelGun.getCasingDelay();
            hammerAngle = modelGun.getHammerAngle();
            althammerAngle = modelGun.getAlthammerAngle();
        }

        float shootTime = data.getShootTime(hand);
        while (shootTime <= 0F)
        {
            animations.doShoot(pumpDelay, pumpTime, hammerDelay, hammerAngle, althammerAngle, casingDelay);
            addScreenShake(gunItem.getConfigType());

            if (gunItem.getConfigType().isUseFancyRecoil())
                ModClient.getPlayerRecoil().addRecoil(gunItem.getConfigType().getRecoil(gunStack));
            else
            {
                ModClient.setPlayerRecoilPitch(ModClient.getPlayerRecoilPitch() + gunItem.getConfigType().getRecoilPitch(gunStack, ModUtils.getEnumMovement(player)));
                ModClient.setPlayerRecoilYaw(ModClient.getPlayerRecoilYaw() + gunItem.getConfigType().getRecoilYaw(gunStack, ModUtils.getEnumMovement(player)));
            }

            shootTime += gunItem.getConfigType().getShootDelay(gunStack);
        }
        data.setShootTime(hand, shootTime);

        DebugHelper.spawnDebugDot(player.getEyePosition(0.0F), 1000, 1F, 1F, 1F);
    }

    /** Adds the Labjac Edition firing impact of a gun that opts into {@code HasScreenShake}. */
    private static void addScreenShake(GunType type)
    {
        ModClientConfig config = ModClientConfig.get();
        if (!type.isHasScreenShake() || config == null || !config.gunScreenShake)
            return;
        float cameraKick = type.isScreenShakeCameraKick() ? type.getCameraRecoil() * type.getScreenShakeIntensity() : 0F;
        GunScreenShake.addShot(type.isScreenShakeUsesSustainedRecoil(), type.getScreenShakeIntensity(), cameraKick);
    }

    @Override
    public void reloadGunItem(GunItem gunItem, Player player, InteractionHand hand, float reloadTime, int reloadCount, boolean hasMultipleAmmo)
    {
        PlayerData data = PlayerData.getInstance(player);
        GunAnimations animations = ModClient.getGunAnimations(player, hand);

        data.doGunReload(hand, reloadTime, gunItem.getConfigType().getShootDelay(player.getItemInHand(hand)));

        int pumpDelay = 0;
        int pumpTime = 1;
        int chargeDelay = 0;
        int chargeTime = 1;

        if (ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun modelGun)
        {
            pumpDelay = modelGun.getPumpDelayAfterReload();
            pumpTime = modelGun.getPumpTime();
            chargeDelay = modelGun.getChargeDelayAfterReload();
            chargeTime = modelGun.getChargeTime();
        }

        animations.doReload(reloadTime, pumpDelay, pumpTime, chargeDelay, chargeTime, reloadCount, hasMultipleAmmo);
    }

    @Override
    public void cancelReloadGunItem(Player player, InteractionHand hand)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.setShootTimeRight(0);
        data.setShootTimeLeft(0);
        data.setReloading(hand, false);
        ModClient.getGunAnimations(player, hand).cancelReload();
    }

    @Override
    public void tickGunItem(GunItem gunItem, Level level, Player player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean dualWield)
    {
        if (player != Minecraft.getInstance().player || gunItem.getConfigType().isDeployable() || !gunItem.getGunItemHandler().gunCanBeHandled(player))
            return;
        if ((!gunItem.getConfigType().isUsableByPlayers() && (!player.getAbilities().instabuild || !ModCommonConfig.get().gunsAlwaysUsableByPlayersInCreativeMode())))
            return;

        GunType configType = gunItem.getConfigType();
        GunItemHandler gunItemHandler = gunItem.getGunItemHandler();

        // Force release actions when entering a GUI
        if (Minecraft.getInstance().screen != null && (data.isShooting(hand) || data.isSecondaryFunctionKeyPressed()))
        {
            data.setShootKeyPressed(hand, false);
            data.setSecondaryFunctionKeyPressed(false);
            PacketHandler.sendToServer(new PacketGunInput(false, data.isPrevShootKeyPressed(hand), false, hand));
        }

        GunInputState.ButtonState primaryFunctionState = GunInputState.getPrimaryFunctionState(hand);
        GunInputState.ButtonState secondaryFunctionState = GunInputState.getSecondaryFunctionState(hand, player.getMainHandItem().isEmpty());

        // Scope handling
        handleScope(gunItem, gunStack, hand, primaryFunctionState, secondaryFunctionState, dualWield);

        GunAnimations animations = ModClient.getGunAnimations(player, hand);

        // Switch Delay
        handleGunSwitchDelay(gunItem, data, animations, hand);

        // Client Shooting
        if (data.isShooting(hand))
            shootGunItem(gunItem, level, player, data, animations, gunStack, hand);

        if (ModClient.getSwitchTime() <= 0)
        {
            // Don’t shoot certain entities under crosshair
            if (configType.getPrimaryFunction() == EnumFunction.SHOOT && gunItemHandler.shouldBlockFireAtCrosshair())
                primaryFunctionState = new GunInputState.ButtonState(false, primaryFunctionState.isPrevPressed());

            data.setShootKeyPressed(hand, primaryFunctionState.isPressed());
            data.setPrevShootKeyPressed(hand, primaryFunctionState.isPrevPressed());
            data.setSecondaryFunctionKeyPressed(secondaryFunctionState.isPressed());

            // Send update to server when keys are pressed or released
            if (primaryFunctionState.isPressed() != primaryFunctionState.isPrevPressed() || secondaryFunctionState.isPressed() != secondaryFunctionState.isPrevPressed())
                PacketHandler.sendToServer(new PacketGunInput(primaryFunctionState.isPressed(), primaryFunctionState.isPrevPressed(), secondaryFunctionState.isPressed(), hand));
        }
    }

    private static void handleScope(GunItem gunItem, ItemStack gunStack, InteractionHand hand, GunInputState.ButtonState primaryFunctionState, GunInputState.ButtonState secondaryFunctionState,
        boolean dualWield)
    {
        GunType gunType = gunItem.getConfigType();
        boolean canZoom = gunType.getSecondaryFunction().isZoom() || gunType.getPrimaryFunction().isZoom() || gunType.getZoomFactor() > 1F || gunType.getFovFactor() > 1F;

        if (dualWield || !canZoom)
            return;

        IScope scope = null;
        EnumAimType aimType = ModClientConfig.get().aimType;

        if (gunType.getSecondaryFunction().isZoom())
        {
            if (aimType == EnumAimType.HOLD)
            {
                scope = secondaryFunctionState.isPressed() ? gunType.getCurrentScope(gunStack) : null;
            }
            else if (aimType == EnumAimType.TOGGLE)
            {
                scope = ModClient.getCurrentScope();
                if (secondaryFunctionState.isPressed() && !secondaryFunctionState.isPrevPressed())
                    scope = (scope == null) ? gunType.getCurrentScope(gunStack) : null;
            }
        }
        else if (gunType.getPrimaryFunction().isZoom())
        {
            if (aimType == EnumAimType.HOLD)
            {
                scope = primaryFunctionState.isPressed() ? gunType.getCurrentScope(gunStack) : null;
            }
            else if (aimType == EnumAimType.TOGGLE)
            {
                scope = ModClient.getCurrentScope();
                if (primaryFunctionState.isPressed() && !primaryFunctionState.isPrevPressed())
                    scope = (scope == null) ? gunType.getCurrentScope(gunStack) : null;
            }
        }
        else if (gunType.getZoomFactor() > 1F || gunType.getFovFactor() > 1F)
        {
            if (aimType == EnumAimType.HOLD)
            {
                scope = secondaryFunctionState.isPressed() ? gunType.getCurrentScope(gunStack) : null;
            }
            else if (aimType == EnumAimType.TOGGLE)
            {
                scope = ModClient.getCurrentScope();
                if (secondaryFunctionState.isPressed() && !secondaryFunctionState.isPrevPressed())
                    scope = (scope == null) ? gunType.getCurrentScope(gunStack) : null;
            }
        }

        ModClient.updateScope(scope, gunStack, gunItem, hand);
    }

    private static void handleGunSwitchDelay(GunItem gunItem, @NotNull PlayerData data, @NotNull GunAnimations animations, InteractionHand hand)
    {
        if (!hasSwitchedGun(gunItem, hand))
            return;

        float animationLength = gunItem.getConfigType().getSwitchDelay();
        if (animationLength == 0)
        {
            animations.setSwitchAnimationLength(0F);
            animations.setSwitchAnimationProgress(0F);
        }
        else
        {
            animations.setSwitchAnimationProgress(1);
            animations.setSwitchAnimationLength(animationLength);
            ModClient.setSwitchTime(Math.max(ModClient.getSwitchTime(), animationLength));

            data.setShootTime(hand, Math.max(data.getShootTime(hand), animationLength));
            // The server owns the shooting logic, so it has to know about the switch delay as well
            PacketHandler.sendToServer(new PacketGunSwitchDelay(hand));
        }
    }

    /**
     * @return whether the gun held in the given hand changed since the last check, which is what starts the switch delay
     */
    private static boolean hasSwitchedGun(GunItem gunItem, InteractionHand hand)
    {
        boolean switched = lastHeldGuns.put(hand, gunItem) != gunItem;

        if (hand == InteractionHand.MAIN_HAND)
        {
            int selectedSlot = Minecraft.getInstance().player == null ? -1 : Minecraft.getInstance().player.getInventory().selected;
            switched |= selectedSlot != lastSelectedSlot;
            lastSelectedSlot = selectedSlot;
        }

        return switched;
    }

    public void accelerateMinigun(Player player, InteractionHand hand, float rotationSpeed)
    {
        ModClient.getGunAnimations(player, hand).addMinigunBarrelRotationSpeed(rotationSpeed);
    }

    @Override
    public void tickDeployedGun(DeployedGun deployedGun)
    {
        if (deployedGun.getFirstPassenger() != Minecraft.getInstance().player)
            return;

        // Force release key when entering a GUI
        if (Minecraft.getInstance().screen != null)
        {
            if (deployedGun.isShootKeyPressed())
            {
                deployedGun.setShootKeyPressed(false);
                PacketHandler.sendToServer(new PacketDeployedGunInput(deployedGun, false, deployedGun.isPrevShootKeyPressed()));
            }
        }
        else
        {
            GunInputState.ButtonState primaryFunctionState = GunInputState.getPrimaryFunctionState(InteractionHand.MAIN_HAND);
            deployedGun.setShootKeyPressed(primaryFunctionState.isPressed());
            deployedGun.setPrevShootKeyPressed(primaryFunctionState.isPrevPressed());

            // Send update to server when key is pressed or released
            if (deployedGun.isShootKeyPressed() != deployedGun.isPrevShootKeyPressed())
                PacketHandler.sendToServer(new PacketDeployedGunInput(deployedGun, deployedGun.isShootKeyPressed(), deployedGun.isPrevShootKeyPressed()));
        }
    }

    @Override
    public void tickAAGun(AAGun aaGun)
    {
        ModelAAGun.BarrelOriginData barrelOriginData = getAAGunModelBarrelOrigins(aaGun);
        syncAAGunModelBarrelOrigins(aaGun, barrelOriginData);
        spawnAAGunDebugMarkers(aaGun, barrelOriginData);

        if (aaGun.getFirstPassenger() != Minecraft.getInstance().player)
            return;

        if (Minecraft.getInstance().screen != null)
        {
            if (aaGun.isShootKeyPressed())
            {
                aaGun.setShootKeyPressed(false);
                PacketHandler.sendToServer(new PacketDeployedGunInput(aaGun, false, aaGun.isPrevShootKeyPressed(), pivots(barrelOriginData), muzzles(barrelOriginData)));
            }
        }
        else
        {
            GunInputState.ButtonState primaryFunctionState = GunInputState.getPrimaryFunctionState(InteractionHand.MAIN_HAND);
            aaGun.setShootKeyPressed(primaryFunctionState.isPressed());
            aaGun.setPrevShootKeyPressed(primaryFunctionState.isPrevPressed());

            if (aaGun.isShootKeyPressed() != aaGun.isPrevShootKeyPressed())
                PacketHandler.sendToServer(new PacketDeployedGunInput(aaGun, aaGun.isShootKeyPressed(), aaGun.isPrevShootKeyPressed(), pivots(barrelOriginData), muzzles(barrelOriginData)));
        }
    }

    @Nullable
    private static ModelAAGun.BarrelOriginData getAAGunModelBarrelOrigins(AAGun aaGun)
    {
        AAGunType type = aaGun.getConfigType();
        if (type == null || !(ModelCache.getOrLoadTypeModel(type) instanceof ModelAAGun model))
            return null;

        return model.getModelBarrelOriginData(type);
    }

    private static void syncAAGunModelBarrelOrigins(AAGun aaGun, @Nullable ModelAAGun.BarrelOriginData barrelOriginData)
    {
        if (barrelOriginData == null)
            return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;

        long gameTime = minecraft.level.getGameTime();
        long nextSyncTick = aaGunBarrelOriginSyncTicks.getOrDefault(aaGun.getId(), 0L);
        if (gameTime < nextSyncTick)
            return;

        aaGunBarrelOriginSyncTicks.put(aaGun.getId(), gameTime + AA_GUN_BARREL_ORIGIN_SYNC_INTERVAL);
        PacketHandler.sendToServer(new PacketAAGunModelBarrelOrigins(aaGun, barrelOriginData.pivots(), barrelOriginData.muzzles()));
    }

    @Nullable
    private static Vec3[] pivots(@Nullable ModelAAGun.BarrelOriginData barrelOriginData)
    {
        return barrelOriginData == null ? null : barrelOriginData.pivots();
    }

    @Nullable
    private static Vec3[] muzzles(@Nullable ModelAAGun.BarrelOriginData barrelOriginData)
    {
        return barrelOriginData == null ? null : barrelOriginData.muzzles();
    }

    private static void spawnAAGunDebugMarkers(AAGun aaGun, @Nullable ModelAAGun.BarrelOriginData barrelOriginData)
    {
        AAGunType type = aaGun.getConfigType();
        if (type == null)
            return;

        int currentBarrel = aaGun.getCurrentBarrelIndex();
        boolean sentry = type.isSentry();
        Vec3 gunnerSeat = aaGun.getGunnerSeatPosition();

        for (int barrel = 0; barrel < type.getNumBarrels(); barrel++)
        {
            Vec3 barrelOrigin = getAAGunDebugBarrelOrigin(aaGun, barrel, sentry, barrelOriginData);
            Vec3 shootingVector = aaGun.getShootingDirection().scale(1.5D);
            if (barrel == currentBarrel)
            {
                DebugHelper.spawnDebugDot(barrelOrigin, 2, 1F, 0F, 0F);
                DebugHelper.spawnDebugVector(barrelOrigin, shootingVector, 2, 1F, 0.2F, 0F);
            }
            else
            {
                DebugHelper.spawnDebugDot(barrelOrigin, 2, 1F, 1F, 0F);
                DebugHelper.spawnDebugVector(barrelOrigin, shootingVector, 2, 1F, 1F, 0F);
            }
            // The type file's Barrel line, which only fires until a client has
            // reported the model. Shown whenever the model is what fires, so the
            // two can be compared and the line corrected with /flandebug.
            if (hasModelBarrel(barrelOriginData, barrel))
            {
                Vec3 authored = aaGun.getAuthoredBarrelOrigin(barrel, sentry, barrelOriginData.pivots()[barrel]);
                DebugHelper.spawnDebugDot(authored, 2, 1F, 0F, 1F);
                DebugHelper.spawnDebugVector(authored, shootingVector, 2, 1F, 0F, 1F);
            }
        }

        if (!sentry)
        {
            DebugHelper.spawnDebugDot(gunnerSeat, 2, 0F, 0.45F, 1F);
            DebugHelper.spawnDebugVector(gunnerSeat, new Vec3(0D, 2D, 0D), 2, 0F, 0.45F, 1F);
        }
    }

    private static boolean hasModelBarrel(@Nullable ModelAAGun.BarrelOriginData barrelOriginData, int barrel)
    {
        return barrelOriginData != null && barrel < barrelOriginData.pivots().length && barrel < barrelOriginData.muzzles().length;
    }

    private static Vec3 getAAGunDebugBarrelOrigin(AAGun aaGun, int barrel, boolean sentryShot, @Nullable ModelAAGun.BarrelOriginData barrelOriginData)
    {
        if (hasModelBarrel(barrelOriginData, barrel))
            return aaGun.position().add(AAGunBarrelGeometry.modelBarrelOffset(barrelOriginData.pivots()[barrel], barrelOriginData.muzzles()[barrel], aaGun.getGunYaw(), aaGun.getGunPitch()));

        return aaGun.getBarrelOrigin(barrel, sentryShot);
    }

    @Override
    @Nullable
    public HitResult getClientHitResult()
    {
        return Minecraft.getInstance().hitResult;
    }
}
