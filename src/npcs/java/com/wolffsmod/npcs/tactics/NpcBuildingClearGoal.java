package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Follows an enemy that broke contact by going into a building. While the NPC fights, it remembers where it last saw its
 * target; when the target vanishes indoors, it walks through the door to that place instead of turning back, tells idle
 * friends nearby to come along, looks round the room when it gets there, and gives up after a while, when the target dies, or
 * when it finds someone to fight. Only NPCs that open doors do this. Server side.
 */
public final class NpcBuildingClearGoal extends Goal
{
    /** Ticks after losing the target during which the NPC still goes after it. */
    private static final long MEMORY_TICKS = 600L;
    /** Ticks the search may last once begun. */
    private static final int SEARCH_TICKS = 400;
    /** Ticks spent looking round on arrival. */
    private static final int LOOK_TICKS = 40;
    private static final double ARRIVAL_SQR = 4D;
    private static final double MAX_DISTANCE_SQR = 48D * 48D;
    /** Friends within this distance of the NPC join its search. */
    private static final double SHARE_RANGE = 16D;
    /** Goals of loaded NPCs, so that a sighting can be passed to friends. */
    private static final Map<EntityNPCInterface, NpcBuildingClearGoal> GOALS = new WeakHashMap<>();

    private final EntityNPCInterface npc;
    @Nullable
    private LivingEntity enemy;
    @Nullable
    private Vec3 lastSeen;
    private long lostAt;
    private boolean wasIndoors;
    private int searchTicks;
    private int lookTicks;

    public NpcBuildingClearGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        synchronized (GOALS)
        {
            GOALS.put(npc, this);
        }
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
        if (enemy != null && !enemy.isAlive())
            forget();
        return lastSeen != null && wasIndoors && NpcBuildings.entersBuildings(npc) && !TeamsBots.isCommander(npc) && npc.level().getGameTime() - lostAt <= MEMORY_TICKS
            && npc.position().distanceToSqr(lastSeen) <= MAX_DISTANCE_SQR;
    }

    /** Cheap while fighting: one sight check per second, and the cached indoor check only once the target is out of sight. */
    private void remember(LivingEntity target)
    {
        if (!target.isAlive())
        {
            forget();
            return;
        }
        long time = npc.level().getGameTime();
        if ((time + npc.getId()) % 20L != 0L)
            return;
        if (target != enemy)
        {
            enemy = target;
            lastSeen = null;
            wasIndoors = false;
        }
        // Only a sighting tells where the enemy is; the NPC does not follow it through walls.
        if (npc.getSensing().hasLineOfSight(target))
        {
            lastSeen = target.position();
            lostAt = time;
            wasIndoors = false;
        }
        else if (lastSeen != null && !wasIndoors)
            wasIndoors = NpcBuildings.indoors(npc.level(), target.blockPosition()) || NpcBuildings.indoors(npc.level(), BlockPos.containing(lastSeen));
    }

    private void forget()
    {
        enemy = null;
        lastSeen = null;
        wasIndoors = false;
    }

    @Override
    public void start()
    {
        searchTicks = 0;
        lookTicks = 0;
        if (lastSeen != null && !npc.getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, 1D))
            lastSeen = null;
        else
            share();
    }

    /** Idle friends of the same faction nearby clear the building together with the NPC instead of leaving it alone. */
    private void share()
    {
        if (lastSeen == null)
            return;
        for (EntityNPCInterface other : npc.level().getEntitiesOfClass(EntityNPCInterface.class, npc.getBoundingBox().inflate(SHARE_RANGE),
            other -> other != npc && other.isAlive() && other.getTarget() == null && other.faction.id == npc.faction.id))
        {
            NpcBuildingClearGoal goal;
            synchronized (GOALS)
            {
                goal = GOALS.get(other);
            }
            if (goal != null && goal.lastSeen == null)
            {
                goal.enemy = enemy;
                goal.lastSeen = lastSeen;
                goal.lostAt = lostAt;
                goal.wasIndoors = true;
            }
        }
    }

    @Override
    public boolean canContinueToUse()
    {
        return lastSeen != null && npc.getTarget() == null && (enemy == null || enemy.isAlive()) && searchTicks < SEARCH_TICKS && lookTicks < LOOK_TICKS;
    }

    @Override
    public void tick()
    {
        searchTicks++;
        if (lastSeen == null)
            return;
        if (lookTicks > 0 || npc.position().distanceToSqr(lastSeen) <= ARRIVAL_SQR)
        {
            // Arrived: stop and look round the room before giving up.
            if (lookTicks++ == 0)
                npc.getNavigation().stop();
            float yaw = npc.getYRot() + (lookTicks / 10 % 2 == 0 ? 9F : -9F);
            npc.getLookControl().setLookAt(npc.getX() - Math.sin(Math.toRadians(yaw)) * 4D, npc.getEyeY(), npc.getZ() + Math.cos(Math.toRadians(yaw)) * 4D);
            return;
        }
        // Repath now and then: a door the NPC opens, or one an enemy shuts, changes the route.
        // A route that no longer exists ends the search.
        if ((npc.getNavigation().isDone() || searchTicks % 40 == 0) && !npc.getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, 1D))
            lastSeen = null;
    }

    @Override
    public void stop()
    {
        forget();
        if (npc.getTarget() == null)
            npc.getNavigation().stop();
    }
}
