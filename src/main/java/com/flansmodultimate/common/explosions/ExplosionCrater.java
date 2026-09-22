package com.flansmodultimate.common.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The block-breaking half of a {@link FlanExplosion}, in two passes.
 * <p>
 * First, rays march out from the centre and each records how far it got before stone, dirt and
 * open air used up its energy ({@link CraterReachMap}). Then every block inside the crater radius
 * is broken if it lies within the reach interpolated for its own direction. The result is a
 * solid crater with a smooth outline, bounded by the explosion radius in soft ground and
 * shallower in hard rock. The ray spacing never shows as pillars, holes or a square.
 * <p>
 * The second pass works one chunk section at a time, so a large crater can be carved over
 * several ticks by {@link CraterCarver} without holding millions of positions in memory.
 */
public final class ExplosionCrater
{
    /** Upper bound on ray-march steps per direction, independent of radius. */
    static final float MAX_RAY_STEPS = 100F;
    /** Finest ray-march step, and the step the attenuation rates below were tuned at. */
    static final float BASE_STEP = 0.3F;
    /** Energy lost per block travelled, in air or anything else. */
    static final float FREE_SPACE_ATTENUATION = 0.25F;
    /**
     * Extra energy lost per block travelled through a material, per point of its explosion
     * resistance (plus 0.3). Charged per block rather than per ray sample, so how deep a crater
     * goes depends on the charge and the ground, never on the step a large crater marches at.
     */
    static final float MATERIAL_ATTENUATION = 0.35F / BASE_STEP;
    /** Ray-energy spread between rays, so the crater wall has some natural roughness. */
    static final float RAY_ENERGY_JITTER = 0.1F;
    /** Spread of each ray's maximum reach below the crater radius, for the same reason. */
    static final float RAY_REACH_JITTER = 0.08F;

    private final ServerLevel level;
    private final Explosion explosion;
    private final ExplosionDamageCalculator damageCalculator;
    private final Vec3 center;
    private final float radius;
    private final float rayStartBudget;
    private final CraterReachMap reachMap;
    /** First block each ray broke; a representative sample of the crater. */
    private final List<BlockPos> firstHits;

    private ExplosionCrater(ServerLevel level, Explosion explosion, ExplosionDamageCalculator damageCalculator,
                            Vec3 center, float radius, float rayStartBudget, CraterReachMap reachMap, List<BlockPos> firstHits)
    {
        this.level = level;
        this.explosion = explosion;
        this.damageCalculator = damageCalculator;
        this.center = center;
        this.radius = radius;
        this.rayStartBudget = rayStartBudget;
        this.reachMap = reachMap;
        this.firstHits = firstHits;
    }

    /**
     * Casts the rays. Cheap and bounded: at most {@code 6 * 48^2} rays of at most about
     * {@link #MAX_RAY_STEPS} samples each, however big the crater.
     */
    public static ExplosionCrater trace(ServerLevel level, Explosion explosion, ExplosionDamageCalculator damageCalculator,
                                        Vec3 center, float radius, float power)
    {
        RandomSource random = level.random;
        float rayStartBudget = power * (0.7F + random.nextFloat() * 0.6F);

        // About one ray per block of radius across each cube face keeps the rays roughly a block
        // or two apart at the crater wall for small and medium charges, so the wall follows the
        // terrain closely. Past 48 the rays spread further apart but the interpolation keeps the
        // wall smooth, so the ray cost stays capped.
        CraterReachMap reachMap = new CraterReachMap(Mth.clamp(Mth.ceil(radius), 16, 48));
        float step = Math.max(BASE_STEP, radius / MAX_RAY_STEPS);
        Set<BlockPos> firstHits = new LinkedHashSet<>();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        double[] dir = new double[3];

        for (int cell = 0; cell < reachMap.cellCount(); cell++)
        {
            reachMap.cellDirection(cell, dir);
            float budget = rayStartBudget * (1F + (random.nextFloat() * 2F - 1F) * RAY_ENERGY_JITTER);
            float maxReach = radius * (1F - random.nextFloat() * RAY_REACH_JITTER);
            float reach = maxReach;
            boolean hitSolid = false;

            for (float traveled = 0F; traveled < maxReach; traveled += step)
            {
                mpos.set(Mth.floor(center.x + dir[0] * traveled), Mth.floor(center.y + dir[1] * traveled), Mth.floor(center.z + dir[2] * traveled));
                BlockState state = level.getBlockState(mpos);
                FluidState fluid = level.getFluidState(mpos);

                budget -= FREE_SPACE_ATTENUATION * step;
                boolean solid = !(state.isAir() && fluid.isEmpty());
                if (solid)
                {
                    float resistance = damageCalculator.getBlockExplosionResistance(explosion, level, mpos, state, fluid).orElse(0F);
                    budget -= (resistance + 0.3F) * MATERIAL_ATTENUATION * step;
                }

                if (budget <= 0F)
                {
                    reach = traveled;
                    break;
                }
                if (solid && !hitSolid)
                {
                    hitSolid = true;
                    if (damageCalculator.shouldBlockExplode(explosion, level, mpos, state, budget))
                        firstHits.add(mpos.immutable());
                }
            }
            reachMap.setReach(cell, reach);
        }

        return new ExplosionCrater(level, explosion, damageCalculator, center, radius, rayStartBudget, reachMap, new ArrayList<>(firstHits));
    }

