package com.wolffsarmormod;

import com.flansmod.client.model.GunAnimations;
import com.wolffsarmormod.common.types.IScope;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModClient
{
    // Recoil variables
    /** The recoil applied to the player view by shooting */
    @Getter @Setter
    private static float playerRecoil;
    /** The amount of compensation to apply to the recoil in order to bring it back to normal */
    private static float antiRecoil;

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

    // Variables to hold the state of some settings so that after being hacked for scopes, they may be restored
    /** The player's mouse sensitivity setting, as it was before being hacked by my mod */
    private static double originalMouseSensitivity = 0.5;
    /** The player's original FOV */
    private static int originalFOV = 90;
    /** The original CameraType */
    private static CameraType originalCameraType = CameraType.FIRST_PERSON;

    @Getter @Setter
    private static int hitMarkerTime = 0;

    @OnlyIn(Dist.CLIENT)
    public static GunAnimations getGunAnimations(LivingEntity living, InteractionHand hand)
    {
        return getGunAnimations(living, hand == InteractionHand.OFF_HAND);
    }

    @OnlyIn(Dist.CLIENT)
    public static GunAnimations getGunAnimations(LivingEntity living, boolean leftHanded)
    {
        GunAnimations animations;

        if (leftHanded)
        {
            if (gunAnimationsLeft.containsKey(living))
                animations = gunAnimationsLeft.get(living);
            else
            {
                animations = new GunAnimations();
                gunAnimationsLeft.put(living, animations);
            }
        }
        else
        {
            if (gunAnimationsRight.containsKey(living))
                animations = gunAnimationsRight.get(living);
            else
            {
                animations = new GunAnimations();
                gunAnimationsRight.put(living, animations);
            }
        }

        return animations;
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
                lastFOVZoomLevel = scope.getFOVFactor();

                // save originals
                originalMouseSensitivity = opts.sensitivity().get();
                originalCameraType = opts.getCameraType();
                originalFOV = opts.fov().get();

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
                opts.fov().set(originalFOV);
            }
            scopeTime = 10;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void tick()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;

        if (player == null || level  == null)
            return;

        // Guns
        if(scopeTime > 0)
            scopeTime--;
        if(playerRecoil > 0)
            playerRecoil *= 0.8F;
        if(hitMarkerTime > 0)
            hitMarkerTime--;

        updateRecoil(player);

        // Update gun animations for the gun in hand
        for(GunAnimations g : gunAnimationsRight.values())
            g.update();
        for(GunAnimations g : gunAnimationsLeft.values())
            g.update();
    }

    private static void updateRecoil(LocalPlayer p)
    {
        // recoil kick up
        float x = p.getXRot() - playerRecoil;
        antiRecoil += playerRecoil;

        // recovery (spring back)
        x += antiRecoil * 0.2F;
        antiRecoil *= 0.8F;

        // clamp to sensible pitch range
        x = Mth.clamp(x, -90F, 90F);

        p.setXRot(x);
    }
}
