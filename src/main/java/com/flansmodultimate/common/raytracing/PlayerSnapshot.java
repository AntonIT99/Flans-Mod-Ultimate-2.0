package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.api.entity.HumanoidBody;
import com.flansmodultimate.api.equipment.ArmPoseRule;
import com.flansmodultimate.common.driveables.physics.PlaneRiderRotation;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.ProneSupport;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.guns.HeldItemPresentation;
import com.flansmodultimate.common.guns.ProneArmPoses;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.flansmodultimate.util.JomlUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * This class takes a snapshot of the player's position rotation and held items at a certain point in time.
 * It is used to handle bullet detection. The server will store a second or two of snapshots so that it
 * can work out where the player thought they were shooting accounting for packet lag.
 * <p>
 * The hitboxes mirror the vanilla player model: the same part pivots, cubes and animation as
 * {@code HumanoidModel.setupAnim}, placed with the same transforms as {@code PlayerRenderer}.
 * Everything used here is available on the server as well. Not replicated: death and upside-down
 * rotations, slim arms (the skin model is only known client-side) and gun animations in third person.
 * <p>
 * Humanoid NPCs ({@link com.flansmodultimate.api.entity.IFlanHumanoidBody}) get the same boxes, sized and placed by the proportions they are drawn with.
 */
public class PlayerSnapshot
{
    public static final int NUM_PLAYER_SNAPSHOTS = 20;

    private static final float PIXEL = 1F / 16F;
    /** A player as {@code PlayerRenderer} draws it, standing and crouching */
    private static final HumanoidBody PLAYER = HumanoidBody.standard(HumanoidBody.PLAYER_SCALE, 0F);
    private static final HumanoidBody CROUCHING_PLAYER = HumanoidBody.standard(HumanoidBody.PLAYER_SCALE, -0.125F);

    /** The player or humanoid NPC this snapshot is for */
    public final LivingEntity entity;
    /** The entity's position at the point the snapshot was taken */
    public final Vector3f pos;
    /** The entity's velocity at the point the snapshot was taken */
    public final Vector3f vel;
    /** The hitboxes for this entity */
    public final List<PlayerHitbox> hitboxes = new ArrayList<>();
    /** The time at which this snapshot was taken */
    public final long time;

    public PlayerSnapshot(Player p)
    {
        this(p, 1F);
    }

    /**
     * @param partialTick
     *            interpolation between the previous and current tick, as used for rendering;
     *            {@code 1} takes the current tick values
     */
    public PlayerSnapshot(Player p, float partialTick)
    {
        this(p, partialTick, p.isCrouching() ? CROUCHING_PLAYER : PLAYER);
    }

    /**
     * @param shape
     *            the proportions the entity is drawn with
     */
    public PlayerSnapshot(LivingEntity p, float partialTick, HumanoidBody shape)
    {
        entity = p;
        time = p.level().getGameTime();
        pos = new Vector3f((float) Mth.lerp(partialTick, p.xOld, p.getX()), (float) Mth.lerp(partialTick, p.yOld, p.getY()), (float) Mth.lerp(partialTick, p.zOld, p.getZ()));
        vel = JomlUtils.fromVec3(p.getDeltaMovement());

        boolean sitting = p.isPassenger() && p.getVehicle() != null && p.getVehicle().shouldRiderSit();
        boolean crouching = p.isCrouching();
        boolean fallFlying = p.getFallFlyingTicks() > 4;
        float swimAmount = p.getSwimAmount(partialTick);

        // Body and head angles, as computed in LivingEntityRenderer.render
        float bodyYaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, p.yHeadRotO, p.yHeadRot);
        if (sitting && p.getVehicle() instanceof LivingEntity mount)
        {
            float netYaw = Mth.clamp(Mth.wrapDegrees(headYaw - Mth.rotLerp(partialTick, mount.yBodyRotO, mount.yBodyRot)), -85F, 85F);
            bodyYaw = headYaw - netYaw;
            if (netYaw * netYaw > 2500F)
                bodyYaw += netYaw * 0.2F;
        }
        float headPitch = Mth.lerp(partialTick, p.xRotO, p.getXRot());

