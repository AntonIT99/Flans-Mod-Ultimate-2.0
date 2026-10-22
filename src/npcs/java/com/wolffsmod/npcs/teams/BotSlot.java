package com.wolffsmod.npcs.teams;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * One place in a team's force for the round: a soldier or a vehicle crew with a name, a role and a score, who comes back
 * after dying as long as the round's rules allow, the way a player respawns. Server thread.
 */
@Getter
@Setter
public final class BotSlot
{
    /** Where a slot's place in the force comes from. */
    public enum Source
    {
        /** Loose infantry, counted by the team's infantry setting. */
        INFANTRY,
        /** A member of a squad of one of the team's squad entries. */
        SQUAD,
        /** A soldier a transport of one of the team's vehicle entries brought to the front; it never comes back. */
        CARGO,
        /** A vehicle of one of the team's vehicle entries. */
        VEHICLE,
        /** An NPC that joined the battle as it was, enlisted or moved over from another; it never comes back. */
        VOLUNTEER
    }

    private final int id;
    private final String team;
    private final Source source;
    /** Index of the squad or vehicle entry in the team's settings; -1 for loose infantry. */
    private final int entry;
    /** Whether the slot was made when the round started, whose first spawn costs no ticket. */
    private final boolean initial;
    private int squad;
    private String name = "";
    /** Short name of the slot's class, or of its vehicle definition. */
    private String role = "";
    @Nullable
    private UUID entity;
    /** Game time from which a fallen slot may come back. */
    private long respawnAt;
    /** Game time the slot's entity was last seen in a loaded chunk. */
    private long lastSeen;
    @Nullable
    private Vec3 lastPosition;
    /** Game time its entity died, 0 while it lives or has not spawned yet. */
    private long diedAt;
    private boolean queued;
    /** A retired slot never comes back; it is dropped once its entity is gone. */
    private boolean retired;
    private int spawns;
    private int kills;
    private int deaths;
    /** Whether the slot's vehicle left a wreck players can use, put in place once its body is gone. */
    private boolean wreckPending;

    public BotSlot(int id, String team, Source source, int entry, boolean initial)
    {
        this.id = id;
        this.team = team;
        this.source = source;
        this.entry = entry;
        this.initial = initial;
    }

    public boolean isVehicle()
    {
        return source == Source.VEHICLE;
    }

    /** The score shown on the scoreboard: a point a kill, as for players. */
    public int getScore()
    {
        return kills;
    }
}
