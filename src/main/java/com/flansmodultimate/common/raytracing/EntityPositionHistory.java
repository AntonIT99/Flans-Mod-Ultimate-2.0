package com.flansmodultimate.common.raytracing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The last second of bounding boxes of living entities other than players, which keep full {@link PlayerSnapshot}s.
 * A player aims at entities as their client shows them, a few ticks behind the server, so lag-compensated shots
 * test these boxes instead of where the entity is now.
 * <p>
 * Server thread only. A box is recorded as the entity starts its tick, so the box recorded at game time {@code t}
 * is where the entity stood at the end of tick {@code t - 1}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityPositionHistory
{
    private static final int LENGTH = PlayerSnapshot.NUM_PLAYER_SNAPSHOTS;
    /** Weak keys, so entities that leave the level take their history with them. */
    private static final Map<Entity, History> HISTORIES = new WeakHashMap<>();

    public static void record(LivingEntity entity)
    {
        if (entity.level().isClientSide || entity instanceof Player)
            return;
        HISTORIES.computeIfAbsent(entity, e -> new History()).add(entity.level().getGameTime(), entity.getBoundingBox());
    }

    /**
     * @return the recorded boxes from game time {@code from} to {@code to} inclusive, the one nearest {@code centre} first;
     *         empty when nothing was recorded then
     */
    public static List<AABB> boxesAround(Entity entity, long centre, long from, long to)
    {
        History history = HISTORIES.get(entity);
        return history == null ? List.of() : history.around(centre, from, to);
    }

    private static final class History
    {
        private final long[] times = new long[LENGTH];
        private final AABB[] boxes = new AABB[LENGTH];
        private int next;

        private void add(long time, AABB box)
        {
            int last = (next + LENGTH - 1) % LENGTH;
            // A second tick in the same game time only moves the entity on; keep its latest box.
            if (boxes[last] != null && times[last] == time)
            {
                boxes[last] = box;
                return;
            }
            times[next] = time;
            boxes[next] = box;
            next = (next + 1) % LENGTH;
        }

        private List<AABB> around(long centre, long from, long to)
        {
            List<Integer> found = new ArrayList<>(3);
            for (int i = 0; i < LENGTH; i++)
            {
                if (boxes[i] != null && times[i] >= from && times[i] <= to)
                    found.add(i);
            }
            found.sort((a, b) -> Long.compare(Math.abs(times[a] - centre), Math.abs(times[b] - centre)));
            List<AABB> result = new ArrayList<>(found.size());
            for (int i : found)
                result.add(boxes[i]);
            return result;
        }
    }
}
