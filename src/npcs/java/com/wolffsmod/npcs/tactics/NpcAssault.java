package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.TacticKind;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The route of an NPC on the Assault position tactic: the points of its Custom NPCs moving path, taken one after another,
 * the last of which it holds. The NPC never goes back along it, whatever it fights on the way. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcAssault
{
    /**
     * Blocks from a point of the route within which the NPC goes on to the next. It is more than a holding NPC's radius, so
     * that the NPC moves on before it would stand at a point on the way.
     */
    static final double REACHED = TacticDecision.HOLD_RADIUS + 1D;

    /**
     * The point of the route the NPC heads for.
     *
     * @param last
     *            whether it is the end of the route, which the NPC holds
     */
    public record Point(Vec3 spot, boolean last)
    {}

    /**
     * @return whether the NPC is set to Assault position and the tactic is written for its kind. Between fights it walks
     *         its route whatever it carries, so a soldier with only a bayonet goes too.
     */
    public static boolean assigned(EntityNPCInterface npc)
    {
        return NpcTuning.of(npc).tactic() == Tactic.ASSAULT && Tactic.ASSAULT.appliesTo(TacticKind.of(npc));
    }

    /** @return the point the NPC heads for now, moving on past those it has reached; empty without a route of two points or more */
    public static Optional<Point> next(EntityNPCInterface npc)
    {
        List<int[]> path = npc.ais.getMovingPath();
        if (path.size() < 2)
            return Optional.empty();
        List<Vec3> route = new ArrayList<>(path.size());
        int signature = 1;
        for (int[] point : path)
        {
            route.add(new Vec3(point[0] + 0.5D, point[1], point[2] + 0.5D));
            signature = 31 * signature + Arrays.hashCode(point);
        }
        NpcTacticState memory = NpcTacticState.of(npc);
        int index = advance(npc.position(), route, memory.assaultIndex(signature));
        memory.assault(signature, index);
        return Optional.of(new Point(route.get(index), index == route.size() - 1));
    }

    /**
     * @param index
     *            the point it headed for, or -1 to start from the point nearest it, as a reloaded NPC does
     * @return the point it heads for: the given one, or the first after it that it is not yet within reach of
     */
    static int advance(Vec3 position, List<Vec3> route, int index)
    {
        int next = index < 0 || index >= route.size() ? nearest(position, route) : index;
        while (next < route.size() - 1 && TacticDecision.horizontal(position, route.get(next)) <= REACHED)
            next++;
        return next;
    }

    private static int nearest(Vec3 position, List<Vec3> route)
    {
        int nearest = 0;
        for (int i = 1; i < route.size(); i++)
        {
            if (TacticDecision.horizontal(position, route.get(i)) < TacticDecision.horizontal(position, route.get(nearest)))
                nearest = i;
        }
        return nearest;
    }
}
