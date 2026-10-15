package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;

/**
 * Keeps NPCs told to stay active simulating while a player is within their active range, even where the server's
 * simulation distance would freeze them: far-away battles that are still drawn then carry on.
 *
 * <p>
 * A loaded entity outside the simulation distance does not tick, so it cannot keep itself going. The level does it
 * instead: once a second it renews a short-lived chunk ticket under each such NPC. The tickets expire on their own
 * when the player leaves or the switch is turned off, and are never saved.
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

    /** At the end of every server level tick. */
    public static void levelTick(ServerLevel level)
    {
        if (level.getGameTime() % INTERVAL != 0 || level.players().isEmpty())
            return;
        for (Entity entity : level.getAllEntities())
        {
            if (entity instanceof EntityNPCInterface npc && staysActive(level, npc))
                level.getChunkSource().addRegionTicket(TICKET, npc.chunkPosition(), TICKET_DISTANCE, npc.chunkPosition());
        }
    }

    private static boolean staysActive(ServerLevel level, EntityNPCInterface npc)
    {
        if (npc.isRemoved() || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.STAY_ACTIVE))
            return false;
        double range = NpcTuning.of(npc).get(Value.ACTIVE_RANGE);
        for (Player player : level.players())
        {
            if (!player.isSpectator() && player.distanceToSqr(npc) <= range * range)
                return true;
        }
        return false;
    }
}
