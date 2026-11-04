package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcSkillAimTest
{
    /** Straight at a target 60 blocks north, and the full lead onto one crossing to the east. */
    private static final Vec3 STILL = new Vec3(0D, 0D, 1D);
    private static final Vec3 LED = new Vec3(0.6D, 0D, 0.8D);

    @Test
    void aFullLeadAimsWhereTheTargetIsGoing()
    {
        assertSame(LED, NpcSkill.partialLead(1F, STILL, LED));
    }

    @Test
    void withoutAnyLeadItAimsWhereTheTargetStands()
    {
        assertSame(STILL, NpcSkill.partialLead(0F, STILL, LED));
    }

    @Test
    void aPartialLeadFallsBehindACrossingTarget()
    {
        Vec3 half = NpcSkill.partialLead(0.5F, STILL, LED);
        assertTrue(half.x > 0D && half.x < LED.x, "aims between the target and its run");
        assertEquals(LED.length(), half.length(), 1.0E-6D, "keeps the length of the full solution");
    }

    @Test
    void anErrorOfNoSizeLeavesTheAimAlone()
    {
        NpcSkill.AimError none = NpcSkill.AimError.random(RandomSource.create(1L), 0F);
        assertTrue(none.none());
        assertSame(LED, none.apply(LED));
    }

    @Test
    void errorsAddUpAndStayWithinTheirSizes()
    {
        NpcSkill.AimError unsettled = new NpcSkill.AimError(2F, -1F);
        NpcSkill.AimError unsteady = new NpcSkill.AimError(-0.5F, 3F);
        NpcSkill.AimError both = unsettled.plus(unsteady);
        assertEquals(1.5F, both.yaw(), 1.0E-6F);
        assertEquals(2F, both.pitch(), 1.0E-6F);
        assertSame(unsettled, unsettled.plus(NpcSkill.AimError.NONE));
    }

    @Test
    void anErrorTurnsTheAimWithoutChangingItsLength()
    {
        Vec3 turned = new NpcSkill.AimError(5F, 2F).apply(STILL);
        assertEquals(STILL.length(), turned.length(), 1.0E-6D);
        assertTrue(turned.dot(STILL) < 1D, "it no longer points exactly where it did");
    }

    @Test
    void anErrorStaysWithinTheSizeItWasDrawnFor()
    {
        RandomSource random = RandomSource.create(7L);
        for (int draw = 0; draw < 200; draw++)
        {
            NpcSkill.AimError error = NpcSkill.AimError.random(random, 6F);
            assertTrue(Math.hypot(error.yaw(), error.pitch()) <= 6F + 1.0E-4F);
        }
    }
}
