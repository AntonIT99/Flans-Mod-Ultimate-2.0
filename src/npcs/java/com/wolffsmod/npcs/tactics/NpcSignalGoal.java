package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Keeps an NPC that waits for its start signal where it was placed, ahead of every goal that would move it: Custom NPCs'
 * wandering, moving path and return home, boarding and the Assault position route. In a fight at range its tactic holds it
 * there instead, so that Custom NPCs' ranged attack, which also needs to move it, still fires; a soldier that fights in
 * melee is held until an enemy comes within its reach. Server side.
 */
public final class NpcSignalGoal extends Goal
{
    /** Ticks before the NPC is sent home again when it has stopped short of it. */
    static final int REPATH_TICKS = 20;

    private final EntityNPCInterface npc;
    private long orderedAt = Long.MIN_VALUE / 2;

    public NpcSignalGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        if (!NpcSignal.waiting(npc) || npc.level().isClientSide || !npc.isAlive() || npc.isKilled() || npc.isPassenger() || npc.getOwner() != null)
            return false;
        LivingEntity target = npc.getTarget();
        if (target == null)
            return true;
        // A fight at range is left to the tactic, which holds it; a melee fighter strikes what reaches it.
        return !NpcEquipment.fightsAtRange(npc) && !npc.isInRange(target, Math.max(2D, npc.stats.melee.getRange()) + 1D);
    }

    @Override
    public boolean canContinueToUse()
    {
        return canUse();
    }

    @Override
    public void tick()
    {
        Vec3 home = new Vec3(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
        PathNavigation navigation = npc.getNavigation();
        if (TacticDecision.horizontal(npc.position(), home) <= TacticDecision.HOLD_RADIUS)
        {
            navigation.stop();
            return;
        }
        long now = npc.level().getGameTime();
        if (navigation.isDone() && now - orderedAt >= REPATH_TICKS)
        {
            navigation.moveTo(home.x, home.y, home.z, TacticDecision.WALK);
            orderedAt = now;
        }
    }

    @Override
    public void stop()
    {
        npc.getNavigation().stop();
        orderedAt = Long.MIN_VALUE / 2;
    }
}
