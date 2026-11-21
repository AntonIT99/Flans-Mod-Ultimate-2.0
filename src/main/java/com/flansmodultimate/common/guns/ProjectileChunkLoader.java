package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.config.ModCommonConfig;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the chunks ahead of a {@code ProjectileChunkLoading} round loaded while it flies, so a long-range missile or shell
 * is not lost when it leaves the area around players.
 *
 * <p>
 * Each server tick the round places short-lived region tickets along the path it will cover in the next
 * {@link #LOOKAHEAD_TICKS} ticks. The tickets expire on their own {@link #TICKET_LIFESPAN} ticks after the round last
 * renewed them, so nothing is saved with the world and nothing outlives the round. A round is admitted once, at its first
 * tick, within the per-player and per-server limits; a round over either limit flies as ordinary ammunition. It stops
 * loading chunks once it is further from its launch point than the configured distance.
 */
public final class ProjectileChunkLoader
{
    static final int LOOKAHEAD_TICKS = 10;
    static final int TICKET_LIFESPAN = 20;
    /** Region ticket distance 2 makes the chunk itself entity-ticking, with its neighbours loaded around it. */
    static final int TICKET_DISTANCE = 2;
    static final int MAX_SAMPLES = 32;
    private static final double SAMPLE_SPACING = 16D;
    private static final TicketType<Integer> TICKET = TicketType.create("flansmodultimate_projectile", Integer::compareTo, TICKET_LIFESPAN);
    private static final Map<Bullet, Admission> ADMISSIONS = new IdentityHashMap<>();

    private ProjectileChunkLoader()
    {}

    /**
     * Server side, once per tick of a round whose ammunition asks for chunk loading.
     *
     * @param velocity
     *            the round's velocity in blocks per tick
     * @param owner
     *            the player the round counts against, if any
     */
    public static void tick(ServerLevel level, Bullet bullet, Vec3 velocity, @Nullable UUID owner)
    {
        ADMISSIONS.keySet().removeIf(Bullet::isRemoved);
        if (!ModCommonConfig.projectileChunkLoading())
        {
            ADMISSIONS.remove(bullet);
            return;
        }
        Admission admission = ADMISSIONS.computeIfAbsent(bullet, b -> admit(b.position(), owner));
        if (!admission.loading())
            return;
        double maxDistance = ModCommonConfig.projectileChunkLoadingDistance();
        if (bullet.position().distanceToSqr(admission.launch()) > maxDistance * maxDistance)
        {
            ADMISSIONS.put(bullet, admission.stopped());
            return;
        }
        for (long chunk : chunksAhead(bullet.position(), velocity, LOOKAHEAD_TICKS, MAX_SAMPLES))
            level.getChunkSource().addRegionTicket(TICKET, new ChunkPos(chunk), TICKET_DISTANCE, bullet.getId());
    }

    private static Admission admit(Vec3 launch, @Nullable UUID owner)
    {
        int total = 0;
        int byOwner = 0;
        for (Admission admission : ADMISSIONS.values())
        {
            if (!admission.loading())
                continue;
            total++;
            if (owner != null && owner.equals(admission.owner()))
                byOwner++;
        }
        return new Admission(launch, owner, admits(owner != null, byOwner, total, ModCommonConfig.projectileChunkTicketsPerPlayer(), ModCommonConfig.projectileChunkTicketsPerServer()));
    }

    /** Whether one more round fits under the limits, given the rounds already loading chunks. */
    static boolean admits(boolean hasOwner, int ownerLoading, int totalLoading, int perOwnerLimit, int serverLimit)
    {
        return totalLoading < serverLimit && (!hasOwner || ownerLoading < perOwnerLimit);
    }

    /**
     * The distinct chunks, as {@link ChunkPos#toLong()}, along the path from {@code position} over {@code ticks} ticks at
     * {@code velocity} blocks per tick, sampled at most a chunk width apart and capped at {@code maxSamples} points.
     */
    static LongList chunksAhead(Vec3 position, Vec3 velocity, int ticks, int maxSamples)
    {
        Vec3 path = velocity.scale(ticks);
        int steps = Math.min(maxSamples - 1, (int) Math.ceil(path.length() / SAMPLE_SPACING));
        LongList chunks = new LongArrayList(steps + 1);
        for (int i = 0; i <= steps; i++)
        {
            Vec3 sample = steps == 0 ? position : position.add(path.scale(i / (double) steps));
            long chunk = ChunkPos.asLong(BlockPos.containing(sample));
            if (!chunks.contains(chunk))
                chunks.add(chunk);
        }
        return chunks;
    }

    private record Admission(Vec3 launch, @Nullable UUID owner, boolean loading)
    {
        Admission stopped()
        {
            return new Admission(launch, owner, false);
        }
    }
}