        float limbSwing = 0F;
        float limbSwingAmount = 0F;
        if (!sitting && p.isAlive())
        {
            limbSwing = p.walkAnimation.position(partialTick);
            limbSwingAmount = Math.min(p.walkAnimation.speed(partialTick), 1F);
        }
        float ageInTicks = p.tickCount + partialTick;
        float attackTime = p.getAttackAnim(partialTick);

        // Part pivots and rotations, as computed in HumanoidModel.setupAnim
        Part head = new Part(0F, 0F, 0F);
        Part body = new Part(0F, 0F, 0F);
        Part rightArm = new Part(-5F, 2F, 0F);
        Part leftArm = new Part(5F, 2F, 0F);
        Part rightLeg = new Part(-1.9F, 12F, 0F);
        Part leftLeg = new Part(1.9F, 12F, 0F);

        head.yRot = (headYaw - bodyYaw) * Mth.DEG_TO_RAD;
        if (fallFlying)
            head.xRot = -Mth.PI / 4F;
        else if (swimAmount > 0F && p.isVisuallySwimming())
            head.xRot = rotLerpRad(swimAmount, headPitch * Mth.DEG_TO_RAD, -Mth.PI / 4F);
        else
            head.xRot = headPitch * Mth.DEG_TO_RAD;

        // Limbs swing less while gliding fast
        float swingDamping = 1F;
        if (fallFlying)
        {
            swingDamping = (float) p.getDeltaMovement().lengthSqr() / 0.2F;
            swingDamping = Math.max(swingDamping * swingDamping * swingDamping, 1F);
        }

        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * limbSwingAmount / swingDamping;
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount / swingDamping;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount / swingDamping;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount / swingDamping;
        rightLeg.yRot = 0.005F;
        leftLeg.yRot = -0.005F;
        rightLeg.zRot = 0.005F;
        leftLeg.zRot = -0.005F;
        if (sitting)
        {
            rightArm.xRot -= Mth.PI / 5F;
            leftArm.xRot -= Mth.PI / 5F;
            rightLeg.xRot = -1.4137167F;
            rightLeg.yRot = Mth.PI / 10F;
            rightLeg.zRot = 0.07853982F;
            leftLeg.xRot = -1.4137167F;
            leftLeg.yRot = -Mth.PI / 10F;
            leftLeg.zRot = -0.07853982F;
        }

        boolean rightHanded = p.getMainArm() == HumanoidArm.RIGHT;
        ArmPose vanillaMainPose = armPose(p, InteractionHand.MAIN_HAND, false);
        ArmPose vanillaOffPose = armPose(p, InteractionHand.OFF_HAND, vanillaMainPose.twoHanded);
        GunArmPoses.Result gunPoses = GunArmPoses.resolve(p);
        ArmPose mainPose = withGunPose(vanillaMainPose, gunPoses.mainHand());
        ArmPose offPose = withGunPose(vanillaOffPose, gunPoses.offHand());
        ArmPose rightPose = rightHanded ? mainPose : offPose;
        ArmPose leftPose = rightHanded ? offPose : mainPose;
        poseArms(p, rightHanded, rightPose, leftPose, head, rightArm, leftArm);
        animateAttack(p, attackTime, head, body, rightArm, leftArm);

        if (crouching)
        {
            body.xRot = 0.5F;
            rightArm.xRot += 0.4F;
            leftArm.xRot += 0.4F;
            rightLeg.z = 4F;
            leftLeg.z = 4F;
            rightLeg.y = 12.2F;
            leftLeg.y = 12.2F;
            head.y = 4.2F;
            body.y = 3.2F;
            leftArm.y = 5.2F;
            rightArm.y = 5.2F;
        }

        // Idle arm bob (AnimationUtils.bobModelPart)
        if (rightPose != ArmPose.SPYGLASS)
        {
            rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            rightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        }
        if (leftPose != ArmPose.SPYGLASS)
        {
            leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            leftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
        }

        if (swimAmount > 0F)
            animateSwimming(p, swimAmount, limbSwing, attackTime, rightArm, leftArm, rightLeg, leftLeg);

