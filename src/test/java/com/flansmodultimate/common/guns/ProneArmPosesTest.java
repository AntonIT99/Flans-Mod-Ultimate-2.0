package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.guns.GunArmPoses.Arm;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import net.minecraft.util.Mth;

import static org.junit.jupiter.api.Assertions.*;

class ProneArmPosesTest
{
    /** A part's own downward axis, along which an arm hangs. */
    private static final Vector3f ARM = new Vector3f(0F, 1F, 0F);
    /** The way a head faces. */
    private static final Vector3f FACE = new Vector3f(0F, 0F, -1F);

    @Test
    void aProneGunPointsWhereAStandingOneWould()
    {
        for (float pitch : new float[]{-0.8F, 0F, 0.6F})
        {
            for (float yaw : new float[]{-0.7F, 0F, 0.5F})
            {
                ProneArmPoses.Result prone = ProneArmPoses.resolve(new GunArmPoses.Result(Arm.ONE_ARM, Arm.NONE), true, pitch, yaw, 1F).orElseThrow();
                ProneArmPoses.Rotation standingArm = new ProneArmPoses.Rotation(pitch - Mth.HALF_PI, yaw, 0F);
                assertSameDirection(standing(standingArm, ARM), lying(prone.rightArm(), ARM));
                assertSameDirection(standing(new ProneArmPoses.Rotation(pitch, yaw, 0F), FACE), lying(prone.head(), FACE));
                assertNull(prone.leftArm());
            }
        }
    }

    @Test
    void theBowPoseBringsTheFreeArmAcross()
    {
        ProneArmPoses.Result prone = ProneArmPoses.resolve(new GunArmPoses.Result(Arm.BOW, Arm.NONE), false, 0F, 0F, 1F).orElseThrow();
        assertNotNull(prone.rightArm());
        assertNotNull(prone.leftArm());
    }

    @Test
    void nothingChangesStandingOrWithoutARaisedGun()
    {
        assertTrue(ProneArmPoses.resolve(new GunArmPoses.Result(Arm.ONE_ARM, Arm.NONE), true, 0F, 0F, 0F).isEmpty());
        assertTrue(ProneArmPoses.resolve(new GunArmPoses.Result(Arm.RELAXED, Arm.NONE), true, 0F, 0F, 1F).isEmpty());
    }

    private static Vector3f standing(ProneArmPoses.Rotation rotation, Vector3f axis)
    {
        return part(rotation).transform(new Vector3f(axis));
    }

    /** The renderer's quarter turn forward, as it reaches model space through the model's flip of X and Y. */
    private static Vector3f lying(ProneArmPoses.Rotation rotation, Vector3f axis)
    {
        return new Matrix3f().rotationX(Mth.HALF_PI).mul(part(rotation)).transform(new Vector3f(axis));
    }

    /** As {@code ModelPart.translateAndRotate} turns a part. */
    private static Matrix3f part(ProneArmPoses.Rotation rotation)
    {
        return new Matrix3f().rotationZYX(rotation.z(), rotation.y(), rotation.x());
    }

    private static void assertSameDirection(Vector3f expected, Vector3f actual)
    {
        assertEquals(0F, expected.distance(actual), 1E-4F, () -> expected + " != " + actual);
    }
}
