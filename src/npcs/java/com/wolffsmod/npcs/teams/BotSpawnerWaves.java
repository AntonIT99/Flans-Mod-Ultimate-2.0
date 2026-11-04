package com.wolffsmod.npcs.teams;

import java.util.*;

/** Pending members of an all-at-once deployment wave, spanning the per-tick spawn limit. Server thread only. */
final class BotSpawnerWaves
{
    private final Map<UUID, Set<Integer>> pending = new HashMap<>();

    boolean contains(UUID spawner, int slot)
    {
        return pending.getOrDefault(spawner, Set.of()).contains(slot);
    }

    void started(UUID spawner, Collection<Integer> queued)
    {
        pending.computeIfAbsent(spawner, ignored -> new HashSet<>(queued));
    }

    void attempted(int slot)
    {
        pending.values().forEach(slots -> slots.remove(slot));
        pending.values().removeIf(Set::isEmpty);
    }

    void retain(Collection<Integer> queued)
    {
        pending.values().forEach(slots -> slots.retainAll(queued));
        pending.values().removeIf(Set::isEmpty);
    }
}
