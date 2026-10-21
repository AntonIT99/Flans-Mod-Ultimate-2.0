package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Free-for-all deathmatch. */
public final class GameTypeDM extends GameType
{
    GameTypeDM()
    {
        super("dm", "Deathmatch", TeamLimits.exactly(2), true);
    }

    @Override
    public boolean canPlayerBeAttacked(ServerPlayer victim, ServerPlayer attacker)
    {
        return victim != attacker && getPlayerTeam(victim) != Team.SPECTATORS && getPlayerTeam(attacker) != Team.SPECTATORS;
    }

    /** Everyone fights everyone here, combatants included. */
    @Override
    public boolean canBeAttacked(LivingEntity victim, LivingEntity attacker)
    {
        return victim != attacker && TeamsCombatants.getTeam(victim) != Team.SPECTATORS && TeamsCombatants.getTeam(attacker) != Team.SPECTATORS;
    }

    @Override
    public boolean hasWinner(TeamsManager manager, Team team)
    {
        return manager.getServer().getPlayerList().getPlayers().stream().anyMatch(player -> manager.getPlayerTeam(player) != Team.SPECTATORS
            && com.flansmodultimate.common.PlayerData.getInstance(player).getScore() >= manager.getCurrentRound().map(TeamsRound::getScoreLimit).orElse(Integer.MAX_VALUE));
    }

    @Override
    public boolean isAutoBalanceEnabled()
    {
        return false;
    }

    @Override
    public boolean isScoreboardSortedByTeam()
    {
        return false;
    }
}
