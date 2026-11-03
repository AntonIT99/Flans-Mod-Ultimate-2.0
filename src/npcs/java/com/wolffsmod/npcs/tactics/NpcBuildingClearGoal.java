package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Follows an enemy that broke contact by going into a building. While the NPC fights, it remembers where its target was last
 * seen indoors; when it loses the target, it walks through the door to that place instead of turning back, and gives up when
 * it gets there, after a while, or when it finds someone to fight. Only NPCs that open doors do this. Server side.
 */
public final class NpcBuildingClearGoal extends Goal
{
    /** Ticks after losing the target during which the NPC still goes after it. */
    private static final long MEMORY_TICKS = 600L;
    /** Ticks the search may last once begun. */
    private static final int SEARCH_TICKS = 400;
    private static final double ARRIVAL_SQR = 4D;
    private static final double MAX_DISTANCE_SQR = 48D * 48D;

    private final EntityNPCInterface npc;
    @Nullable
    private Vec3 lastSeen;
    private long lostAt;
    private boolean wasIndoors;
    private int searchTicks;

    public NpcBuildingClearGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        LivingEntity target = npc.getTarget();
        if (target != null)
        {
            remember(target);
            return false;
        }
        return lastSeen != null && wasIndoors && NpcBuildings.entersBuildings(npc) && !TeamsBots.isCommander(npc) && npc.level().getGameTime() - lostAt <= MEMORY_TICKS
            && npc.position().distanceToSqr(lastSeen) <= MAX_DISTANCE_SQR;
    }

    /** Cheap while fighting: one cached indoor check per second, and only once the target is out of sight. */
    private void remember(LivingEntity target)
    {
        if (!target.isAlive())
        {
            lastSeen = null;
            return;
        }
        long time = npc.level().getGameTime();
        if ((time + npc.getId()) % 20L != 0L)
            return;
        boolean seen = npc.getSensing().hasLineOfSight(target);
        lastSeen = target.position();
        lostAt = time;
        wasIndoors = !seen && NpcBuildings.indoors(target);
    }

    @Override
    public void start()
    {
        searchTicks = 0;
        if (lastSeen != null && !npc.getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, 1D))
            lastSeen = null;
    }

    @Override
    public boolean canContinueToUse()
    {
        return lastSeen != null && npc.getTarget() == null && searchTicks < SEARCH_TICKS && npc.position().distanceToSqr(lastSeen) > ARRIVAL_SQR;
    }

    @Override
    public void tick()
    {
        searchTicks++;
        // Repath now and then: a door the NPC opens, or one an enemy shuts, changes the route.
        // A route that no longer exists ends the search.
        if (lastSeen != null && (npc.getNavigation().isDone() || searchTicks % 40 == 0) && !npc.getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, 1D))
            lastSeen = null;
    }

    @Override
    public void stop()
    {
        lastSeen = null;
        wasIndoors = false;
        if (npc.getTarget() == null)
            npc.getNavigation().stop();
    }
}