        // Guns held forward along a prone body, as EquipmentAnimationSupport.prone poses the model
        ProneArmPoses.resolve(gunPoses, rightHanded, headPitch * Mth.DEG_TO_RAD, (headYaw - bodyYaw) * Mth.DEG_TO_RAD, ProneSupport.lyingAmount(p, swimAmount))
            .ifPresent(prone -> poseProne(prone, head, rightArm, leftArm));

        customArm(p, InteractionHand.MAIN_HAND, rightHanded ? rightArm : leftArm);
        customArm(p, InteractionHand.OFF_HAND, rightHanded ? leftArm : rightArm);

        Matrix4f root = rootTransform(p, partialTick, bodyYaw, shape, swimAmount);

        addBox(partTransform(root, head, shape.head()), -4F, -8F, -4F, 8F, 8F, 8F, EnumHitboxType.HEAD);
        addBox(partTransform(root, body, shape.body()), -4F, 0F, -2F, 8F, 12F, 4F, EnumHitboxType.BODY);
        Matrix4f rightArmTransform = partTransform(root, rightArm, shape.rightArm());
        Matrix4f leftArmTransform = partTransform(root, leftArm, shape.leftArm());
        addBox(rightArmTransform, -3F, -2F, -2F, 4F, 12F, 4F, EnumHitboxType.RIGHTARM);
        addBox(leftArmTransform, -1F, -2F, -2F, 4F, 12F, 4F, EnumHitboxType.LEFTARM);
        addBox(partTransform(root, rightLeg, shape.rightLeg()), -2F, 0F, -2F, 4F, 12F, 4F, EnumHitboxType.LEGS);
        addBox(partTransform(root, leftLeg, shape.leftLeg()), -2F, 0F, -2F, 4F, 12F, 4F, EnumHitboxType.LEGS);

