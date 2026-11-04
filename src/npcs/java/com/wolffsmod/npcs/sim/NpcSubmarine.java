package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcTuning.Terrain;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps a watercraft NPC with a running depth below the surface: it holds that depth where the water is deep enough,
 * rides higher over shallows, and never runs out of air. The depth it holds is its own to choose, as {@link Trim} lays
 * it: it runs deep on its way somewhere, comes up to periscope depth to fight, where it can see and shoot at what is on
 * the surface, and goes deep again for a moment whenever something hits it. One instance per NPC, server side.
 */
public final class NpcSubmarine
{
    private static final int SAMPLE_INTERVAL = 10;
    /** Blocks above the NPC searched for the surface. */
    private static final int SURFACE_SEARCH = 72;
    /** Blocks below the surface a submarine fights from, shallow enough for its conning tower to see over the water. */
    static final int PERISCOPE_DEPTH = 3;
    /** Blocks below its running depth a submarine goes to break off, and the ticks it stays down for after being hit. */
    static final int EVADE_DEPTH = 8;
    static final int EVADE_TICKS = 120;

    private int sampledAt = Integer.MIN_VALUE;
    /** Whether the NPC is in water and holding a running depth. */
    @Getter
    private boolean submerged;
    private int surface;
    private int depth;
    /** The tick something last hit the NPC, which sends it deep for a moment. */
    private int hurtAt = Integer.MIN_VALUE;
    /** The depth last asked for, so that a change of trim is acted on at once rather than at the next sample. */
    private int trimmedTo = -1;

    /** @return blocks below the surface the NPC is meant to run at; 0 for anything that floats */
    public static int runningDepth(EntityNPCInterface npc)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        return tuning.terrain() == Terrain.WATERCRAFT ? tuning.get(Value.SUBMARINE_DEPTH) : 0;
    }

    /** @return blocks below the surface the submarine is holding now, or 0 while it is not running submerged */
    public int heldDepth()
    {
        return submerged ? depth : 0;
    }

    /** What a submarine is doing with its depth. */
    public enum Trim
    {
        /** Runs at the depth it was given, on its way somewhere. */
        TRANSIT,
        /** Lies at periscope depth to fight, shallow enough to see and shoot at what is on the surface. */
        ATTACK,
        /** Goes deep to break off, having just been hit. */
        EVADE
    }

    /**
     * @return what the submarine is doing with its depth now: deep for a moment after being hit, at periscope depth while
     *         it fights, and at its running depth otherwise
     */
    public Trim trim(EntityNPCInterface npc)
    {
        if (!NpcEquipment.enabled(npc, Feature.SUBMARINE_TRIM))
            return Trim.TRANSIT;
        if (hurtAt != Integer.MIN_VALUE && npc.tickCount - hurtAt < EVADE_TICKS)
            return Trim.EVADE;
        LivingEntity target = npc.getTarget();
        return target != null && target.isAlive() && target.level() == npc.level() ? Trim.ATTACK : Trim.TRANSIT;
    }

    /**
     * @param running
     *            blocks below the surface the submarine was given to run at
     * @return the depth that trim calls for
     */
    static int trimDepth(Trim trim, int running)
    {
        return switch (trim)
        {
            case TRANSIT -> running;
            case ATTACK -> Math.max(1, Math.min(running, PERISCOPE_DEPTH));
            case EVADE -> running + EVADE_DEPTH;
        };
    }

    /** Runs once per tick after the NPC has moved. */
    public void tick(EntityNPCInterface npc)
    {
        if (npc.hurtTime > 0)
            hurtAt = npc.tickCount;
        int wanted = trimDepth(trim(npc), runningDepth(npc));
        // The trim never puts a boat that floats under the water, nor takes a submarine off its depth keeping entirely.
        if (runningDepth(npc) <= 0)
            wanted = 0;
        if (wanted <= 0 || npc.isKilled())
        {
            submerged = false;
            sampledAt = Integer.MIN_VALUE;
            trimmedTo = -1;
            return;
        }
        if (sampledAt == Integer.MIN_VALUE || wanted != trimmedTo || (npc.tickCount + npc.getId()) % SAMPLE_INTERVAL == 0 || npc.tickCount - sampledAt >= SAMPLE_INTERVAL * 2)
        {
            trimmedTo = wanted;
            sample(npc, wanted);
        }
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
