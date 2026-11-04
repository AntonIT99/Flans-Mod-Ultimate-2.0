package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Whether a ship can steam somewhere. A naval course is laid by arithmetic that knows nothing of the coast, so the water
 * along it has to be deep enough for the whole hull the whole way before the ship is sent down it; otherwise it runs
 * aground, and backing a hull off a beach takes far longer than steaming round it would have. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShipWaters
{
    /** Blocks between the samples taken along a course: close enough that a hull cannot pass a headland between two of them. */
    private static final double SAMPLE_STEP = 4D;

    /** @return whether the ship can steam from where it is to the point without grounding on the way */
    public static boolean clear(EntityNPCInterface npc, Entity hull, Vec3 to)
    {
        return clear(npc.level(), hull.position(), to, NpcMobility.footprint(hull.getBbWidth()), NpcTuning.of(npc).get(Value.MIN_WATER_DEPTH));
    }

    /**
     * @param footprint
     *            blocks the hull covers to a side
     * @param minimumDepth
     *            blocks of water the hull needs under it
     * @return whether a hull of that footprint can steam from one point to the other without grounding
     */
    public static boolean clear(Level level, Vec3 from, Vec3 to, int footprint, int minimumDepth)
    {
        Vec3 leg = to.subtract(from);
        int steps = Math.max(1, (int) Math.ceil(leg.horizontalDistance() / SAMPLE_STEP));
        for (int step = 1; step <= steps; step++)
            if (!navigable(level, from.add(leg.scale((double) step / steps)), footprint, minimumDepth))
                return false;
        return true;
    }

    /** @return whether the water at the point is deep enough to carry the hull, as the ship's own settings measure it */
    public static boolean navigable(EntityNPCInterface npc, Entity hull, Vec3 point)
    {
        return navigable(npc.level(), point, NpcMobility.footprint(hull.getBbWidth()), NpcTuning.of(npc).get(Value.MIN_WATER_DEPTH));
    }

    /** @return whether the water under a hull of that footprint lying at the point is deep enough to carry it */
    public static boolean navigable(Level level, Vec3 point, int footprint, int minimumDepth)
    {
        BlockPos at = BlockPos.containing(point);
        // The depth test lays the footprint east and south of its corner, so it is moved back onto the point it is taken for.
        return NpcMobility.hasWaterDepth(level, at.getX() - footprint / 2, at.getY(), at.getZ() - footprint / 2, footprint, minimumDepth);
    }
}