        addShieldBox(p.getMainHandItem(), rightHanded ? rightArmTransform : leftArmTransform, !rightHanded, EnumHitboxType.RIGHTITEM);
        addShieldBox(p.getOffhandItem(), rightHanded ? leftArmTransform : rightArmTransform, rightHanded, EnumHitboxType.LEFTITEM);
    }

    /**
     * Model space relative to the player's feet, as set up by PlayerRenderer: render offset, bed offset,
     * body rotations (sleeping, riptide, elytra, swimming), then the renderer's flip, scale and model origin.
     */
    private static Matrix4f rootTransform(LivingEntity p, float partialTick, float bodyYaw, HumanoidBody shape, float swimAmount)
    {
        Matrix4f root = new Matrix4f().translate(0F, shape.offsetY(), 0F);

        boolean sleeping = p.hasPose(Pose.SLEEPING);
        Direction bedDirection = sleeping ? p.getBedOrientation() : null;
        if (bedDirection != null)
        {
            float headOffset = p.getEyeHeight(Pose.STANDING) - 0.1F;
            root.translate(-bedDirection.getStepX() * headOffset, 0F, -bedDirection.getStepZ() * headOffset);
        }

        if (p.isFullyFrozen())
            bodyYaw += (float) (Math.cos(p.tickCount * 3.25D) * Math.PI * 0.4F);

        if (!sleeping)
            root.rotateY((180F - bodyYaw) * Mth.DEG_TO_RAD);

        if (p.isAutoSpinAttack())
        {
            root.rotateX((-90F - p.getXRot()) * Mth.DEG_TO_RAD);
            root.rotateY((p.tickCount + partialTick) * -75F * Mth.DEG_TO_RAD);
        }
        else if (sleeping)
        {
            root.rotateY((bedDirection != null ? sleepDirectionToRotation(bedDirection) : bodyYaw) * Mth.DEG_TO_RAD);
            root.rotateZ(Mth.PI / 2F);
            root.rotateY(Mth.PI * 1.5F);
        }

        if (p.isFallFlying())
        {
            float flightTicks = p.getFallFlyingTicks() + partialTick;
            float flightBlend = Mth.clamp(flightTicks * flightTicks / 100F, 0F, 1F);
            if (!p.isAutoSpinAttack())
                root.rotateX(flightBlend * (-90F - p.getXRot()) * Mth.DEG_TO_RAD);

            // Bank into turns: angle between where the player looks and where they fly
            Vec3 view = p.getViewVector(partialTick);
            Vec3 motion = p.getDeltaMovement();
            double motionSqr = motion.horizontalDistanceSqr();
            double viewSqr = view.horizontalDistanceSqr();
            if (motionSqr > 0D && viewSqr > 0D)
            {
                double cos = Mth.clamp((motion.x * view.x + motion.z * view.z) / Math.sqrt(motionSqr * viewSqr), -1D, 1D);
                double cross = motion.x * view.z - motion.z * view.x;
                root.rotateY((float) (Math.signum(cross) * Math.acos(cos)));
            }
        }
        else if (swimAmount > 0F)
        {
            boolean inFluid = p.isInWater() || p.isInFluidType((fluidType, height) -> p.canSwimInFluidType(fluidType));
            root.rotateX(Mth.lerp(swimAmount, 0F, inFluid ? -90F - p.getXRot() : -90F) * Mth.DEG_TO_RAD);
            if (p.isVisuallySwimming())
                root.translate(0F, -1F, 0.3F);
        }

        if (p.getVehicle() instanceof Seat seat && seat.getDriveable() instanceof Plane plane)
            root.rotate(PlaneRiderRotation.at(plane, partialTick, bodyYaw));

        return root.scale(-shape.scaleX(), -shape.scaleY(), shape.scaleZ()).translate(0F, -1.501F, 0F);
    }

    private static void poseProne(ProneArmPoses.Result prone, Part head, Part rightArm, Part leftArm)
    {
        head.rotate(prone.head());
        if (prone.rightArm() != null)
            rightArm.rotate(prone.rightArm());
        if (prone.leftArm() != null)
            leftArm.rotate(prone.leftArm());
    }

    private static float sleepDirectionToRotation(Direction direction)
    {
        return switch (direction)
        {
            case SOUTH -> 90F;
            case NORTH -> 270F;
            case EAST -> 180F;
            default -> 0F;
        };
    }

    /** The gun arm pose ClientEventHandler puts over the vanilla pose, as GunArmPoses decides it */
    private static ArmPose withGunPose(ArmPose vanillaPose, GunArmPoses.Arm gunPose)
    {
        return switch (gunPose)
        {
            case NONE -> vanillaPose;
            case RELAXED -> ArmPose.EMPTY;
            case ONE_ARM -> ArmPose.ONE_AIM;
            case BOW -> ArmPose.AIM;
            case BOTH -> ArmPose.BOTH_AIM;
            case THROW -> ArmPose.THROW;
            case SUPPORT -> ArmPose.SUPPORT;
        };
    }

    private static void customArm(LivingEntity player, InteractionHand hand, Part arm)
    {
        ArmPoseRule rule = HeldItemPresentation.rule(player, hand);
        if (rule.pose() == ArmPoseRule.Pose.RELAXED && GunArmPoses.classify(player.getItemInHand(hand)) != GunArmPoses.HandItem.SHIELD)
            arm.xRot = arm.yRot = arm.zRot = 0F;
        if (rule.pose() != ArmPoseRule.Pose.CUSTOM)
            return;
        if ((rule.axes() & 1) != 0)
            arm.xRot = rule.x() * Mth.DEG_TO_RAD;
        if ((rule.axes() & 2) != 0)
            arm.yRot = rule.y() * Mth.DEG_TO_RAD;
        if ((rule.axes() & 4) != 0)
            arm.zRot = rule.z() * Mth.DEG_TO_RAD;
    }

    /**
     * Vanilla arm pose per hand from PlayerRenderer.getArmPose, before any gun pose is applied.
     *
     * @param mainTwoHanded
     *            whether the vanilla main hand pose is two-handed, which only matters for the off hand
     */
    private static ArmPose armPose(LivingEntity p, InteractionHand hand, boolean mainTwoHanded)
    {
        ItemStack stack = p.getItemInHand(hand);
        if (stack.isEmpty())
            return ArmPose.EMPTY;

        // A two-handed main hand pose leaves the off hand merely holding its item
        if (hand == InteractionHand.OFF_HAND && mainTwoHanded)
            return ArmPose.ITEM;

        if (p.getUsedItemHand() == hand && p.getUseItemRemainingTicks() > 0)
        {
            UseAnim useAnim = stack.getUseAnimation();
            switch (useAnim)
            {
                case BLOCK :
                    return ArmPose.BLOCK;
                case BOW :
                    return ArmPose.AIM;
                case SPEAR :
                    return ArmPose.THROW_SPEAR;
                case CROSSBOW :
                    return ArmPose.CROSSBOW_CHARGE;
                case SPYGLASS :
                    return ArmPose.SPYGLASS;
                case TOOT_HORN :
                    return ArmPose.TOOT_HORN;
                case BRUSH :
                    return ArmPose.BRUSH;
                default :
                    break;
            }
        }
        else if (!p.swinging && stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack))
        {
            return ArmPose.CROSSBOW_HOLD;
        }
        return ArmPose.ITEM;
    }

    /** Applies both arm poses in HumanoidModel.setupAnim's order */
    private static void poseArms(LivingEntity p, boolean rightHanded, ArmPose rightPose, ArmPose leftPose, Part head, Part rightArm, Part leftArm)
    {
        if (p.isUsingItem())
        {
            if ((p.getUsedItemHand() == InteractionHand.MAIN_HAND) == rightHanded)
                poseUsingArm(p, rightPose, leftPose, head, rightArm, leftArm, 1F);
            else
                poseUsingArm(p, leftPose, rightPose, head, leftArm, rightArm, -1F);
        }
        else if (rightHanded != (rightHanded ? leftPose.twoHanded : rightPose.twoHanded))
        {
            poseArm(p, leftPose, head, leftArm, rightArm, -1F);
            poseArm(p, rightPose, head, rightArm, leftArm, 1F);
        }
        else
        {
            poseArm(p, rightPose, head, rightArm, leftArm, 1F);
            poseArm(p, leftPose, head, leftArm, rightArm, -1F);
        }
    }

    /**
     * Vanilla poses only the arm of an item in use. ModClient.oneArmThrow also applies the gun pose of the
     * other arm, before drawing the throwing arm back from where it swung.
     */
    private static void poseUsingArm(LivingEntity p, ArmPose pose, ArmPose otherPose, Part head, Part arm, Part otherArm, float side)
    {
        if (pose == ArmPose.THROW && (otherPose == ArmPose.ONE_AIM || otherPose == ArmPose.BOTH_AIM || otherPose == ArmPose.SUPPORT))
        {
            float swing = arm.xRot;
            poseArm(p, otherPose, head, otherArm, arm, -side);
            arm.xRot = swing;
        }
        poseArm(p, pose, head, arm, otherArm, side);
    }

    /**
     * Mirrors HumanoidModel.poseRightArm / poseLeftArm.
     *
     * @param side
     *            {@code 1} when {@code arm} is the right arm, {@code -1} for the left arm
     */
    private static void poseArm(LivingEntity p, ArmPose pose, Part head, Part arm, Part otherArm, float side)
    {
        switch (pose)
        {
            case EMPTY -> arm.yRot = 0F;
            case ITEM -> {
                arm.xRot = arm.xRot * 0.5F - Mth.PI / 10F;
                arm.yRot = 0F;
            }
            case BLOCK -> {
                arm.xRot = arm.xRot * 0.5F - 0.9424779F;
                arm.yRot = -side * Mth.PI / 6F;
            }
            case AIM -> {
                // Bow pose: the held arm follows the head, the other arm reaches across to support it
                arm.yRot = -side * 0.1F + head.yRot;
                otherArm.yRot = side * 0.5F + head.yRot;
                arm.xRot = -Mth.PI / 2F + head.xRot;
                otherArm.xRot = -Mth.PI / 2F + head.xRot;
            }
            case ONE_AIM -> {
                // ModClient.oneArmAim: the held arm of the bow pose alone
                arm.yRot = -side * 0.1F + head.yRot;
                arm.xRot = -Mth.PI / 2F + head.xRot;
            }
            case BOTH_AIM -> {
                // ModClient.bothArmsAim
                arm.xRot = -Mth.PI / 2F;
                arm.yRot = -side * 0.05F;
                arm.zRot = 0F;
                otherArm.xRot = -Mth.PI / 2F;
                otherArm.yRot = side * 0.05F;
                otherArm.zRot = 0F;
            }
            case SUPPORT -> {
                // ModClient.bowSupport: the free arm of the bow pose alone
                arm.yRot = -side * 0.5F + head.yRot;
                arm.xRot = -Mth.PI / 2F + head.xRot;
            }
            case THROW_SPEAR, THROW -> {
                arm.xRot = arm.xRot * 0.5F - Mth.PI;
                arm.yRot = 0F;
            }
            case CROSSBOW_CHARGE -> {
                arm.yRot = -side * 0.8F;
                arm.xRot = -0.97079635F;
                float chargeDuration = EntityPlatform.crossbowChargeDuration(p.getUseItem(), p);
                float charge = Mth.clamp(p.getTicksUsingItem(), 0F, chargeDuration) / chargeDuration;
                otherArm.yRot = Mth.lerp(charge, 0.4F, 0.85F) * side;
                otherArm.xRot = Mth.lerp(charge, arm.xRot, -Mth.PI / 2F);
            }
            case CROSSBOW_HOLD -> {
                arm.yRot = -side * 0.3F + head.yRot;
                otherArm.yRot = side * 0.6F + head.yRot;
                arm.xRot = -Mth.PI / 2F + head.xRot + 0.1F;
                otherArm.xRot = -1.5F + head.xRot;
            }
            case SPYGLASS -> {
                arm.xRot = Mth.clamp(head.xRot - 1.9198622F - (p.isCrouching() ? 0.2617994F : 0F), -2.4F, 3.3F);
                arm.yRot = head.yRot - side * 0.2617994F;
            }
            case TOOT_HORN -> {
                arm.xRot = Mth.clamp(head.xRot, -1.2F, 1.2F) - 1.4835298F;
                arm.yRot = head.yRot - side * Mth.PI / 6F;
            }
            case BRUSH -> {
                arm.xRot = arm.xRot * 0.5F - Mth.PI / 5F;
                arm.yRot = 0F;
            }
        }
    }

    private static HumanoidArm attackArm(LivingEntity p)
    {
        return p.swingingArm == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
    }

    /** Mirrors HumanoidModel.setupAttackAnimation */
    private static void animateAttack(LivingEntity p, float attackTime, Part head, Part body, Part rightArm, Part leftArm)
    {
        if (attackTime <= 0F)
            return;

        HumanoidArm attackArm = attackArm(p);
        Part arm = attackArm == HumanoidArm.LEFT ? leftArm : rightArm;

        body.yRot = Mth.sin(Mth.sqrt(attackTime) * Mth.TWO_PI) * 0.2F;
        if (attackArm == HumanoidArm.LEFT)
            body.yRot *= -1F;

        // HumanoidModelMixin restores the non-attacking shield arm after vanilla animates it.
        boolean keepShieldRaised = GunArmPoses.keepsOffhandShieldRaised(p);
        if (!keepShieldRaised || attackArm == HumanoidArm.RIGHT)
        {
            rightArm.z = Mth.sin(body.yRot) * 5F;
            rightArm.x = -Mth.cos(body.yRot) * 5F;
            rightArm.yRot += body.yRot;
        }
        if (!keepShieldRaised || attackArm == HumanoidArm.LEFT)
        {
            leftArm.z = -Mth.sin(body.yRot) * 5F;
            leftArm.x = Mth.cos(body.yRot) * 5F;
            leftArm.yRot += body.yRot;
            leftArm.xRot += body.yRot;
        }

        float swing = 1F - attackTime;
        swing *= swing;
        swing *= swing;
        swing = 1F - swing;
        float lift = Mth.sin(swing * Mth.PI);
        float headFollow = Mth.sin(attackTime * Mth.PI) * -(head.xRot - 0.7F) * 0.75F;
        arm.xRot -= lift * 1.2F + headFollow;
        arm.yRot += body.yRot * 2F;
        arm.zRot += Mth.sin(attackTime * Mth.PI) * -0.4F;
    }

    /** Mirrors the swimming and crawling part of HumanoidModel.setupAnim */
    private static void animateSwimming(LivingEntity p, float swimAmount, float limbSwing, float attackTime, Part rightArm, Part leftArm, Part rightLeg, Part leftLeg)
    {
        float cycle = limbSwing % 26F;
        HumanoidArm attackArm = attackArm(p);
        float rightBlend = attackArm == HumanoidArm.RIGHT && attackTime > 0F ? 0F : swimAmount;
        float leftBlend = attackArm == HumanoidArm.LEFT && attackTime > 0F ? 0F : swimAmount;

        if (!p.isUsingItem())
        {
            if (cycle < 14F)
            {
                float stroke = 1.8707964F * quadraticArmUpdate(cycle) / quadraticArmUpdate(14F);
                leftArm.xRot = rotLerpRad(leftBlend, leftArm.xRot, 0F);
                rightArm.xRot = Mth.lerp(rightBlend, rightArm.xRot, 0F);
                leftArm.yRot = rotLerpRad(leftBlend, leftArm.yRot, Mth.PI);
                rightArm.yRot = Mth.lerp(rightBlend, rightArm.yRot, Mth.PI);
                leftArm.zRot = rotLerpRad(leftBlend, leftArm.zRot, Mth.PI + stroke);
                rightArm.zRot = Mth.lerp(rightBlend, rightArm.zRot, Mth.PI - stroke);
            }
            else if (cycle < 22F)
            {
                float progress = (cycle - 14F) / 8F;
                leftArm.xRot = rotLerpRad(leftBlend, leftArm.xRot, Mth.PI / 2F * progress);
                rightArm.xRot = Mth.lerp(rightBlend, rightArm.xRot, Mth.PI / 2F * progress);
                leftArm.yRot = rotLerpRad(leftBlend, leftArm.yRot, Mth.PI);
                rightArm.yRot = Mth.lerp(rightBlend, rightArm.yRot, Mth.PI);
                leftArm.zRot = rotLerpRad(leftBlend, leftArm.zRot, 5.012389F - 1.8707964F * progress);
                rightArm.zRot = Mth.lerp(rightBlend, rightArm.zRot, 1.2707963F + 1.8707964F * progress);
            }
            else
            {
                float progress = (cycle - 22F) / 4F;
                leftArm.xRot = rotLerpRad(leftBlend, leftArm.xRot, Mth.PI / 2F - Mth.PI / 2F * progress);
                rightArm.xRot = Mth.lerp(rightBlend, rightArm.xRot, Mth.PI / 2F - Mth.PI / 2F * progress);
                leftArm.yRot = rotLerpRad(leftBlend, leftArm.yRot, Mth.PI);
                rightArm.yRot = Mth.lerp(rightBlend, rightArm.yRot, Mth.PI);
                leftArm.zRot = rotLerpRad(leftBlend, leftArm.zRot, Mth.PI);
                rightArm.zRot = Mth.lerp(rightBlend, rightArm.zRot, Mth.PI);
            }
        }

        leftLeg.xRot = Mth.lerp(swimAmount, leftLeg.xRot, 0.3F * Mth.cos(limbSwing * 0.33333334F + Mth.PI));
        rightLeg.xRot = Mth.lerp(swimAmount, rightLeg.xRot, 0.3F * Mth.cos(limbSwing * 0.33333334F));
    }

    private static float quadraticArmUpdate(float limbSwing)
    {
        return -65F * limbSwing + limbSwing * limbSwing;
    }

    /** Interpolates towards {@code to} along the shortest way around the circle */
    private static float rotLerpRad(float amount, float from, float to)
    {
        float delta = (to - from) % Mth.TWO_PI;
        if (delta < -Mth.PI)
            delta += Mth.TWO_PI;
        if (delta >= Mth.PI)
            delta -= Mth.TWO_PI;
        return from + amount * delta;
    }

    /**
     * Positions a model part like ModelPart.translateAndRotate, after moving it by its proportions' offset as a part
     * with its own offset is drawn, and scales it about its pivot.
     */
    private static Matrix4f partTransform(Matrix4f root, Part part, HumanoidBody.Part shape)
    {
        return new Matrix4f(root).translate((part.x + shape.offsetX()) * PIXEL, (part.y + shape.offsetY()) * PIXEL, (part.z + shape.offsetZ()) * PIXEL).rotateZYX(part.zRot, part.yRot, part.xRot)
            .scale(shape.scaleX(), shape.scaleY(), shape.scaleZ());
    }

    /** Adds a box given in model pixels */
    private void addBox(Matrix4f transform, float minX, float minY, float minZ, float sizeX, float sizeY, float sizeZ, EnumHitboxType type)
    {
        hitboxes.add(new PlayerHitbox(entity, transform, new Vector3f(minX, minY, minZ).mul(PIXEL), new Vector3f(sizeX, sizeY, sizeZ).mul(PIXEL), vel, type));
    }

    /**
     * Shield origin and dimensions are in gun model coordinates, so the box follows the third person item path:
     * ItemInHandLayer's hand transform, then GunItemRenderer's third person adjustments and model scale.
     * Reload and melee animation movements are ignored.
     */
    private void addShieldBox(ItemStack stack, Matrix4f armTransform, boolean leftHand, EnumHitboxType type)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
            return;

        GunType gunType = gunItem.getConfigType();
        if (!gunType.isShield())
            return;

        Matrix4f transform = new Matrix4f(armTransform).rotateX(-Mth.PI / 2F).rotateY(Mth.PI).translate((leftHand ? -1F : 1F) / 16F, 0.125F, -0.625F).rotateY(Mth.PI / 2F).translate(-0.08F, -0.12F,
            0F);

        com.flansmod.common.vector.Vector3f thirdPersonOffset = gunType.getAnimationConfig().getThirdPersonOffset();
        if (thirdPersonOffset != null)
            transform.translate(thirdPersonOffset.x, thirdPersonOffset.y, thirdPersonOffset.z);
        transform.scale(gunType.getModelScale());

        com.flansmod.common.vector.Vector3f origin = gunType.getShieldOrigin();
        com.flansmod.common.vector.Vector3f dimensions = gunType.getShieldDimensions();
        hitboxes.add(new PlayerHitbox(entity, transform, new Vector3f(origin.x, origin.y, origin.z), new Vector3f(dimensions.x, dimensions.y, dimensions.z), vel, type));
    }

    public List<BulletHit> raytrace(Vector3f origin, Vector3f motion)
    {
        return raytrace(origin, motion, 0F, 1F);
    }

    public List<BulletHit> raytrace(Vec3 origin, Vec3 motion, float lowerBound, float upperBound)
    {
        return raytrace(JomlUtils.fromVec3(origin), JomlUtils.fromVec3(motion), lowerBound, upperBound);
    }

    public List<BulletHit> raytrace(Vector3f origin, Vector3f motion, float lowerBound, float upperBound)
    {
        // Prepare a list for the hits
        List<BulletHit> hits = new ArrayList<>();

        if (upperBound <= lowerBound)
            return hits;

        // Get the bullet raytrace vector into local coordinates
        Vector3f localOrigin = new Vector3f(origin).sub(pos);

        // Check each hitbox for a hit
        for (PlayerHitbox hitbox : hitboxes)
        {
            PlayerBulletHit hit = hitbox.raytrace(localOrigin, motion);
            if (hit != null && hit.getIntersectTime() >= lowerBound && hit.getIntersectTime() <= upperBound)
            {
                hits.add(hit);
            }
        }

        return hits;
    }

    /** Vanilla arm poses plus this mod's gun aiming poses (AIM also covers drawing a bow) */
    private enum ArmPose
    {
        EMPTY(false), ITEM(false), BLOCK(false), AIM(true), ONE_AIM(false), BOTH_AIM(true), THROW_SPEAR(false), THROW(false), SUPPORT(false), CROSSBOW_CHARGE(true), CROSSBOW_HOLD(true), SPYGLASS(
            false), TOOT_HORN(false), BRUSH(false);

        private final boolean twoHanded;

        ArmPose(boolean twoHanded)
        {
            this.twoHanded = twoHanded;
        }
    }

    /** Pivot in model pixels and rotation in radians of one model part */
    private static final class Part
    {
        private float x;
        private float y;
        private float z;
        private float xRot;
        private float yRot;
        private float zRot;

        private Part(float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void rotate(ProneArmPoses.Rotation rotation)
        {
            xRot = rotation.x();
            yRot = rotation.y();
            zRot = rotation.z();
        }
    }
}
