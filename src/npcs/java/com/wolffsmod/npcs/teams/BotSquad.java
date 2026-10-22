package com.wolffsmod.npcs.teams;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bots of one team that move and fight together, under one task from their side's commander. Server thread. */
@Getter
@Setter
public final class BotSquad
{
    private final int id;
    private final String team;
    private final BotRole role;
    private final BotSlot.Source source;
    /** Index of the squad or vehicle entry the squad was raised for; -1 for loose infantry. */
    private final int entry;
    /** The squad template it follows, empty for loose infantry and vehicles. */
    private final String template;
    private final List<Integer> slots = new ArrayList<>();
    @Nullable
    private SquadTask task;
    /** Index of the next route point, for a squad that follows a patrol of several points. */
    private int patrolIndex;
    /** The squad template a transport still carries to the front; empty when it carries nobody or has unloaded. */
    private String cargo = "";
    /** What a player last told the squad to do, which comes before its commander's task until it runs out. */
    @Nullable
    private PlayerOrder order;
    /** A disbanded squad takes no new members and is dropped once its slots are gone. */
    private boolean disbanded;
    /** The player leading the squad, whom it follows while they are in the field, or null. */
    @Nullable
    private UUID leader;
    /** Where the squad's leader and its own fallen spawn, or null without a rally point. */
    @Nullable
    private Vec3 rally;
    /** Game time the rally point stands until. */
    private long rallyUntil;

    public BotSquad(int id, String team, BotRole role, BotSlot.Source source, int entry, String template)
    {
        this.id = id;
        this.team = team;
        this.role = role;
        this.source = source;
        this.entry = entry;
        this.template = template;
    }
}
