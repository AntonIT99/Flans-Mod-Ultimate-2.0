package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * How a managed faction treats a player, from the faction the player belongs to: its own members are friends, members of
 * a faction it is at war with are enemies, and members of a faction it is at peace with are friends too. Players of no
 * faction are left neutral or to Custom NPCs' faction points, as the faction says. Pure arithmetic, any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FactionStance
{
    /**
     * @param atRest
     *            whether managed factions are at rest, as while a Teams round is played; everyone is then neutral
     * @param faction
     *            the managed faction
     * @param playerFaction
     *            the managed faction the player belongs to, or null for none
     * @param attacks
     *            whether the first faction attacks the second
     * @param outsiders
     *            how the faction treats players of no faction
     * @param creative
     *            whether the player is in creative mode, which no faction attacks
     * @return the stance, or empty to leave it to Custom NPCs' faction points
     */
    static Optional<BotFactions.Stance> resolve(boolean atRest, int faction, @Nullable Integer playerFaction, BiPredicate<Integer, Integer> attacks, BattleFactions.Outsiders outsiders,
        boolean creative)
    {
        if (atRest)
            return Optional.of(BotFactions.Stance.NEUTRAL);
        if (playerFaction == null)
            return outsiders == BattleFactions.Outsiders.NEUTRAL ? Optional.of(BotFactions.Stance.NEUTRAL) : Optional.empty();
        if (playerFaction == faction)
            return Optional.of(BotFactions.Stance.FRIENDLY);
        if (attacks.test(faction, playerFaction) || attacks.test(playerFaction, faction))
            return Optional.of(creative ? BotFactions.Stance.NEUTRAL : BotFactions.Stance.HOSTILE);
        return Optional.of(BotFactions.Stance.FRIENDLY);
    }
}
