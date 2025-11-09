package com.wolffsarmormod;

import com.flansmod.client.model.GunAnimations;
import com.wolffsarmormod.common.item.GunItem;
import com.wolffsarmormod.common.types.IScope;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModClient
{
    public static final ThreadLocal<LivingEntity> entityRenderContext = new ThreadLocal<>();

    public static final HumanoidModel.ArmPose bothArmsAim = HumanoidModel.ArmPose.create("both_arms_aim", true,
        (model, entity, arm) -> {
            model.rightArm.xRot = -(float)Math.PI / 2F;
            model.rightArm.yRot = -0.05F;
            model.rightArm.zRot = 0F;
            model.leftArm.xRot = -(float)Math.PI / 2F;
            model.leftArm.yRot = 0.05F;
            model.leftArm.zRot = 0F;
        });

    @Getter @Setter
    private static boolean isDebug = false;

    // Plane / Vehicle control handling
    /** Whether the player has received the vehicle tutorial text */
    private static boolean doneTutorial = false;
    /** Whether the player is in mouse control mode */
    private static boolean controlModeMouse = true;
    /** A delayer on the mouse control switch */
    private static int controlModeSwitchTimer = 20;

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

    // Gun animations
    /** Gun animation variables for each entity holding a gun. Currently only applicable to the player */
    private static final HashMap<LivingEntity, GunAnimations> gunAnimationsRight = new HashMap<>();
    private static final HashMap<LivingEntity, GunAnimations> gunAnimationsLeft = new HashMap<>();

    // Scope variables
    /** A delayer on the scope button to avoid repeat presses */
    private static int scopeTime;
    /** The scope that is currently being looked down */
    @Getter
    private static IScope currentScope = null;
    /** The transition variable for zooming in / out with a smoother. 0 = unscoped, 1 = scoped */
    @Getter
    private static float zoomProgress = 0F;
    @Getter
    private static float lastZoomProgress = 0F;
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

    @Getter @Setter
    private static int hitMarkerTime = 0;

    @NotNull
    @OnlyIn(Dist.CLIENT)
    public static GunAnimations getGunAnimations(LivingEntity living, InteractionHand hand)
    {
        GunAnimations animations = null;

        if (hand == InteractionHand.MAIN_HAND)
        {
            if (gunAnimationsRight.containsKey(living))
                animations = gunAnimationsRight.get(living);
            else
            {
                animations = new GunAnimations();
                gunAnimationsRight.put(living, animations);
            }
        }
        else if (hand == InteractionHand.OFF_HAND)
        {
            if (gunAnimationsLeft.containsKey(living))
                animations = gunAnimationsLeft.get(living);
            else
            {
                animations = new GunAnimations();
                gunAnimationsLeft.put(living, animations);
            }
        }

        return Objects.requireNonNullElse(animations, new GunAnimations());
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
    public static void setScope(IScope scope)
    {
        Minecraft mc = Minecraft.getInstance();
        Options opts = mc.options;

        if (scopeTime <= 0 && mc.screen == null)
        {
            if (currentScope == null)
            {
                // entering scope
                currentScope = scope;
                lastZoomLevel = scope.getZoomFactor();
                lastFOVZoomLevel = scope.getFovFactor();

                // save originals
                originalMouseSensitivity = opts.sensitivity().get();
                originalCameraType = opts.getCameraType();

                // adjust sensitivity by sqrt(zoom)
                double newSensitivity = originalMouseSensitivity / Math.sqrt(scope.getZoomFactor());
                opts.sensitivity().set(newSensitivity);

                // force first-person while scoped
                opts.setCameraType(CameraType.FIRST_PERSON);
            }
            else
            {
                // exiting scope
                currentScope = null;

                // restore
                opts.sensitivity().set(originalMouseSensitivity);
                opts.setCameraType(originalCameraType);
            }
            scopeTime = 10;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level  == null)
            return;

        updateScopeAndHitMarkerTime();
        updateRecoil();
        updateGunAnimations();
        updateScopeState(mc, player);
        updateZoom();

        if (controlModeSwitchTimer > 0)
            controlModeSwitchTimer--;

        if (changedCameraEntity && (mc.getCameraEntity() == null || !mc.getCameraEntity().isAlive()))
        {
            mc.setCameraEntity(player);
            changedCameraEntity = false;
        }
    }

    private static void updateScopeAndHitMarkerTime()
    {
        if (scopeTime > 0)
            scopeTime--;
        if (hitMarkerTime > 0)
            hitMarkerTime--;
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
