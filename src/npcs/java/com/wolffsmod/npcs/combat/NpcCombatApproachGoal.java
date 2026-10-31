package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.tactics.NpcWeaponRange;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Brings an NPC that fights with an equipped ranged weapon into position: it closes in until its target is within firing
 * range and in sight, then stands while {@link NpcEquipmentRangedGoal} fires. It runs at the priority of Custom NPCs' own
 * attack goal, below the tactical movement Custom NPCs chose, which takes the NPC over whenever it has somewhere to go.
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
        return NpcEquipmentRangedGoal.engages(npc);
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
        if (!npc.isInRange(target, NpcWeaponRange.firing(npc)) || !npc.getSensing().hasLineOfSight(target))
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
