package com.wolffsmod.npcs.sim;

/** The roam ranges a Custom NPCs spawner job keeps for itself, added to the job by its mixin. */
public interface SpawnerRanges
{
    /** @return the spawner's own range for the kind in blocks, or 0 to take the server default */
    int wolffsmodnpcsSpawnerRange(NpcSpawnerRange.Kind kind);

    /** Sets the spawner's own range for the kind in blocks; 0 takes the server default. */
    void wolffsmodnpcsSetSpawnerRange(NpcSpawnerRange.Kind kind, int blocks);
}
