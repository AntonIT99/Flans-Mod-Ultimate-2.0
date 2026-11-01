package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.platform.entity.EntityPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Decides where a shot meets its targets. A player aims at targets as their client shows them: behind the server
 * by the round trip of the shooter's ping and by the ticks the client takes to glide each entity to the position
 * the server sent. Shots aimed by a player therefore test targets where they were that long ago. Shots aimed on
 * the server, by NPCs or automated weapons, test targets where they are.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LagCompensation
{
    /** Ticks a client takes to move a remote entity to a position the server sent, as set by vanilla's lerpTo */
    public static final int CLIENT_INTERPOLATION_TICKS = 3;
    /**
     * Ticks either side of the estimate that are also tested, for the jitter of ping, of when position packets
     * are sent and of the frame the shooter fired on. The estimate is tested first.
     */
    static final int WINDOW_TICKS = 1;

    /** @return the ticks the shot's targets are rewound by; 0 for a shot aimed on the server */
    public static int rewindTicks(FiredShot shot)
    {
        return shot.getPlayerAttacker().map(LagCompensation::rewindTicks).orElse(0);
    }

    /** @return the ticks behind the server at which the player sees other entities */
    public static int rewindTicks(ServerPlayer shooter)
    {
        return rewindTicks(EntityPlatform.latency(shooter), FlansMod.teamsManager.getBulletSnapshotMin(), FlansMod.teamsManager.getBulletSnapshotDivisor());
    }

    static int rewindTicks(int pingMillis, int snapshotMin, int snapshotDivisor)
    {
        int pingTicks = snapshotDivisor > 0 ? Math.round(Math.max(0, pingMillis) / (float) snapshotDivisor) : 0;
        return Math.max(0, snapshotMin) + pingTicks + CLIENT_INTERPOLATION_TICKS;
    }

    /**
     * @return the snapshots of the player to test, the best estimate first; empty when the player has none yet
     */
    public static List<PlayerSnapshot> playerSnapshots(PlayerData data, long gameTime, int rewindTicks)
    {
        PlayerSnapshot[] snapshots = data.getSnapshots();
        if (rewindTicks <= 0)
            return snapshots[0] == null ? List.of() : List.of(snapshots[0]);

        // Snapshots are taken after the player's tick, while a shot is traced during the level's tick:
        // the snapshot taken at game time t shows the player at the end of tick t.
        long centre = gameTime - rewindTicks - 1;
        List<PlayerSnapshot> found = new ArrayList<>(2 * WINDOW_TICKS + 1);
        PlayerSnapshot nearest = null;
        for (PlayerSnapshot snapshot : snapshots)
        {
            if (snapshot == null)
                continue;
            if (Math.abs(snapshot.time - centre) <= WINDOW_TICKS)
                found.add(snapshot);
            if (nearest == null || Math.abs(snapshot.time - centre) < Math.abs(nearest.time - centre))
                nearest = snapshot;
        }
        // A ping longer than the history, or a player who has just joined, falls back to the closest snapshot.
        if (found.isEmpty())
            return nearest == null ? List.of() : List.of(nearest);
        found.sort(Comparator.comparingLong(snapshot -> Math.abs(snapshot.time - centre)));
        return found;
    }

    /**
     * @return the past boxes of the entity to test, the best estimate first; empty when the shot is aimed on the
     *         server or nothing was recorded for that time
     */
    public static List<AABB> entityBoxes(Entity entity, long gameTime, int rewindTicks)
    {
        if (rewindTicks <= 0)
            return List.of();
        // The box recorded at game time t shows the entity at the end of tick t - 1.
        long centre = gameTime - rewindTicks;
        return EntityPositionHistory.boxesAround(entity, centre, centre - WINDOW_TICKS, centre + WINDOW_TICKS);
    }
}
