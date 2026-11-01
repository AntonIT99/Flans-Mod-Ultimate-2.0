package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcTuning.Flight;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Paths of NPCs that fly as aircraft. A flying mob's path search tests, at every step through the air, each block its
 * body would fill and the blocks round it; for an aircraft model that is hundreds of blocks a step, and a search toward a
 * target a hundred blocks away takes more than a second. Neither kind of aircraft needs that: a plane flies along its own
 * heading whatever path it is given, and a helicopter keeps its height above the terrain. So a plane is given a straight
 * path to its goal, and so is a helicopter that sees its goal; a helicopter that does not searches as if its body were no
 * larger than {@link #MAX_BODY_WIDTH} by {@link #MAX_BODY_HEIGHT} blocks. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcFlightPaths
{
    /** Blocks across and high an aircraft's body is taken to be, at most, when its path is searched. */
    public static final int MAX_BODY_WIDTH = 3;
    public static final int MAX_BODY_HEIGHT = 2;

    /**
     * @param goals
     *            the blocks a path is wanted to, the nearest of which it goes to
     * @return a straight path from the aircraft to its nearest goal, for a plane and for a helicopter with a clear line to
     *         that goal; null when the path has to be searched
     */
    @Nullable
    public static Path direct(Mob mob, Set<BlockPos> goals)
    {
        if (!(mob instanceof EntityNPCInterface npc) || goals.isEmpty() || npc.isKilled() || npc.level().isClientSide)
            return null;
        Flight flight = NpcFlight.type(npc);
        if (flight == Flight.NORMAL)
            return null;
        BlockPos goal = nearest(npc, goals);
        if (flight == Flight.HELICOPTER && !NpcSight.clear(npc.level(), npc.getEyePosition(), Vec3.atCenterOf(goal), 0, npc))
            return null;
        BlockPos start = npc.blockPosition();
        // Navigation may replace nodes of the path it follows.
        List<Node> nodes = new ArrayList<>(2);
        nodes.add(new Node(start.getX(), start.getY(), start.getZ()));
        nodes.add(new Node(goal.getX(), goal.getY(), goal.getZ()));
        return new Path(nodes, goal, true);
    }

    /** @return whether a path search of the mob takes its body as no larger than the limits: it is an aircraft that keeps its height */
    public static boolean limitsBody(Mob mob)
    {
        return mob instanceof EntityNPCInterface npc && !npc.level().isClientSide && NpcFlight.type(npc) != Flight.NORMAL;
    }

    /** @return the goal nearest the NPC; the set must not be empty */
    private static BlockPos nearest(EntityNPCInterface npc, Set<BlockPos> goals)
    {
        Iterator<BlockPos> remaining = goals.iterator();
        BlockPos nearest = remaining.next();
        double distance = nearest.distToCenterSqr(npc.position());
        while (remaining.hasNext())
        {
            BlockPos goal = remaining.next();
            double squared = goal.distToCenterSqr(npc.position());
            if (squared < distance)
            {
                distance = squared;
                nearest = goal;
            }
        }
        return nearest;
    }
}
