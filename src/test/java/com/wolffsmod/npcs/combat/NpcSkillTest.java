package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.teams.BotSettings;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcSkillTest
{
    @Test
    void noAimErrorLeavesTheDirectionAsItIs()
    {
        Vec3 direction = new Vec3(0.3D, -0.2D, 0.9D);
        assertSame(direction, NpcSkill.AimError.NONE.apply(direction));
    }

    @Test
    void anAimErrorTurnsTheDirectionByItsAnglesAndKeepsItsLength()
    {
        Vec3 north = new Vec3(0D, 0D, 2D);
        Vec3 turned = new NpcSkill.AimError(90F, 0F).apply(north);
        assertEquals(2D, turned.length(), 1.0E-9D);
        assertEquals(2D, turned.x, 1.0E-9D);
        assertEquals(0D, turned.z, 1.0E-9D);

        Vec3 raised = new NpcSkill.AimError(0F, 30F).apply(north);
        assertEquals(2D, raised.length(), 1.0E-9D);
        assertEquals(Math.toRadians(30D), Math.atan2(raised.y, raised.z), 1.0E-9D);
    }

    /** An untouched NPC fights without limits, as it did before NPCs had limits of their own. */
    @Test
    void anUntouchedNpcsOwnLimitsAreNone()
    {
        NpcTuning tuning = new NpcTuning();
        assertEquals(NpcSkill.Level.ACE, NpcSkill.Level.values()[tuning.get(Value.SKILL_LEVEL)]);
        assertEquals(NpcSkill.NONE, NpcSkill.Level.ACE.getSkill());
        assertEquals(0, tuning.get(Value.REACTION_TIME));
        assertEquals(0, tuning.get(Value.AIM_SETTLE_TIME));
        assertEquals(0, tuning.get(Value.AIM_ERROR));
        assertEquals(100, tuning.get(Value.LEAD_SHARE));
        assertEquals(100, tuning.get(Value.PLAYER_DAMAGE));
        assertEquals(NpcTuning.Duty.NONE, tuning.duty());
        assertEquals(30, tuning.get(Value.RETREAT_HEALTH));
    }

    /** Every level fits the settings it fills in, in tenths of a degree and percent, without losing anything on the way. */
    @Test
    void everyLevelFitsTheOwnLimitsSettings()
    {
        for (NpcSkill.Level level : NpcSkill.Level.values())
        {
            NpcSkill skill = level.getSkill();
            assertEquals(skill.reactionTicks(), Value.REACTION_TIME.clamp(skill.reactionTicks()), level.name());
            assertEquals(skill.settleTicks(), Value.AIM_SETTLE_TIME.clamp(skill.settleTicks()), level.name());
            assertEquals(skill.startError(), Value.AIM_ERROR.clamp(Math.round(skill.startError() * 10F)) / 10F, 1.0E-6F, level.name());
            assertEquals(skill.lead(), Value.LEAD_SHARE.clamp(Math.round(skill.lead() * 100F)) / 100F, 1.0E-6F, level.name());
            assertEquals(skill.playerDamage(), Value.PLAYER_DAMAGE.clamp(Math.round(skill.playerDamage() * 100F)) / 100F, 1.0E-6F, level.name());
        }
    }

    @Test
    void teamsDifficultiesTakeTheLimitsOfTheLevelOfTheSameName()
    {
        for (BotSettings.Difficulty difficulty : BotSettings.Difficulty.values())
            assertEquals(NpcSkill.Level.valueOf(difficulty.name()).getSkill(), difficulty.skill(), difficulty.name());
    }

    @Test
    void npcsFightPlayersOnPeacefulUnlessAnOperatorSaysOtherwise()
    {
        assertEquals(1, NpcServerSettings.Setting.PEACEFUL_COMBAT.getFallback());
    }
}
