/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.teams.TeamsCombatants;
import com.flansmodultimate.common.teams.TeamsListeners;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

/**
 * Entry point to the Teams game modes for addons: the rotation, the round being played, the events of a round, and
 * combatants, the non-player entities that fight for a team.
 *
 * <p>
 * A combatant counts for its team as a player does for as long as the round it was assigned in is played: it is
 * spared by its allies unless the game type allows friendly fire, it stands on capture points with its capture weight,
 * its kills score for its team and its deaths are scored like a player's. The assignment is saved with the entity and
 * lapses by itself when that round ends.
 * </p>
 *
 * <p>
 * Every method is for the server thread. Elsewhere, or without a running server, queries return nothing and changes
 * are refused.
 * </p>
 */
@ApiStatus.Experimental
public final class FlansTeams
{
    private FlansTeams()
    {}

    /**
     * @return the round being played, or empty between rounds and while Teams is disabled
     */
    public static Optional<ITeamsLiveRound> getRunningRound()
    {
        return manager().flatMap(manager -> manager.getLiveRound().map(ITeamsLiveRound.class::cast));
    }

    /**
     * @return the entries of the rotation in their order, immutable
     */
    public static List<ITeamsRound> getRounds()
    {
        return manager().map(manager -> List.<ITeamsRound>copyOf(manager.getRounds())).orElse(List.of());
    }

    /**
     * Saves data of an addon with a rotation entry, under the addon's own key; it is kept with the rotation.
     *
     * @param round
     *            an entry of the rotation
     * @param key
     *            the addon's key, namespaced with its mod id
     * @param data
     *            the data to keep, copied; an empty tag removes the key
     * @return whether the entry is in the rotation and the data was saved
     */
    public static boolean setRoundExtensionData(ITeamsRound round, ResourceLocation key, CompoundTag data)
    {
        return manager().map(manager -> manager.setRoundExtensionData(round, key, data)).orElse(false);
    }

    /**
     * @param entity
     *            any entity
     * @return the team the entity fights for in the round being played: a player's team, or a combatant's. Empty for
     *         spectators, for players and entities taking no part, and between rounds.
     */
    public static Optional<ITeam> getTeam(Entity entity)
    {
        return manager().map(manager -> TeamsCombatants.getPlayingTeam(manager, entity));
    }

    /**
     * Makes a living entity fight for a team in the round being played, as a player of that team would.
     *
     * @param entity
     *            any living entity but a player, which joins teams through the team menu
     * @param team
     *            a team of the running round
     * @param captureWeight
     *            how much the entity counts on a capture point, where a player counts 1; 0 keeps it off the points
     * @return whether the entity is now a combatant of that team; false when no round runs, the team does not play
     *         it or the weight is negative or not finite
     */
    public static boolean assignCombatant(LivingEntity entity, ITeam team, float captureWeight)
    {
        return !(entity instanceof Player) && team instanceof Team internal && manager().map(manager -> TeamsCombatants.assign(manager, entity, internal, captureWeight)).orElse(false);
    }

    /**
     * Makes a living entity fight for a team in the round being played, counting as one player on capture points.
     *
     * @see #assignCombatant(LivingEntity, ITeam, float)
     */
    public static boolean assignCombatant(LivingEntity entity, ITeam team)
    {
        return assignCombatant(entity, team, 1F);
    }

    /**
     * Takes an entity out of the round; nothing happens to one that is not a combatant.
     *
     * @param entity
     *            a combatant
     */
    public static void releaseCombatant(LivingEntity entity)
    {
        if (manager().isPresent())
            TeamsCombatants.release(entity);
    }

    /**
     * @return whether the entity is a combatant of the round being played; players never are
     */
    public static boolean isCombatant(Entity entity)
    {
        return manager().map(manager -> TeamsCombatants.getCombatantTeam(manager, entity) != null).orElse(false);
    }

    /**
     * @return how much the entity counts on a capture point: 1 for a playing player, a combatant's capture weight, and
     *         0 for anyone else
     */
    public static float getCaptureWeight(Entity entity)
    {
        return manager().map(manager -> TeamsCombatants.getCaptureWeight(manager, entity)).orElse(0F);
    }

    /**
     * @return whether both entities play the running round, players or combatants, on opposite sides
     */
    public static boolean areEnemies(Entity first, Entity second)
    {
        return manager().map(manager -> TeamsCombatants.areEnemies(manager, first, second)).orElse(false);
    }

    /**
     * Ends the round being played at once, with the side of {@code winner} as the winner, as when a game type decides
     * it: the end-of-round event, scores and the break that follows all run as usual. For rules an addon adds, such as
     * a side running out of reinforcements it manages.
     *
     * @param winner
     *            a team of the running round; its allies win with it
     * @return whether a round was running with that team and has now ended
     */
    public static boolean declareWinner(ITeam winner)
    {
        return winner instanceof Team internal && manager().map(manager -> manager.declareWinner(internal)).orElse(false);
    }

    /**
     * Sets the rows an addon shows for a team on the scoreboard of the round being played, beside the team's players.
     * They replace the rows set before and are dropped when the round ends. Clients receive them with the next
     * scoreboard update, every two seconds.
     *
     * @param team
     *            a team of the running round
     * @param entries
     *            the rows, copied; empty removes them
     * @return whether a round was running with that team
     */
    public static boolean setScoreboardEntries(ITeam team, List<TeamsScoreEntry> entries)
    {
        return team instanceof Team internal && manager().map(manager -> manager.setScoreboardEntries(internal, entries)).orElse(false);
    }

    /**
     * Registers a listener for Teams events. Registering the same listener twice has no effect.
     */
    public static void addListener(ITeamsListener listener)
    {
        TeamsListeners.add(listener);
    }

    public static void removeListener(ITeamsListener listener)
    {
        TeamsListeners.remove(listener);
    }

    /**
     * Registers a rule that may keep players from taking the field again, such as a ticket limit. Registering the same rule
     * twice has no effect.
     */
    public static void addRespawnRule(ITeamsRespawnRule rule)
    {
        TeamsListeners.addRespawnRule(rule);
    }

    public static void removeRespawnRule(ITeamsRespawnRule rule)
    {
        TeamsListeners.removeRespawnRule(rule);
    }

    /**
     * Registers a provider of spawn positions, such as squads or rally points players may spawn on. Registering the same
     * provider twice has no effect.
     */
    public static void addSpawnProvider(ITeamsSpawnProvider provider)
    {
        TeamsListeners.addSpawnProvider(provider);
    }

    public static void removeSpawnProvider(ITeamsSpawnProvider provider)
    {
        TeamsListeners.removeSpawnProvider(provider);
    }

    private static Optional<TeamsManager> manager()
    {
        TeamsManager manager = TeamsManager.getInstance();
        return manager.isServerThread() ? Optional.of(manager) : Optional.empty();
    }
}
