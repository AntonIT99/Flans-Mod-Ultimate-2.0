package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.entity.HumanoidBody;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcModels;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.ModelData;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.api.constants.AnimationType;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.Optional;

/**
 * The proportions a Custom NPC drawn with the player model has, so that Flan rounds hit it limb by limb as they hit
 * players. Each part is moved and scaled as {@code ModelDataShared.updateTransate} and {@code ModelScaleRenderer}
 * place it, and the whole model is scaled and raised as {@code RenderNPCInterface} draws it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcHumanoidBody
{
    /** Model pixels per block: part offsets are stored in blocks. */
    private static final float PIXELS = 16F;
    /** The size an NPC has when Custom NPCs draws it at the scale it stores. */
    private static final float DEFAULT_SIZE = 5F;

    /**
     * @return the proportions, or empty while the NPC wears another mob's or a Flan model, has the switch off, sleeps, or
     *         plays an animation that moves its body where the player model does not, outside a seat
     */
    public static Optional<HumanoidBody> of(EntityNPCInterface npc)
    {
        if (!(npc instanceof EntityCustomNpc custom) || custom.modelData == null || custom.modelData.hasEntity() || npc.isSleeping() || !playerPose(npc)
            || !NpcEquipment.enabled(npc, Feature.LIMB_HITBOXES))
            return Optional.empty();

        ModelData model = custom.modelData;
        float size = npc.display.getSize() / DEFAULT_SIZE;
        float bodyY = model.getBodyY();
        float legsY = model.getLegsY();
        ModelPartConfig body = model.body;
        ModelPartConfig leftArm = model.arm1;
        ModelPartConfig rightArm = model.arm1.notShared ? model.arm2 : model.arm1;
        ModelPartConfig leftLeg = model.leg1;
        ModelPartConfig rightLeg = model.leg1.notShared ? model.leg2 : model.leg1;
        return Optional.of(new HumanoidBody(npc.scaleX * size, npc.scaleY * size, npc.scaleZ * size, (float) NpcModels.bodyOffset(npc), part(model.head, 0F, bodyY, 0F), part(body, 0F, bodyY, 0F),
            part(rightArm, armX(body, rightArm), armY(bodyY, rightArm), 0F), part(leftArm, -armX(body, leftArm), armY(bodyY, leftArm), 0F), part(rightLeg, legX(rightLeg), legsY, legZ(rightLeg)),
            part(leftLeg, -legX(leftLeg), legsY, legZ(leftLeg))));
    }

    /** Animations that leave the body where the player model has it; a seated NPC is drawn seated whatever it plays. */
    private static boolean playerPose(EntityNPCInterface npc)
    {
        if (npc.isPassenger())
            return true;
        return switch (npc.currentAnimation)
        {
            case AnimationType.NONE, AnimationType.CROUCH, AnimationType.AIM, AnimationType.WALK, AnimationType.IDLE -> true;
            default -> false;
        };
    }

    /** Takes the part's offset in blocks, as Custom NPCs stores it. */
    private static HumanoidBody.Part part(ModelPartConfig config, float x, float y, float z)
    {
        return new HumanoidBody.Part(x * PIXELS, y * PIXELS, z * PIXELS, config.scaleX, config.scaleY, config.scaleZ);
    }

    private static float armX(ModelPartConfig body, ModelPartConfig arm)
    {
        return (1F - body.scaleX) * 0.25F + (1F - arm.scaleX) * 0.0625F;
    }

    private static float armY(float bodyY, ModelPartConfig arm)
    {
        return bodyY + (1F - arm.scaleY) * -0.125F;
    }

    private static float legX(ModelPartConfig leg)
    {
        return (1F - leg.scaleX) * 0.118F;
    }

    private static float legZ(ModelPartConfig leg)
    {
        return -(1F - leg.scaleZ) * 0.00625F;
    }
}
