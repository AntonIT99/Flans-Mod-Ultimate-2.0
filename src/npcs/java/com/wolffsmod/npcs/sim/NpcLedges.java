package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcVehiclePaths.Solids;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Looks along the way a vehicle model is driving for ground that drops away deeper than the vehicle drives down. A path
 * leads round such a drop or down it on purpose; a vehicle steering straight at a target, or cutting a corner of its
 * path, would otherwise drive over the edge and only then find its wheels in the air.
 *
 * <p>
 * The look follows both tracks as well as the middle of the hull, as a vehicle driving along an edge goes over it with
 * the track nearest it first. Without a path it also stops short of a wall or ledge that drops away again behind it,
 * which it would otherwise climb and then hang over by its front.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcLedges
{
    /** No drop within reach. */
    public static final double NONE = Double.MAX_VALUE;
    /** Blocks ahead a vehicle looks at most, however long it takes to stop. */
    static final double MAX_REACH = 8D;
    /** Blocks in from the side of the hull that its track or outer wheels run along. */
    static final double TRACK_INSET = 0.5D;

    /**
     * @param heading
     *            the yaw the vehicle travels along, in degrees
     * @param reach
     *            blocks ahead of its middle to look
     * @param halfWidth
     *            blocks from the middle of the hull to its side; the tracks are looked along as well once they run clear of
     *            the middle
     * @param walls
     *            whether a step up onto ground that drops away again close behind it counts as a drop where the step
     *            begins: without a path, which would have priced such a climb, the vehicle is better off halting short of it
     * @return blocks from the NPC's middle to the first drop deeper than it drives down, or {@link #NONE}. Server thread.
     */
    public static double ahead(EntityNPCInterface npc, float heading, double reach, double halfWidth, boolean walls)
    {
        Level level = npc.level();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        // It drives down what it drives up; a vehicle that jumps like a mob takes a block either way.
        int blocks = Math.max(1, Mth.floor(NpcMobility.stepHeight(npc).orElse(1D)));
        float radians = (float) Math.toRadians(heading);
        Solids ground = (x, y, z) ->
        {
            probe.set(x, y, z);
            // Ground the search cannot see is no drop, and neither is water, which the terrain profile answers for.
            return !level.isLoaded(probe) || !level.getFluidState(probe).isEmpty() || !level.getBlockState(probe).getCollisionShape(level, probe).isEmpty();
        };
        Vec3 direction = new Vec3(-Mth.sin(radians), 0D, Mth.cos(radians));
        double limit = Math.min(reach, MAX_REACH);
        Vec3 origin = npc.position();
        double nearest = distance(ground, origin, direction, limit, blocks, blocks, walls);
        double track = halfWidth - TRACK_INSET;
        if (track >= TRACK_INSET)
        {
            Vec3 side = new Vec3(direction.z * track, 0D, -direction.x * track);
            nearest = Math.min(nearest, distance(ground, origin.add(side), direction, limit, blocks, blocks, walls));
            nearest = Math.min(nearest, distance(ground, origin.subtract(side), direction, limit, blocks, blocks, walls));
        }
        return nearest;
    }

    /** As {@link #distance(Solids, Vec3, Vec3, double, int, int, boolean)}, stopping short of walls and ledges as well. */
    static double distance(Solids ground, Vec3 origin, Vec3 direction, double reach, int climb, int drop)
    {
        return distance(ground, origin, direction, reach, climb, drop, true);
    }

    /**
     * Follows the ground ahead column by column, up and down the steps the vehicle takes.
     *
     * @param origin
     *            where the vehicle stands
     * @param direction
     *            horizontal unit vector it travels along
     * @param climb
     *            whole blocks it drives up at once; anything higher is a wall, which ends the look
     * @param drop
     *            whole blocks it drives down at once
     * @param walls
     *            whether a step up onto ground that drops away deeper than that within {@value NpcVehiclePaths#RIDGE_REACH}
     *            blocks behind it is reported where the step begins, rather than where the ground behind it drops
     * @return blocks to the first column whose ground lies deeper, or {@link #NONE}
     */
    static double distance(Solids ground, Vec3 origin, Vec3 direction, double reach, int climb, int drop, boolean walls)
    {
        int feet = Mth.floor(origin.y + 0.5D);
        int lastX = Mth.floor(origin.x);
        int lastZ = Mth.floor(origin.z);
        for (double travelled = 1D; travelled <= reach; travelled += 1D)
        {
            int x = Mth.floor(origin.x + direction.x * travelled);
            int z = Mth.floor(origin.z + direction.z * travelled);
            if (x == lastX && z == lastZ)
                continue;
            lastX = x;
            lastZ = z;
            if (ground.solid(x, feet + climb, z))
                return NONE;
            int surface = feet + climb - 1;
            int lowest = feet - 1 - drop;
            while (surface >= lowest && !ground.solid(x, surface, z))
                surface--;
            if (surface < lowest)
                return travelled;
            if (walls && surface >= feet && dropsBehind(ground, origin, direction, travelled, surface + 1, climb, drop))
                return travelled;
            feet = surface + 1;
        }
        return NONE;
    }

    /**
     * @param travelled
     *            blocks along the look to the column stepped up onto
     * @param top
     *            where the feet of the vehicle stand on it
     * @return whether, within {@value NpcVehiclePaths#RIDGE_REACH} further columns, the ground lies deeper below that top
     *         than the vehicle drives down, before any wall ends the look
     */
    private static boolean dropsBehind(Solids ground, Vec3 origin, Vec3 direction, double travelled, int top, int climb, int drop)
    {
        int lastX = Mth.floor(origin.x + direction.x * travelled);
        int lastZ = Mth.floor(origin.z + direction.z * travelled);
        int columns = 0;
        for (double further = travelled + 1D; columns < NpcVehiclePaths.RIDGE_REACH; further += 1D)
        {
            int x = Mth.floor(origin.x + direction.x * further);
            int z = Mth.floor(origin.z + direction.z * further);
            if (x == lastX && z == lastZ)
                continue;
            lastX = x;
            lastZ = z;
            columns++;
            if (ground.solid(x, top + climb, z))
                return false;
            boolean carried = false;
            for (int y = top + climb - 1; y >= top - 1 - drop && !carried; y--)
                carried = ground.solid(x, y, z);
            if (!carried)
                return true;
        }
        return false;
    }
}
