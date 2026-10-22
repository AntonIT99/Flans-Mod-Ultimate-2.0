package com.wolffsmod.npcs.crew;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Implemented on Custom NPCs' entity: the seat it was given as a crew member, and whether it has been crewed as a vehicle. */
public interface NpcCrewHolder
{
    /** @return the seat the NPC took as a crew member, synchronized to clients; -1 for an NPC that is no crew member */
    int wolffsmodnpcsCrewSeat();

    void wolffsmodnpcsCrewSeat(int seat);

    /**
     * @return the id the NPC had when it was last crewed as a vehicle, or null; a clone spawned from it has another id and
     *         is crewed again
     */
    @Nullable
    UUID wolffsmodnpcsCrewedAs();

    void wolffsmodnpcsCrewedAs(@Nullable UUID id);
}
