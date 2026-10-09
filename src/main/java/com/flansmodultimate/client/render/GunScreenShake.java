package com.flansmodultimate.client.render;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

/**
 * The firing impact an infantry gun that opts into {@code HasScreenShake} gives its shooter, from the
 * Labjac Edition's Polati-style screen shake.
 *
 * <p>
 * Every shot punches the field of view in and, optionally, kicks the camera upwards. The punch is
 * a shake budget from 0 down to -100 that recovers in steps, faster the deeper it is, and narrows the
 * view by a tenth of a degree per point. The {@code Sustained} style strengthens it with a sustained-fire
 * meter that climbs by 10 per shot and drains between bursts, so long bursts feel heavier. The camera
 * kick decays to a fifth every tick like the legacy view-bobbing pitch it was written for. Both offsets
 * are purely visual: they never move the player's real aim.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunScreenShake
{
    static final int MAX_SHAKE = 100;
    static final float MAX_SUSTAINED_FIRE = 100F;
    static final float SUSTAINED_FIRE_PER_SHOT = 10F;
    /** Legacy {@code cameraPitch} kick per unit of camera recoil: -5.5 then +1.5, both scaled by 0.7 */
    static final float CAMERA_KICK_DEGREES = 2.8F;

    private static int shake;
    private static float sustainedFire;
    private static boolean sustainedStyle = true;
    private static float fovOffset;
    private static float prevFovOffset;
    private static float cameraKick;
    private static float prevCameraKick;

    /**
     * Adds the impact of one shot.
     *
     * @param useSustainedRecoil
     *            whether the punch grows with sustained fire ({@code ScreenShakeStyle Sustained})
     * @param intensity
     *            {@code ScreenShakeIntensity}; zero or less adds no punch
     * @param cameraRecoil
     *            camera kick strength, already scaled by the intensity; zero or less adds none
     */
    public static void addShot(boolean useSustainedRecoil, float intensity, float cameraRecoil)
    {
        sustainedStyle = useSustainedRecoil;
        if (intensity > 0F && Float.isFinite(intensity))
            shake = Math.max(-MAX_SHAKE, shake - Math.round(5F * intensity));
        if (cameraRecoil > 0F && Float.isFinite(cameraRecoil))
            cameraKick -= CAMERA_KICK_DEGREES * cameraRecoil;
        sustainedFire = Math.min(MAX_SUSTAINED_FIRE, sustainedFire + SUSTAINED_FIRE_PER_SHOT);
    }

    /** Advances the shake by one client tick. */
    public static void tick()
    {
        prevFovOffset = fovOffset;
        prevCameraKick = cameraKick;

        if (shake < 0)
        {
            fovOffset = 0.1F * shake * (sustainedStyle ? 1F + sustainedFire / 70F : 1F);
            shake = Math.min(0, shake + recoveryStep(shake));
        }
        else
            fovOffset = 0F;

        cameraKick *= 0.2F;
        if (Math.abs(cameraKick) < 0.001F)
            cameraKick = 0F;

        // The legacy sustained-fire meter drains slowly while high and quickly once a burst is over
        if (sustainedFire >= 20F)
            sustainedFire -= 3F;
        else
            sustainedFire *= 0.88F;
        if (sustainedFire < 0.01F)
            sustainedFire = 0F;
    }

    /** Degrees to add to the field of view; negative values narrow it. */
    public static float fovOffset(float partialTick)
    {
        return Mth.lerp(partialTick, prevFovOffset, fovOffset);
    }

    /** Degrees to add to the camera pitch; negative values look up. */
    public static float pitchOffset(float partialTick)
    {
        return Mth.lerp(partialTick, prevCameraKick, cameraKick);
    }

    /** Ends any running shake. */
    public static void reset()
    {
        shake = 0;
        sustainedFire = 0F;
        sustainedStyle = true;
        fovOffset = 0F;
        prevFovOffset = 0F;
        cameraKick = 0F;
        prevCameraKick = 0F;
    }

    /**
     * The legacy recovery table. 1.7.10 stored the budget in an {@code int}, so a fractional step such as
     * {@code += 4.5F} was truncated towards zero by the compound assignment, which recovers a negative
     * budget by the step rounded up. The rounded steps are kept here.
     */
    static int recoveryStep(int current)
    {
        if (current < -90)
            return 5;
        if (current < -70)
            return 4;
        if (current < -50)
            return 3;
        if (current < -30)
            return 2;
        return 1;
    }
}
