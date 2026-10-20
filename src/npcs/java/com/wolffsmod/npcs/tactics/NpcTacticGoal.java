package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Moves an NPC that fights with an equipped ranged weapon by its tactic, while {@link NpcEquipmentRangedGoal} aims and
 * fires. It comes before every Custom NPCs goal that moves an NPC in a fight. Within melee range, or with Custom NPCs'
 * tactics selected, it leaves the NPC to them. An NPC firing from its projectile slot follows its tactic from Custom NPCs'
 * ranged attack instead, which moves it as well as firing.
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
