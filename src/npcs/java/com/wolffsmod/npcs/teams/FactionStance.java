package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * How a Custom NPCs faction treats a player, from the faction the player belongs to and the player's own faction points
 * with it. A faction that hates a member by points is hostile to them whatever their faction; otherwise its own members
 * are friends, members of a faction at war with it are enemies, members of an allied faction are friends, and members
 * of a faction merely at peace with it are left to their faction points. Players of no faction are left neutral or to
 * their faction points, as the faction says. Pure arithmetic, any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FactionStance
{
    /** How factions stand toward each other, by Custom NPCs faction id. */
    interface Relations
    {
        /** Whether the first faction attacks the second. */
        boolean attacks(int faction, int other);

        /** Whether two factions are allied and neither attacks the other. */
        boolean allied(int faction, int other);
    }

    /**
     * @param faction
     *            the faction the player belongs to, or null for none
     * @param grudge
     *            whether the player's faction points with the faction asked about are below its neutral points
     * @param creative
     *            whether the player is in creative mode, which no faction attacks
     */
    record Standing(@Nullable Integer faction, boolean grudge, boolean creative)
    {}

    /**
     * @param atRest
     *            whether the faction is at rest, as a managed faction is while a Teams round is played; everyone is then
     *            neutral
     * @param faction
     *            the faction asked about
     * @param player
     *            where the player stands
     * @param relations
     *            how factions stand toward each other
     * @param outsiders
     *            how the faction treats players of no faction
     * @return the stance, or empty to leave it to Custom NPCs' faction points
     */
    static Optional<BotFactions.Stance> resolve(boolean atRest, int faction, Standing player, Relations relations, BattleFactions.Outsiders outsiders)
    {
        if (atRest)
            return Optional.of(BotFactions.Stance.NEUTRAL);
        Integer own = player.faction();
        if (own == null)
            return outsiders == BattleFactions.Outsiders.NEUTRAL ? Optional.of(BotFactions.Stance.NEUTRAL) : Optional.empty();
        if (player.grudge())
            return Optional.of(hostile(player));
        if (own == faction)
            return Optional.of(BotFactions.Stance.FRIENDLY);
        if (relations.attacks(faction, own) || relations.attacks(own, faction))
            return Optional.of(hostile(player));
        if (relations.allied(faction, own))
            return Optional.of(BotFactions.Stance.FRIENDLY);
        return Optional.empty();
    }

    private static BotFactions.Stance hostile(Standing player)
    {
        return player.creative() ? BotFactions.Stance.NEUTRAL : BotFactions.Stance.HOSTILE;
    }
}
