package com.wolffsarmormod;

import com.flansmod.client.model.GunAnimations;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;

public class ModClient
{

    /**
     * Gun animation variables for each entity holding a gun. Currently only applicable to the player
     */
    private static final HashMap<LivingEntity, GunAnimations> gunAnimationsRight = new HashMap<>();
    private static final HashMap<LivingEntity, GunAnimations> gunAnimationsLeft = new HashMap<>();
    /**
     * The transition variable for zooming in / out with a smoother. 0 = unscoped, 1 = scoped
     */
    public static float zoomProgress = 0F, lastZoomProgress = 0F;
    /**
     * The zoom level of the last scope used for transitioning out of being scoped, even after the scope is forgotten
     */
    public static float lastZoomLevel = 1F, lastFOVZoomLevel = 1F;

    public static GunAnimations getGunAnimations(boolean leftHanded, Object... data)
    {
        GunAnimations animations = new GunAnimations();

        if (data.length > 1 && data[1] instanceof LivingEntity living)
        {
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
        }

        return animations;
    }
}
