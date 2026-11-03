package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.tactics.NpcWeaponRange;
import com.wolffsmod.npcs.teams.NpcSuppression;
import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Brings an NPC that fights with an equipped ranged weapon into position: it closes in until its target is within firing
 * range and in sight, then stands while {@link NpcEquipmentRangedGoal} fires. It runs at the priority of Custom NPCs' own
 * attack goal, below the tactical movement Custom NPCs chose, which takes the NPC over whenever it has somewhere to go. A
 * soldier pinned down by fire stands too.
 */
public final class NpcCombatApproachGoal extends Goal
{
    private final EntityNPCInterface npc;

    public NpcCombatApproachGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        // A soldier on its way to an objective leaves its movement to the objective goal and fires as it goes.
        return !TeamsBots.isCommander(npc) && NpcEquipmentRangedGoal.engages(npc) && !TeamsBots.movesUnderFire(npc);
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        LivingEntity target = npc.getTarget();
        if (target == null)
            return;
        // Pinned down by fire, it moves no closer until the fire lets up.
        if (!NpcSuppression.isPinned(npc) && (!npc.isInRange(target, NpcWeaponRange.firing(npc)) || !npc.getSensing().hasLineOfSight(target)))
            npc.getNavigation().moveTo(target, 1D);
        else
            npc.getNavigation().stop();
    }

    @Override
    public void stop()
    {
        npc.getNavigation().stop();
    }
}
