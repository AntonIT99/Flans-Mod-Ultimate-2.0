package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.guns.GunArmPoses.Arm;
import com.flansmodultimate.common.guns.GunArmPoses.HandItem;
import com.flansmodultimate.common.guns.GunArmPoses.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GunArmPosesTest
{
    private static Result enforced(HandItem main, HandItem off)
    {
        return GunArmPoses.resolve(main, off, false, false, false);
    }

    private static Result dynamic(HandItem main, HandItem off, boolean mainActive, boolean offActive)
    {
        return GunArmPoses.resolve(main, off, true, mainActive, offActive);
    }

    @Test
    void gunWithFreeOtherHandTakesTheBowPose()
    {
        assertEquals(new Result(Arm.BOW, Arm.NONE), enforced(HandItem.GUN, HandItem.EMPTY));
        assertEquals(new Result(Arm.NONE, Arm.BOW), enforced(HandItem.EMPTY, HandItem.GUN));
    }

    @Test
    void shieldRaisesOnlyItsOwnArm()
    {
        assertEquals(new Result(Arm.ONE_ARM, Arm.NONE), enforced(HandItem.SHIELD, HandItem.EMPTY));
        assertEquals(new Result(Arm.NONE, Arm.ONE_ARM), enforced(HandItem.OTHER, HandItem.SHIELD));
    }

    @Test
    void twoGunsAimWithBothArms()
    {
        assertEquals(new Result(Arm.BOTH, Arm.BOTH), enforced(HandItem.GUN, HandItem.GUN));
    }

    @Test
    void shieldArmIsNeverDrawnIntoAGunPose()
    {
        Result eachOwnArm = new Result(Arm.ONE_ARM, Arm.ONE_ARM);
        assertEquals(eachOwnArm, enforced(HandItem.GUN, HandItem.SHIELD));
        assertEquals(eachOwnArm, enforced(HandItem.SHIELD, HandItem.GUN));
        assertEquals(eachOwnArm, enforced(HandItem.SHIELD, HandItem.SHIELD));
    }

    @Test
    void gunNextToAnotherItemRaisesOnlyItsOwnArm()
    {
        assertEquals(new Result(Arm.ONE_ARM, Arm.NONE), enforced(HandItem.GUN, HandItem.OTHER));
        assertEquals(new Result(Arm.NONE, Arm.ONE_ARM), enforced(HandItem.OTHER, HandItem.GUN));
    }

    @Test
    void nothingToRaise()
    {
        assertEquals(Result.NONE, enforced(HandItem.EMPTY, HandItem.EMPTY));
        assertEquals(Result.NONE, enforced(HandItem.OTHER, HandItem.OTHER));
    }

    @Test
    void dynamicGunIsLoweredUntilFiredOrAimed()
    {
        assertEquals(Result.NONE, dynamic(HandItem.GUN, HandItem.EMPTY, false, false));
        assertEquals(new Result(Arm.BOW, Arm.NONE), dynamic(HandItem.GUN, HandItem.EMPTY, true, false));
        assertEquals(new Result(Arm.ONE_ARM, Arm.NONE), dynamic(HandItem.GUN, HandItem.OTHER, true, false));
    }

    @Test
    void dynamicShieldStaysRaised()
    {
        assertEquals(new Result(Arm.ONE_ARM, Arm.NONE), dynamic(HandItem.SHIELD, HandItem.EMPTY, false, false));
        // The idle gun beside it stays down; firing it raises the gun arm alone
        assertEquals(new Result(Arm.NONE, Arm.ONE_ARM), dynamic(HandItem.GUN, HandItem.SHIELD, false, false));
        assertEquals(new Result(Arm.ONE_ARM, Arm.ONE_ARM), dynamic(HandItem.GUN, HandItem.SHIELD, true, false));
    }

    @Test
    void dynamicDualGunsRaiseOnlyTheActiveOne()
    {
        assertEquals(new Result(Arm.NONE, Arm.ONE_ARM), dynamic(HandItem.GUN, HandItem.GUN, false, true));
        assertEquals(new Result(Arm.BOTH, Arm.BOTH), dynamic(HandItem.GUN, HandItem.GUN, true, true));
    }
}
