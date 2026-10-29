package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcTuning.Terrain;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps a watercraft NPC with a running depth below the surface: it holds that depth where the water is deep enough,
 * rides higher over shallows, and never runs out of air. One instance per NPC, server side.
 */
public final class NpcSubmarine
{
    private static final int SAMPLE_INTERVAL = 10;
    /** Blocks above the NPC searched for the surface. */
    private static final int SURFACE_SEARCH = 72;

    private int sampledAt = Integer.MIN_VALUE;
    /** Whether the NPC is in water and holding a running depth. */
    @Getter
    private boolean submerged;
    private int surface;
    private int depth;

    /** @return blocks below the surface the NPC is meant to run at; 0 for anything that floats */
    public static int runningDepth(EntityNPCInterface npc)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        return tuning.terrain() == Terrain.WATERCRAFT ? tuning.get(Value.SUBMARINE_DEPTH) : 0;
    }

    /** Runs once per tick after the NPC has moved. */
    public void tick(EntityNPCInterface npc)
    {
        int wanted = runningDepth(npc);
        if (wanted <= 0 || npc.isKilled())
        {
            submerged = false;
            sampledAt = Integer.MIN_VALUE;
            return;
        }
        if (sampledAt == Integer.MIN_VALUE || (npc.tickCount + npc.getId()) % SAMPLE_INTERVAL == 0 || npc.tickCount - sampledAt >= SAMPLE_INTERVAL * 2)
            sample(npc, wanted);
        if (!submerged)
            return;
        // A submarine is watertight, whatever the NPC's own drowning setting says.
        npc.setAirSupply(npc.getMaxAirSupply());
        npc.fallDistance = 0F;
        Vec3 motion = npc.getDeltaMovement();
        int rate = NpcTuning.of(npc).get(Value.DIVE_RATE);
        npc.setDeltaMovement(motion.x, rate > 0 ? dive(motion.y, surface - depth - npc.getY(), rate / 200D) : dive(motion.y, surface - depth - npc.getY()), motion.z);
    }

    /**
     * @param error
     *            blocks the submarine is below the height it should hold; negative above it
     * @return the vertical speed that brings it there gently and then holds it
     */
    static double dive(double vertical, double error)
    {
        if (error < -0.35D)
            return Math.max(-0.18D, error * 0.08D);
        if (error > 0.35D)
            return Math.min(0.16D, error * 0.08D);
        return vertical * 0.45D;
    }

    /**
     * @param rate
     *            blocks per tick the submarine dives and surfaces at most, as its dive rate setting says
     * @return the vertical speed that brings it there no faster than that, gently at the end, and then holds it
     */
    static double dive(double vertical, double error, double rate)
    {
        if (Math.abs(error) <= 0.35D)
            return vertical * 0.45D;
        return Mth.clamp(error * 0.08D, -rate, rate);
    }

    /**
     * A floating NPC's path is laid along the surface, and Custom NPCs only counts a node as reached within a block of
     * its height. A submarine reaches it by passing below.
     *
     * @param dx
     *            blocks from the middle of the node to the submarine, likewise dy from the node's floor and dz
     * @param width
     *            how wide the submarine is
     * @return whether the submarine is under the node, out of the reach Custom NPCs tests itself
     */
    public static boolean passesBelow(double dx, double dy, double dz, float width)
    {
        double reach = width > 0.75F ? width / 2F : 0.75F - width / 2F;
        return dy <= -1D && Math.abs(dx) < reach && Math.abs(dz) < reach;
    }

    /** @return the depth to run at in water that deep: the wanted one, less over shallows so the hull clears the bottom, at least 1 */
    static int effectiveDepth(int wanted, int waterDepth)
    {
        return Math.max(1, Math.min(wanted, Math.max(1, waterDepth - 1)));
    }

    private void sample(EntityNPCInterface npc, int wanted)
    {
        sampledAt = npc.tickCount;
        Level level = npc.level();
        BlockPos.MutableBlockPos position = npc.blockPosition().mutable();
        if (!level.getFluidState(position).is(FluidTags.WATER))
        {
            submerged = false;
            return;
        }
        int start = position.getY();
        int top = start;
        while (top - start < SURFACE_SEARCH && level.getFluidState(position.setY(top + 1)).is(FluidTags.WATER))
            top++;
        int bottom = top;
        while (top - bottom < wanted + 4 && level.getFluidState(position.setY(bottom - 1)).is(FluidTags.WATER))
            bottom--;
        submerged = true;
        surface = top + 1;
        depth = effectiveDepth(wanted, top - bottom + 1);
    }
}
