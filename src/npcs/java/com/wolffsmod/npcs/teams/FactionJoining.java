package com.wolffsmod.npcs.teams;

import noppes.npcs.controllers.data.Faction;

import java.util.Optional;

/**
 * Who may join a Custom NPCs faction by themselves, and what leaving it costs, saved with the faction. Operators, and
 * dialogs running commands, make anyone a member with {@code /teams faction member} whatever these say.
 */
public interface FactionJoining
{
    /** Game ticks in a minute. */
    long TICKS_PER_MINUTE = 1200;

    /** Who may join a faction by themselves. */
    enum Rule
    {
        /** Anyone it is not hostile to by their faction points. */
        OPEN,
        /** Only players it is friendly to by their faction points. */
        POINTS,
        /** Nobody: members are made with {@code /teams faction member}, as by a recruiter's dialog. */
        INVITE;

        /**
         * The rule of a saved name, or {@link #INVITE} for none or an unknown one, so that factions of existing worlds and
         * maps take nobody until an operator opens them.
         */
        public static Rule named(String name)
        {
            return BotSettings.parse(Rule.class, name, INVITE);
        }
    }

    /** Why a player may not join a faction by themselves. */
    enum Refusal
    {
        INVITE, HATED, NOT_TRUSTED, COOLDOWN
    }

    Rule wolffsmodnpcsJoinRule();

    void wolffsmodnpcsSetJoinRule(Rule rule);

    /** The faction points a player loses with the faction on leaving it by themselves. */
    int wolffsmodnpcsLeavePenalty();

    void wolffsmodnpcsSetLeavePenalty(int points);

    /** The minutes before a player who left the faction may join it again by themselves. */
    int wolffsmodnpcsRejoinCooldown();

    void wolffsmodnpcsSetRejoinCooldown(int minutes);

    static FactionJoining of(Faction faction)
    {
        return (FactionJoining) faction;
    }

    /**
     * Why a player may not join a faction by themselves, or empty when they may. Hidden factions take nobody by
     * themselves, and no faction takes a player it is hostile to. Pure arithmetic, any thread.
     *
     * @param points
     *            the player's faction points with the faction
     * @param neutralPoints
     *            the points below which the faction is hostile to a player
     * @param friendlyPoints
     *            the points from which the faction is friendly to a player
     * @param ticksSinceLeft
     *            the game ticks since the player last left the faction, or {@link Long#MAX_VALUE} if they never did
     */
    static Optional<Refusal> refusal(Rule rule, boolean hidden, int points, int neutralPoints, int friendlyPoints, int cooldownMinutes, long ticksSinceLeft)
    {
        if (rule == Rule.INVITE || hidden)
            return Optional.of(Refusal.INVITE);
        if (points < neutralPoints)
            return Optional.of(Refusal.HATED);
        if (rule == Rule.POINTS && points < friendlyPoints)
            return Optional.of(Refusal.NOT_TRUSTED);
        if (ticksSinceLeft < cooldownMinutes * TICKS_PER_MINUTE)
            return Optional.of(Refusal.COOLDOWN);
        return Optional.empty();
    }
}
