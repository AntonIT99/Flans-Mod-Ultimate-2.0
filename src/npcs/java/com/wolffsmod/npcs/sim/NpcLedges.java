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
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcLedges
{
    /** No drop within reach. */
    public static final double NONE = Double.MAX_VALUE;
    /** Blocks ahead a vehicle looks at most, however long it takes to stop. */
    static final double MAX_REACH = 8D;

    /**
     * @param heading
     *            the yaw the vehicle travels along, in degrees
     * @param reach
     *            blocks ahead of its middle to look
     * @return blocks from the NPC's middle to the first drop deeper than it drives down, or {@link #NONE}. Server thread.
     */
    public static double ahead(EntityNPCInterface npc, float heading, double reach)
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
        return distance(ground, npc.position(), new Vec3(-Mth.sin(radians), 0D, Mth.cos(radians)), Math.min(reach, MAX_REACH), blocks, blocks);
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
     * @return blocks to the first column whose ground lies deeper, or {@link #NONE}
     */
    static double distance(Solids ground, Vec3 origin, Vec3 direction, double reach, int climb, int drop)
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
            feet = surface + 1;
        }
        return NONE;
    }
}