    public float radius()
    {
        return radius;
    }

    public ServerLevel level()
    {
        return level;
    }

    public List<BlockPos> firstHits()
    {
        return firstHits;
    }

    /** Every chunk section the crater sphere touches, nearest to the centre first. */
    public List<SectionPos> sectionsByDistance()
    {
        int minSx = SectionPos.blockToSectionCoord(Mth.floor(center.x - radius));
        int maxSx = SectionPos.blockToSectionCoord(Mth.floor(center.x + radius));
        int minSz = SectionPos.blockToSectionCoord(Mth.floor(center.z - radius));
        int maxSz = SectionPos.blockToSectionCoord(Mth.floor(center.z + radius));
        int minSy = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(Mth.floor(center.y - radius)));
        int maxSy = Math.min(level.getMaxSection() - 1, SectionPos.blockToSectionCoord(Mth.floor(center.y + radius)));

        List<SectionPos> sections = new ArrayList<>();
        for (int sx = minSx; sx <= maxSx; sx++)
            for (int sz = minSz; sz <= maxSz; sz++)
                for (int sy = minSy; sy <= maxSy; sy++)
                {
                    SectionPos section = SectionPos.of(sx, sy, sz);
                    if (distanceSqrToSection(section) <= (double) radius * radius)
                        sections.add(section);
                }
        sections.sort(Comparator.comparingDouble(this::distanceSqrToSection));
        return sections;
    }

    private double distanceSqrToSection(SectionPos section)
    {
        double dx = axisGap(center.x, section.minBlockX());
        double dy = axisGap(center.y, section.minBlockY());
        double dz = axisGap(center.z, section.minBlockZ());
        return dx * dx + dy * dy + dz * dz;
    }

    private static double axisGap(double value, int min)
    {
        if (value < min)
            return min - value;
        return Math.max(0D, value - (min + 16));
    }

    /**
     * Finds every block of one section that the crater destroys, and hands each to {@code sink}.
     * Returns the number of block positions examined, as a measure of the work done.
     */
    public int collectSection(SectionPos section, Consumer<BlockPos> sink)
    {
        LevelChunk chunk = level.getChunk(section.x(), section.z());
        int index = level.getSectionIndexFromSectionY(section.y());
        if (index < 0 || index >= chunk.getSections().length)
            return 0;
        LevelChunkSection chunkSection = chunk.getSections()[index];
        if (chunkSection.hasOnlyAir())
            return 1;

        double radiusSqr = (double) radius * radius;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int baseX = section.minBlockX();
        int baseY = section.minBlockY();
        int baseZ = section.minBlockZ();

        for (int ly = 0; ly < 16; ly++)
        {
            double dy = baseY + ly + 0.5D - center.y;
            for (int lz = 0; lz < 16; lz++)
            {
                double dz = baseZ + lz + 0.5D - center.z;
                for (int lx = 0; lx < 16; lx++)
                {
                    double dx = baseX + lx + 0.5D - center.x;
                    double distSqr = dx * dx + dy * dy + dz * dz;
                    if (distSqr > radiusSqr)
                        continue;

                    BlockState state = chunkSection.getBlockState(lx, ly, lz);
                    FluidState fluid = state.getFluidState();
                    if (state.isAir() && fluid.isEmpty())
                        continue;

                    float reach = reachMap.reachTowards(dx, dy, dz);
                    if (distSqr >= (double) reach * reach)
                        continue;

                    mpos.set(baseX + lx, baseY + ly, baseZ + lz);
                    // A block too tough for even a fresh ray to break survives inside the
                    // crater, the way obsidian or bedrock always did.
                    float resistance = damageCalculator.getBlockExplosionResistance(explosion, level, mpos, state, fluid).orElse(0F);
                    float toughness = (resistance + 0.3F) * MATERIAL_ATTENUATION * BASE_STEP + FREE_SPACE_ATTENUATION * BASE_STEP;
                    if (toughness >= rayStartBudget)
                        continue;
                    if (damageCalculator.shouldBlockExplode(explosion, level, mpos, state, rayStartBudget - toughness))
                        sink.accept(mpos.immutable());
                }
            }
        }
        return 4096;
    }

    /** Chunks in which an event handler refused one of the representative {@link #firstHits}. */
    public Set<Long> chunksRefusedBy(List<BlockPos> allowed)
    {
        Set<BlockPos> kept = new HashSet<>(allowed);
        Set<Long> refused = new HashSet<>();
        for (BlockPos pos : firstHits)
            if (!kept.contains(pos))
                refused.add(ChunkPos.asLong(pos));
        return refused;
    }
}
