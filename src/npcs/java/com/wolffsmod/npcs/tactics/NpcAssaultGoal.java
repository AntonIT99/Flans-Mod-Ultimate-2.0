package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.teams.BotTag;
import com.wolffsmod.npcs.teams.NpcSuppression;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Walks an NPC on the Assault position tactic along its route while it has nobody to fight, and holds it at the end. It
 * comes before Custom NPCs' own return home, moving path and wandering, which would otherwise take it back after every
 * fight, and keeps running at the end of the route so that none of them starts. In a fight its tactic moves it instead.
 * Teams bots and enlisted NPCs follow their squad's orders. Server side.
 */
public final class NpcAssaultGoal extends Goal
{
    /** Ticks before the NPC is sent again to a point it has stopped short of. */
    static final int REPATH_TICKS = 20;
    /** Blocks the point may move before the NPC is sent there again. */
    private static final double REPATH_DISTANCE_SQR = 1D;

    private final EntityNPCInterface npc;
    @Nullable
    private Vec3 ordered;
    private long orderedAt;

    public NpcAssaultGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        return free() && NpcAssault.next(npc).isPresent();
    }

    @Override
    public boolean canContinueToUse()
    {
        return canUse();
    }

    @Override
    public void start()
    {
        ordered = null;
    }

    @Override
    public void tick()
    {
        Optional<NpcAssault.Point> next = NpcAssault.next(npc);
        if (next.isEmpty())
            return;
        Vec3 spot = next.get().spot();
        PathNavigation navigation = npc.getNavigation();
        if (next.get().last() && TacticDecision.horizontal(npc.position(), spot) <= TacticDecision.HOLD_RADIUS)
        {
            navigation.stop();
            ordered = null;
            return;
        }
        long now = npc.level().getGameTime();
        boolean retry = navigation.isDone() && now - orderedAt >= REPATH_TICKS;
        if (ordered == null || ordered.distanceToSqr(spot) > REPATH_DISTANCE_SQR || retry)
        {
            navigation.moveTo(spot.x, spot.y, spot.z, TacticDecision.WALK);
            ordered = spot;
            orderedAt = now;
        }
    }

    @Override
    public void stop()
    {
        npc.getNavigation().stop();
        ordered = null;
    }

    /** Whether the NPC is free to go on: set to the tactic, alive, on foot or driving, with nobody to fight, following nobody and not pinned down. */
    private boolean free()
    {
        return NpcAssault.assigned(npc) && !npc.level().isClientSide && npc.isAlive() && !npc.isKilled() && npc.getTarget() == null && npc.getOwner() == null
            && (!npc.isPassenger() || NpcDrivers.isDriver(npc)) && !BotTag.isBot(npc) && !NpcSuppression.isPinned(npc);
    }
}
