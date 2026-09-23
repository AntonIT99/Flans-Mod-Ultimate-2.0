package com.flansmodultimate.common.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Carves a large crater over several server ticks, nearest chunk sections first, so the hole
 * visibly grows outward from the blast. A solid crater grows with the cube of its radius, and
 * breaking hundreds of thousands of blocks at once, let alone the millions of a nuclear
 * detonation, would stall the server for seconds. Craters small enough to finish in one tick
 * never come through here.
 */
public final class CraterCarver
{
    /**
     * Work allowed per server tick across every active crater. Examining a block position costs
     * 1 and breaking a block costs {@link #BREAK_COST}. At the limit a tick breaks about 16 000
     * blocks, so a 50 block crater finishes in about half a second.
     */
    static final int WORK_PER_TICK = 400_000;
    static final int BREAK_COST = 24;

    private static final List<CraterCarver> ACTIVE = new ArrayList<>();

    private final FlanExplosion explosion;
    private final ExplosionCrater crater;
    private final Deque<SectionPos> sections;
    private final Set<Long> refusedChunks;

    private CraterCarver(FlanExplosion explosion, ExplosionCrater crater, Set<Long> refusedChunks)
    {
        this.explosion = explosion;
        this.crater = crater;
        this.refusedChunks = refusedChunks;
        sections = new ArrayDeque<>(crater.sectionsByDistance());
    }

    /**
     * Queues a crater for carving. Chunks in {@code refusedChunks} are left alone: an explosion
     * event handler, typically a claim-protection mod, removed a block there from the explosion's
     * block list, and that is the only way such a handler can see a crater this big.
     */
    public static void start(FlanExplosion explosion, ExplosionCrater crater, Set<Long> refusedChunks)
    {
        ACTIVE.add(new CraterCarver(explosion, crater, refusedChunks));
    }

    public static void tick()
    {
        int work = WORK_PER_TICK;
        Iterator<CraterCarver> it = ACTIVE.iterator();
        while (it.hasNext() && work > 0)
        {
            CraterCarver carver = it.next();
            work = carver.carve(work);
            if (carver.sections.isEmpty())
                it.remove();
        }
    }

    public static void clear()
    {
        ACTIVE.clear();
    }

    /** Carves whole sections until {@code work} runs out, and returns what is left of it. */
    private int carve(int work)
    {
        List<BlockPos> broken = new ArrayList<>();
        while (work > 0 && !sections.isEmpty())
        {
            SectionPos section = sections.poll();
            if (refusedChunks.contains(ChunkPos.asLong(section.x(), section.z())))
                continue;

            broken.clear();
            work -= crater.collectSection(section, broken::add);
            for (BlockPos pos : broken)
                explosion.blowUpBlock(pos);
            for (BlockPos pos : broken)
                explosion.maybeIgnite(pos);
            work -= broken.size() * BREAK_COST;
        }
        return work;
    }
}
