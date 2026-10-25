package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Keeps NPCs told to stay active simulating while a player is within their active range, even where the server's
 * simulation distance would freeze them: far-away battles that are still drawn then carry on.
 *
 * <p>
 * A loaded entity outside the simulation distance does not tick, so it cannot keep itself going. The level does it
 * instead: once a second it renews a short-lived chunk ticket under each such NPC. The tickets expire on their own
 * when the player leaves or the switch is turned off, and are never saved. With a limit set for the whole server, only
 * that many NPCs are held at once, those nearest a player first; the others wait until a place is free.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcActivity
{
    private static final int INTERVAL = 20;
    /** Lifetime of a ticket in ticks: long enough to be renewed before it runs out. */
    private static final int LIFETIME = INTERVAL * 3;
    /** Distance that puts the NPC's own chunk at the entity-ticking level. */
    private static final int TICKET_DISTANCE = 2;
    private static final TicketType<ChunkPos> TICKET = TicketType.create("wolffsmodnpcs_active", Comparator.comparingLong(ChunkPos::toLong), LIFETIME);

    /** Per level, the NPCs that hold a ticket since the last renewal, and those the server's limit turned away. Server thread. */
    private static final Map<ResourceKey<Level>, Held> held = new HashMap<>();
    /** The server tick the limit was last counted from, and how many NPCs took a place in it across every level. */
    private static int countedTick = -1;
    private static int placesTaken;

    /**
     * The NPCs of one level that Stay active held at the last renewal.
     *
     * @param ids
     *            entity ids of the NPCs holding a ticket
     * @param refused
     *            NPCs that wanted one but found the server's limit reached
     */
    public record Held(Set<Integer> ids, int refused)
    {}

    /** At the end of every server level tick. */
    public static void levelTick(ServerLevel level)
    {
        if (level.getGameTime() % INTERVAL != 0)
            return;
        if (level.players().isEmpty())
        {
            held.remove(level.dimension());
            return;
        }
        int tick = level.getServer().getTickCount();
        if (tick != countedTick)
        {
            countedTick = tick;
            placesTaken = 0;
        }
        int limit = NpcServerSettings.get(level.getServer()).get(NpcServerSettings.Setting.STAY_ACTIVE_LIMIT);
        List<Candidate> candidates = new ArrayList<>();
        for (Entity entity : level.getAllEntities())
        {
            if (entity instanceof EntityNPCInterface npc)
            {
                double nearest = nearestPlayer(level, npc);
                if (nearest >= 0D)
                    candidates.add(new Candidate(npc, nearest));
            }
        }
        if (limit > 0)
            candidates.sort(Comparator.comparingDouble(Candidate::distanceSquared));
        Set<Integer> ids = new HashSet<>();
        int refused = 0;
        for (Candidate candidate : candidates)
        {
            if (limit > 0 && placesTaken >= limit)
            {
                refused++;
                continue;
            }
            placesTaken++;
            ids.add(candidate.npc().getId());
            level.getChunkSource().addRegionTicket(TICKET, candidate.npc().chunkPosition(), TICKET_DISTANCE, candidate.npc().chunkPosition());
        }
        held.put(level.dimension(), new Held(Set.copyOf(ids), refused));
    }

    /** @return the NPCs of the level that Stay active held at the last renewal; none while no player is in it */
    public static Held held(ServerLevel level)
    {
        return held.getOrDefault(level.dimension(), new Held(Set.of(), 0));
    }

    /** Forgets every level's NPCs, as a stopping server does. */
    public static void clear()
    {
        held.clear();
        countedTick = -1;
    }

    private record Candidate(EntityNPCInterface npc, double distanceSquared)
    {}

    /** @return the squared distance to the nearest player within the NPC's active range, or -1 when it does not stay active */
    private static double nearestPlayer(ServerLevel level, EntityNPCInterface npc)
    {
        if (npc.isRemoved() || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.STAY_ACTIVE))
            return -1D;
        double range = NpcTuning.of(npc).get(Value.ACTIVE_RANGE);
        double nearest = -1D;
        for (Player player : level.players())
        {
            double distance = player.distanceToSqr(npc);
            if (!player.isSpectator() && distance <= range * range && (nearest < 0D || distance < nearest))
                nearest = distance;
        }
        return nearest;
    }
}
