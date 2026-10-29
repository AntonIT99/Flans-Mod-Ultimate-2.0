package com.flansmodultimate.client.render.item;

import com.flansmodultimate.client.render.PoseScaling;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The frames 1.7.10 set up before a Flan's IItemRenderer drew a held item, rebuilt on top of the modern vanilla hand
 * transforms, so item movements and pack offsets written for 1.7.10 land where they did there.
 *
 * <p>
 * 1.7.10 only had a right-hand frame. A left-hand item is drawn as the exact mirror image of the right-hand one: wrap
 * everything that places it between {@link #beginHand} and {@link #endHand}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LegacyHeldItemFrames
{
    /** Starts a left-hand placement as the mirror image of the right-hand one, as vanilla places the arms symmetrically. */
    public static void beginHand(PoseStack poseStack, boolean leftHand)
    {
        if (leftHand)
            PoseScaling.scale(poseStack, -1F, 1F, 1F);
    }

    /** Ends a mirrored left-hand placement, so the model itself keeps its handedness; legacy models put their side axis on Z. */
    public static void endHand(PoseStack poseStack, boolean leftHand)
    {
        if (leftHand)
            PoseScaling.scale(poseStack, 1F, 1F, -1F);
    }

    /** From vanilla's first-person item origin: the 1.7.10 ItemRenderer turn and scale, then Forge's equipped-item frame. */
    public static void firstPerson(PoseStack poseStack)
    {
        poseStack.mulPose(Axis.YP.rotationDegrees(45F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        equippedItem(poseStack);
    }

    /** From vanilla's right-hand item origin in third person: back to the arm, then 1.7.10 RenderPlayer's full-3D item placement and Forge's equipped-item frame. */
    public static void thirdPerson(PoseStack poseStack)
    {
        // Undo ItemInHandLayer's turn and offset
        poseStack.translate(-1F / 16F, -0.125F, 0.625F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-180F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));

        poseStack.translate(-0.0625F, 0.4375F + 0.1875F, 0.0625F);
        PoseScaling.scale(poseStack, 0.625F, -0.625F, 0.625F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-100F));
        poseStack.mulPose(Axis.YP.rotationDegrees(45F));
        equippedItem(poseStack);
    }

    /** ForgeHooksClient.renderEquippedItem for renderers that do not use the render helper. */
    private static void equippedItem(PoseStack poseStack)
    {
        poseStack.translate(0F, -0.3F, 0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(50F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335F));
        poseStack.translate(-0.9375F, -0.0625F, 0F);
    }
}
