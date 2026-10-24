package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.api.teams.ITeamsListener;
import com.flansmodultimate.api.teams.ITeamsLiveRound;
import com.flansmodultimate.api.teams.ITeamsRespawnRule;
import com.flansmodultimate.api.teams.ITeamsSpawnProvider;
import com.flansmodultimate.api.teams.TeamsCapturePoint;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** The addon listeners of Teams events, called on the server thread. One failing listener does not stop the others or the round. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsListeners
{
    private static final List<ITeamsListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<ITeamsRespawnRule> RESPAWN_RULES = new CopyOnWriteArrayList<>();
    private static final List<ITeamsSpawnProvider> SPAWN_PROVIDERS = new CopyOnWriteArrayList<>();

    public static void add(ITeamsListener listener)
    {
        if (!LISTENERS.contains(listener))
            LISTENERS.add(listener);
    }

    public static void remove(ITeamsListener listener)
    {
        LISTENERS.remove(listener);
    }

    public static void addRespawnRule(ITeamsRespawnRule rule)
    {
        if (!RESPAWN_RULES.contains(rule))
            RESPAWN_RULES.add(rule);
    }

    public static void removeRespawnRule(ITeamsRespawnRule rule)
    {
        RESPAWN_RULES.remove(rule);
    }

    /** Whether every respawn rule lets the player take the field; a rule that throws counts as allowing. */
    static boolean mayRespawn(ITeamsLiveRound round, ServerPlayer player, ITeam team, ITeamsRespawnRule.Reason reason)
    {
        boolean allowed = true;
        // Every rule is asked, even after one refused, so none charges for a respawn another one refuses unseen.
        for (ITeamsRespawnRule rule : RESPAWN_RULES)
        {
            try
            {
                allowed &= rule.mayRespawn(round, player, team, reason);
            }
            // A rule belongs to another mod: whatever it throws is reported and must not keep the player out.
            catch (RuntimeException e)
            {
                FlansLog.log.error("Teams respawn rule {} failed", rule.getClass().getName(), e);
            }
        }
        return allowed;
    }

    public static void addSpawnProvider(ITeamsSpawnProvider provider)
    {
        if (!SPAWN_PROVIDERS.contains(provider))
            SPAWN_PROVIDERS.add(provider);
    }

    public static void removeSpawnProvider(ITeamsSpawnProvider provider)
    {
        SPAWN_PROVIDERS.remove(provider);
    }

    /** The first position a spawn provider offers the player, or empty to leave it to the game type. */
    static Optional<Vec3> spawnPosition(ITeamsLiveRound round, ServerPlayer player, ITeam team, ITeamsRespawnRule.Reason reason)
    {
        for (ITeamsSpawnProvider provider : SPAWN_PROVIDERS)
        {
            try
            {
                // A provider of another mod might break its contract and hand back null.
                Optional<Vec3> position = Objects.requireNonNullElse(provider.spawnPosition(round, player, team, reason), Optional.empty());
                if (position.filter(TeamsListeners::finite).isPresent())
                    return position;
            }
            // A provider belongs to another mod: whatever it throws is reported and leaves the spawn to the game type.
            catch (RuntimeException e)
            {
                FlansLog.log.error("Teams spawn provider {} failed", provider.getClass().getName(), e);
            }
        }
        return Optional.empty();
    }

    private static boolean finite(Vec3 position)
    {
        return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
    }

    static void roundStarted(ITeamsLiveRound round)
    {
        dispatch("roundStarted", listener -> listener.roundStarted(round));
    }

    static void roundEnded(ITeamsLiveRound round, @Nullable ITeam winner)
    {
        dispatch("roundEnded", listener -> listener.roundEnded(round, winner));
    }

    static void capturePointChanged(ITeamsLiveRound round, TeamsCapturePoint point, int previousOwnerSide)
    {
        dispatch("capturePointChanged", listener -> listener.capturePointChanged(round, point, previousOwnerSide));
    }

    static void combatantKilled(ITeamsLiveRound round, LivingEntity victim, @Nullable LivingEntity killer, DamageSource source)
    {
        dispatch("combatantKilled", listener -> listener.combatantKilled(round, victim, killer, source));
    }

    private static void dispatch(String event, Consumer<ITeamsListener> call)
    {
        for (ITeamsListener listener : LISTENERS)
        {
            try
            {
                call.accept(listener);
            }
            // A listener belongs to another mod: whatever it throws is reported and must not break the round.
            catch (RuntimeException e)
            {
                FlansLog.log.error("Teams listener {} failed in {}", listener.getClass().getName(), event, e);
            }
        }
    }
}
