package com.flansmodultimate.common.driveables;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValkyrieAnimationTest
{
    private static final float EPSILON = 1.0E-4F;
    private static final int LEFT_LEG_MID = 4;
    private static final int LEFT_WING = 13;
    private static final int RIGHT_WING = 14;
    private static final int LEFT_HAND = 27;

    @Test
    void theSkeletonHasTheLegacyHierarchy()
    {
        ValkyrieAnimation animation = new ValkyrieAnimation();
        assertEquals(ValkyrieAnimation.PART_COUNT, animation.getParts().size());
        assertEquals(-1, animation.getCore().getParent());
        assertEquals(4, animation.getParts().get(ValkyrieAnimation.LEFT_LEG_SHIN).getParent());
        assertEquals(26, animation.getParts().get(LEFT_HAND).getParent());
        assertEquals(6, animation.getCore().getChildren().size());
    }

    @Test
    void fighterModeSweepsTheWingsAndStowsEverythingElse()
    {
        ValkyrieAnimation animation = settle(true);
        assertVector(animation.getParts().get(LEFT_WING).getRotation(), 0F, 30F, 0F);
        assertVector(animation.getParts().get(RIGHT_WING).getRotation(), 0F, -30F, 0F);
        assertVector(animation.getParts().get(LEFT_LEG_MID).getOffset(), 0F, 0F, 0F);
        assertVector(animation.getParts().get(LEFT_HAND).getRotation(), 0F, 0F, 0F);
    }

    @Test
    void gerwalkStepsThroughEveryStageToTheFinalPose()
    {
        ValkyrieAnimation animation = settle(false);
        // GERWALK 4: legs from stage 2, arms deployed with the hands flipped.
        assertVector(animation.getParts().get(LEFT_LEG_MID).getOffset(), 10F, 0F, 0F);
        assertVector(animation.getParts().get(LEFT_LEG_MID).getRotation(), -20F, 0F, -35F);
        assertVector(animation.getParts().get(LEFT_HAND).getOffset(), 23F, 0F, 0F);
        assertVector(animation.getParts().get(LEFT_HAND).getRotation(), 180F, 0F, 0F);
        assertVector(animation.getParts().get(LEFT_WING).getRotation(), 0F, 0F, 0F);
    }

    @Test
    void jointsEaseAtTheirPoseRateAndInterpolateBetweenTicks()
    {
        ValkyrieAnimation animation = new ValkyrieAnimation();
        animation.tick(false);
        // GERWALK 1 turns the left shin to -100 degrees at 16 degrees per tick.
        ValkyrieAnimation.Part shin = animation.getParts().get(ValkyrieAnimation.LEFT_LEG_SHIN);
        assertVector(shin.getRotation(), 0F, 0F, -16F);
        assertVector(shin.getRotation(0.5F), 0F, 0F, -8F);
        assertVector(shin.getOffset(), 2F, 0F, 0F);
    }

    @Test
    void switchingBackToFighterReturnsToTheFighterPose()
    {
        ValkyrieAnimation animation = settle(false);
        for (int tick = 0; tick < 400; tick++)
            animation.tick(true);
        assertVector(animation.getParts().get(LEFT_HAND).getOffset(), 0F, 0F, 0F);
        assertVector(animation.getParts().get(LEFT_HAND).getRotation(), 0F, 0F, 0F);
        assertVector(animation.getParts().get(LEFT_WING).getRotation(), 0F, 30F, 0F);
    }

    @Test
    void footExhaustsAreMirroredAndInPlaneBlocks()
    {
        ValkyrieAnimation animation = new ValkyrieAnimation();
        Vector3f left = animation.leftFootExhaust();
        Vector3f right = animation.rightFootExhaust();
        assertNotNull(left);
        assertNotNull(right);
        // At rest the chain telescopes back to the nozzle point, (151, -25, +-24) model units.
        assertVector(left, -151F / 16F, 25F / 16F, -24F / 16F);
        assertVector(right, -151F / 16F, 25F / 16F, 24F / 16F);
    }

    private static ValkyrieAnimation settle(boolean fighterMode)
    {
        ValkyrieAnimation animation = new ValkyrieAnimation();
        for (int tick = 0; tick < 400; tick++)
            animation.tick(fighterMode);
        return animation;
    }

    private static void assertVector(Vector3f actual, float x, float y, float z)
    {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }
}
