package com.wolffsmod.npcs.crew;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Implemented on Custom NPCs' entity: the seat it was given as a crew member, the seats its riders moved to, whether it
 * has been crewed as a vehicle, and its seat guns.
 */
public interface NpcCrewHolder
{
    /** @return the seat the NPC took as a crew member, synchronized to clients; -1 for an NPC that is no crew member */
    int wolffsmodnpcsCrewSeat();

    void wolffsmodnpcsCrewSeat(int seat);

    /**
     * @return the seats riders without a crew seat, such as players, moved to while the NPC carries them as a vehicle, by
     *         rider id, synchronized to clients; never change the tag in place
     */
    CompoundTag wolffsmodnpcsRiderSeats();

    void wolffsmodnpcsRiderSeats(CompoundTag seats);

    /**
     * @return the id the NPC had when it was last crewed as a vehicle, or null; a clone spawned from it has another id and
     *         is crewed again
     */
    @Nullable
    UUID wolffsmodnpcsCrewedAs();

    void wolffsmodnpcsCrewedAs(@Nullable UUID id);

    /** @return the seat guns the NPC's crew and riders man while it is a vehicle */
    CrewGunners wolffsmodnpcsGunners();
}
