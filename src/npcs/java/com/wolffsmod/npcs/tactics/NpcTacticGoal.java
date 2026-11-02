package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Moves an NPC that fights with an equipped ranged weapon by its tactic, while {@link NpcEquipmentRangedGoal} aims and
 * fires. It comes before every Custom NPCs goal that moves an NPC in a fight. Within melee range, or with Custom NPCs'
 * tactics selected, it leaves the NPC to them. An NPC firing from its projectile slot follows its tactic from Custom NPCs'
 * ranged attack instead, which moves it as well as firing.
 * Physical commanders also hold their rear position without a ranged weapon, yielding to melee only at close range.
 */
public final class NpcTacticGoal extends Goal
{
    private final EntityNPCInterface npc;
    private Tactic tactic = Tactic.CUSTOM_NPCS;

    public NpcTacticGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        if (TeamsBots.isCommander(npc))
        {
            tactic = Tactic.HOLD;
            LivingEntity target = npc.getTarget();
            return npc.isAlive() && !npc.isKilled() && target != null && target.isAlive() && !npc.isInRange(target, Math.max(2D, npc.stats.melee.getRange()));
        }
        if (!NpcEquipmentRangedGoal.engages(npc))
            return false;
        tactic = NpcTactics.of(npc);
        return NpcTactics.followsPath(tactic);
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        NpcTacticSteering.steer(npc, tactic);
    }

    @Override
    public void stop()
    {
        npc.getNavigation().stop();
    }
}
