package com.flansmodultimate.client.render.item;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelGun;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.keyframe.EnumKeyframeBone;
import com.flansmodultimate.common.types.keyframe.GunKeyframeConfig;
import com.flansmodultimate.common.types.keyframe.KeyframeAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Plays Tyrants and Plebeians keyframe ("GLTF") gun animations on a {@link PoseStack}. The transforms, their order
 * and the timing rules match its renderer, so animations authored for that pack keep their look.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GunKeyframeAnimator
{
    /** Sideways shift applied after an animated arm pose, in blocks */
    private static final float RIGHT_ARM_OFFSET = 0.3F;
    private static final float LEFT_ARM_OFFSET = -0.3F;

    /** What a playback belongs to; it decides which arm flag applies and how far parts translate. */
    enum Kind
    {
        DRAW,
        SECONDARY_SWITCH,
        STAGED_RELOAD,
        SECONDARY_RELOAD,
        TACTICAL_RELOAD,
        RELOAD;

        /** A default or tactical reload moves the whole gun in plain pixels; everything else in model pixels */
        boolean plainReload()
        {
            return this == TACTICAL_RELOAD || this == RELOAD;
        }
    }

    /**
     * The draw, secondary-mode switch or reload animation a gun is playing in this frame.
     *
     * @param partScale model scale the part bones divide their translation by
     */
    record Playback(Kind kind, KeyframeAnimation animation, float progress, float partScale)
    {}

    /**
     * Keyframe state of one first-person gun render.
     *
     * @param state what currently owns the gun's moving parts
     * @param playback the reload or switch animation to apply, if it exists
     * @param actionProgress bolt or pump action progress from 0 to 1, or negative when no action is cycling
     */
    record Frame(GunKeyframeConfig config, ModelGun model, @Nullable Kind state, @Nullable Playback playback, float actionProgress)
    {
        /** A draw, secondary-mode switch or reload owns the moving parts, replacing their legacy movement */
        boolean partsAnimated()
        {
            return state != null;
        }

        /** Moves a part with the current reload or switch animation. */
        void applyPart(PoseStack poseStack, EnumKeyframeBone bone, @Nullable Vector3f rotationPoint)
        {
            if (playback != null)
                apply(poseStack, playback.animation(), bone, playback.progress(), false, playback.partScale(), rotationPoint);
        }

        /** Moves a bolt or pump with the action animation while it cycles; false when the part follows its legacy movement. */
        boolean applyAction(PoseStack poseStack, EnumKeyframeBone bone, Vector3f rotationPoint)
        {
            if (partsAnimated())
            {
                applyPart(poseStack, bone, rotationPoint);
                return true;
            }
            if (config.getActionAnimation() == null)
                return false;
            KeyframeAnimation action = config.getAnimation(config.getActionAnimation());
            if (action != null && actionProgress >= 0F)
                apply(poseStack, action, bone, actionProgress, false, model.getType().getModelScale(), rotationPoint);
            return true;
        }

        /**
         * Whether an arm follows the keyframes. A secondary-mode switch moves the left arm by the right arm's setting,
         * as in Tyrants and Plebeians.
         */
        boolean armAnimated(boolean rightArm)
        {
            return rightArm || state == Kind.SECONDARY_SWITCH ? config.isRightArmAnimated() : config.isLeftArmAnimated();
        }
    }

    /** The keyframe state of a first-person render, or null when the gun does not use keyframe animations. */
    @Nullable
    static Frame frame(ModelGun model, GunAnimations animations, ItemStack stack, float smoothing)
    {
        GunKeyframeConfig config = model.getType().getKeyframes();
        if (!config.isEnabled())
            return null;
        return new Frame(config, model, state(config, model, animations, stack), playback(config, model, animations, stack, smoothing), actionProgress(animations, smoothing));
    }

    static boolean isSwitching(GunKeyframeConfig config, GunAnimations animations)
    {
        return animations.getSwitchAnimationProgress() > 0F && animations.getSwitchAnimationLength() > 0F && config.getSwitchAnimation() != null;
    }

    /** The grip's secondary mode has its own keyframe animations (Tyrants and Plebeians {@code HasSecondaryModeAnimations}). */
    static boolean secondaryAnimated(ModelGun model, ItemStack stack)
    {
        AttachmentType grip = model.getType().getGrip(stack);
        return grip != null && grip.isSecondaryFire() && grip.isHasSecondaryModeAnimations();
    }

    static boolean isSecondarySwitching(ModelGun model, GunAnimations animations, ItemStack stack)
    {
        return animations.getSecondarySwitchRemaining() > 0 && animations.getSecondarySwitchLength() > 0 && secondaryAnimated(model, stack);
    }

    @Nullable
    private static Kind state(GunKeyframeConfig config, ModelGun model, GunAnimations animations, ItemStack stack)
    {
        if (isSwitching(config, animations))
            return Kind.DRAW;
        if (isSecondarySwitching(model, animations, stack))
            return Kind.SECONDARY_SWITCH;
        return animations.isReloading() ? Kind.RELOAD : null;
    }

    /** The draw animation while switching to the gun, then a secondary-mode switch, otherwise the animation of the current reload. */
    @Nullable
    static Playback playback(GunKeyframeConfig config, ModelGun model, GunAnimations animations, ItemStack stack, float smoothing)
    {
        float modelScale = model.getType().getModelScale();
        if (isSwitching(config, animations))
        {
            float progress = Mth.clamp((animations.getSwitchAnimationProgress() + smoothing) / animations.getSwitchAnimationLength(), 0F, 1F);
            return playback(Kind.DRAW, config.getAnimation(config.getSwitchAnimation()), progress, modelScale);
        }
        boolean secondary = model.getType().getSecondaryFire(stack);
        if (isSecondarySwitching(model, animations, stack))
        {
            // The mode flips when the switch starts, so the current mode is the one being switched to
            float length = animations.getSecondarySwitchLength();
            float progress = Mth.clamp((length - animations.getSecondarySwitchRemaining() + smoothing) / length, 0F, 1F);
            String name = secondary ? config.getSecondarySwitchAnimation() : config.getSecondaryUnSwitchAnimation();
            return playback(Kind.SECONDARY_SWITCH, config.getAnimation(name), progress, model.getType().getGrip(stack).getModelScale());
        }
        if (!animations.isReloading())
            return null;

        float progress = reloadProgress(animations, smoothing);
        if (animations.isSinglesReload() || animations.isLoopReload() && config.isLoopReloadAnimation() && !secondary)
        {
            Stage stage = roundStage(config, animations, model, progress);
            return stage == null ? null : playback(Kind.STAGED_RELOAD, config.getAnimation(stage.animation()), stage.progress(), modelScale);
        }
        if (secondary && secondaryAnimated(model, stack) && config.getSecondaryReloadAnimation() != null)
            return playback(Kind.SECONDARY_RELOAD, config.getAnimation(config.getSecondaryReloadAnimation()), progress, modelScale);
        if (animations.isTacticalReload() && config.hasAnimation(config.getTacticalReloadAnimation()))
            return playback(Kind.TACTICAL_RELOAD, config.getAnimation(config.getTacticalReloadAnimation()), progress, modelScale);
        return playback(Kind.RELOAD, config.getAnimation(config.getReloadAnimation()), progress, modelScale);
    }

    @Nullable
    private static Playback playback(Kind kind, @Nullable KeyframeAnimation animation, float progress, float partScale)
    {
        return animation == null ? null : new Playback(kind, animation, progress, partScale);
    }

    static float reloadProgress(GunAnimations animations, float smoothing)
    {
        return animations.getLastReloadAnimationProgress() + (animations.getReloadAnimationProgress() - animations.getLastReloadAnimationProgress()) * smoothing;
    }

    /** Moves the whole gun with the draw, switch or reload animation. */
    static void applyGun(PoseStack poseStack, Playback playback, float modelScale)
    {
        boolean plain = playback.kind().plainReload();
        apply(poseStack, playback.animation(), EnumKeyframeBone.GUN, playback.progress(), plain, plain ? 1F : modelScale, null);
    }

    /**
     * Applies one bone of an animation.
     *
     * @param pixels translate in plain sixteenths of a block; otherwise sixteenths scaled down by {@code scale}
     * @param rotationPoint pivot of the rotation, or null to rotate about the current origin
     */
    static void apply(PoseStack poseStack, KeyframeAnimation animation, EnumKeyframeBone bone, float progress, boolean pixels, float scale, @Nullable Vector3f rotationPoint)
    {
        KeyframeAnimation.KeyframeBone keyframes = animation.bone(bone);
        if (keyframes == null)
            return;

        float time = Mth.clamp(progress, 0F, 1F) * animation.length();
        float[] translate = new float[3];
        float[] rotate = new float[3];
        keyframes.position().sample(time, animation.length(), translate);
        keyframes.rotation().sample(time, animation.length(), rotate);

        float divisor = pixels ? 16F : 16F * scale;
        poseStack.translate(-translate[0] / divisor, translate[1] / divisor, translate[2] / divisor);
        if (rotationPoint != null)
            poseStack.translate(rotationPoint.x, rotationPoint.y, rotationPoint.z);
        rotate(poseStack, rotate[0], rotate[1], rotate[2]);
        if (rotationPoint != null)
            poseStack.translate(-rotationPoint.x, -rotationPoint.y, -rotationPoint.z);
    }

    /** Places an arm by its bone, replacing the model's arm position and rotation. */
    static void applyArm(PoseStack poseStack, KeyframeAnimation animation, boolean rightArm, float progress)
    {
        KeyframeAnimation.KeyframeBone keyframes = animation.bone(rightArm ? EnumKeyframeBone.RIGHT_ARM : EnumKeyframeBone.LEFT_ARM);
        if (keyframes == null)
            return;

        float time = Mth.clamp(progress, 0F, 1F) * animation.length();
        float[] translate = new float[3];
        float[] rotate = new float[3];
        keyframes.position().sample(time, animation.length(), translate);
        keyframes.rotation().sample(time, animation.length(), rotate);
        placeArm(poseStack, translate[0], translate[1], translate[2], rotate[0], rotate[1], rotate[2], rightArm);
    }

    /** The resting arm pose of a model with {@code enableNewArm}: its arm position and rotation in the keyframe convention. */
    static void applyNewArmRest(PoseStack poseStack, Vector3f rotation, Vector3f position, boolean rightArm)
    {
        placeArm(poseStack, position.x, position.y, position.z, rotation.x, rotation.y, rotation.z, rightArm);
    }

    // Arm translation and rotation components are passed separately to avoid allocating vectors every frame
    @SuppressWarnings("java:S107")
    private static void placeArm(PoseStack poseStack, float x, float y, float z, float rotX, float rotY, float rotZ, boolean rightArm)
    {
        poseStack.translate(-x / 16F, y / 16F, z / 16F);
        rotate(poseStack, rotX, rotY, rotZ);
        poseStack.translate(rightArm ? RIGHT_ARM_OFFSET : LEFT_ARM_OFFSET, 0F, 0F);
    }

    private static void rotate(PoseStack poseStack, float x, float y, float z)
    {
        poseStack.mulPose(Axis.ZP.rotationDegrees(z));
        poseStack.mulPose(Axis.YP.rotationDegrees(-y));
        poseStack.mulPose(Axis.XP.rotationDegrees(-x));
    }

    /** Bolt or pump progress mapped from the legacy -1 to 1 cycle onto 0 to 1, or -1 while no action cycles. */
    static float actionProgress(GunAnimations animations, float smoothing)
    {
        if (animations.getPumped() != -1F && animations.getPumped() < 0.9F)
            return (animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * smoothing + 1F) * 0.5F;
        if (animations.getCharged() != -1F && animations.getCharged() < 0.9F)
            return (animations.getLastCharged() + (animations.getCharged() - animations.getLastCharged()) * smoothing + 1F) * 0.5F;
        return -1F;
    }

    /** One stage of a round-by-round reload: the start, per-round loop or end animation, and its own progress. */
    record Stage(@Nullable String animation, float progress)
    {}

    /**
     * Splits a round-by-round reload into its start, per-round loop and end animations. Start and end take the model's
     * {@code startTime} and {@code endTime} seconds and the rounds share the rest; when the reload is shorter than both,
     * all three stages are scaled together.
     */
    @Nullable
    static Stage roundStage(GunKeyframeConfig config, GunAnimations animations, ModelGun model, float reloadProgress)
    {
        int rounds = Math.max(0, animations.getReloadAmmoCount());
        if (rounds <= 0)
            return null;

        float total = animations.getReloadAnimationTime();
        float startTicks = model.getStartTime() * 20F;
        float endTicks = model.getEndTime() * 20F;
        float remaining = total - startTicks - endTicks;
        float loopTicks;
        if (remaining > 0F)
        {
            loopTicks = remaining / rounds;
        }
        else
        {
            float modelTotal = model.getStartTime() + model.getLoopTime() * rounds + model.getEndTime();
            float scale = modelTotal > 0F ? total / modelTotal : 0F;
            startTicks = model.getStartTime() * scale;
            endTicks = model.getEndTime() * scale;
            loopTicks = model.getLoopTime() * scale;
        }

        float time = Mth.clamp(reloadProgress, 0F, 0.999999F) * total;
        if (time < startTicks)
            return new Stage(config.getStartReloadAnimation(), time / startTicks);

        float loopEnd = startTicks + loopTicks * rounds;
        if (time < loopEnd)
        {
            float intoLoop = time - startTicks;
            int round = Mth.floor(intoLoop / loopTicks);
            return new Stage(config.getLoopReloadAnimationName(), (intoLoop - round * loopTicks) / loopTicks);
        }

        float endProgress = endTicks > 0F ? Mth.clamp((time - loopEnd) / endTicks, 0F, 1F) : 1F;
        return new Stage(config.getEndReloadAnimation(), endProgress);
    }
}
