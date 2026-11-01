package com.wolffsmod.npcs.sim;

import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * One list of the living entities of a level, taken at most once a tick and shared by everything that looks around.
 *
 * <p>
 * Without it every NPC asks the level for the entities near itself, several times over: once per due band of its target
 * search, once for its threat alerts, once a second for its duties. Those searches cover largely the same entities, and
 * each walks the level's entity sections and builds a list of its own, so a battle of a few hundred soldiers pays for
 * hundreds of overlapping queries every tick. Here the level is walked once, into arrays that are reused between ticks,
 * and each search becomes a scan of that list with its own test: a scan of a few hundred entries costs less than one
 * query that allocates.
 * </p>
 *
 * <p>
 * The list is deliberately unfiltered beyond dropping removed entities, and carries nothing that could go stale within a
 * tick but the bounding boxes, so no search changes meaning by reading it: each one still applies its own conditions and
 * measures its own distances against the live entity. The NPCs are listed separately, since most searches want nothing
 * else. Server thread only.
 * </p>
 */
public final class NpcCombatants
{
    private static final Map<ResourceKey<Level>, NpcCombatants> levels = new HashMap<>();

    /**
     * One living entity as the list holds it.
     *
     * @param npc
     *            the entity itself when it is an NPC, so searches need not test that again
     * @param bounds
     *            its bounding box when the list was taken, for searches that select by box rather than by distance
     * @param faction
     *            the faction of an NPC, or -1 for anything else
     */
    public record Combatant(LivingEntity entity, @Nullable EntityNPCInterface npc, AABB bounds, int faction)
    {}

    private long taken = Long.MIN_VALUE;
    private Combatant[] combatants = new Combatant[0];
    private int count;
    private Combatant[] npcs = new Combatant[0];
    private int npcCount;

    private NpcCombatants()
    {}

    /** @return the level's list, taken now unless it was already taken this tick */
    public static NpcCombatants of(ServerLevel level)
    {
        NpcCombatants list = levels.computeIfAbsent(level.dimension(), ignored -> new NpcCombatants());
        if (list.taken != level.getGameTime())
            list.take(level);
        return list;
    }

    /** Forgets every level's list, so no entity is held past the world it lives in. */
    public static void clear()
    {
        levels.clear();
    }

    /** @return how many living entities the level held */
    public int size()
    {
        return count;
    }

    public Combatant get(int index)
    {
        return combatants[index];
    }

    /** @return how many of them are NPCs */
    public int npcSize()
    {
        return npcCount;
    }

    public Combatant npc(int index)
    {
        return npcs[index];
    }

    private void take(ServerLevel level)
    {
        taken = level.getGameTime();
        count = 0;
        npcCount = 0;
        for (Entity entity : level.getAllEntities())
        {
            if (!(entity instanceof LivingEntity living) || living.isRemoved())
                continue;
            EntityNPCInterface npc = living instanceof EntityNPCInterface fellow ? fellow : null;
            Combatant combatant = new Combatant(living, npc, living.getBoundingBox(), npc != null && npc.faction != null ? npc.faction.id : -1);
            combatants = grown(combatants, count);
            combatants[count++] = combatant;
            if (npc != null)
            {
                npcs = grown(npcs, npcCount);
                npcs[npcCount++] = combatant;
            }
        }
        // Entries left over from a busier tick must not keep their entities from being collected.
        release(combatants, count);
        release(npcs, npcCount);
    }

    private static Combatant[] grown(Combatant[] array, int index)
    {
        if (index < array.length)
            return array;
        Combatant[] grown = new Combatant[Math.max(16, array.length * 2)];
        System.arraycopy(array, 0, grown, 0, array.length);
        return grown;
    }

    private static void release(Combatant[] array, int from)
    {
        for (int index = from; index < array.length && array[index] != null; index++)
            array[index] = null;
    }
}
