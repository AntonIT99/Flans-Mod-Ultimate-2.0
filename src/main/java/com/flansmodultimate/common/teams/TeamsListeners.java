package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsListener;
import com.flansmodultimate.api.ITeamsLiveRound;
import com.flansmodultimate.api.ITeamsRespawnRule;
import com.flansmodultimate.api.TeamsCapturePoint;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** The addon listeners of Teams events, called on the server thread. One failing listener does not stop the others or the round. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsListeners
{
    private static final List<ITeamsListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<ITeamsRespawnRule> RESPAWN_RULES = new CopyOnWriteArrayList<>();

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
