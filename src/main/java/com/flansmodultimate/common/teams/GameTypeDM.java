package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
            && PlayerData.getInstance(player).getScore() >= manager.getCurrentRound().map(TeamsRound::getScoreLimit).orElse(Integer.MAX_VALUE));
    }

    /** Teams only split the field here, so a fighter comes back at any team's spawn and cannot be camped at their own. */
    @Override
    @Nullable
    public Vec3 getSpawnPoint(TeamsManager manager, ServerPlayer player)
    {
        TeamsRound round = manager.getCurrentRound().orElse(null);
        if (round == null || PlayerData.getInstance(player).getNewTeam() == Team.SPECTATORS)
            return super.getSpawnPoint(manager, player);
        List<Vec3> spawns = round.getTeamIds().stream().map(id -> round.getTeamId(Team.getTeam(id))).filter(teamId -> teamId >= TeamsRound.FIRST_TEAM_ID)
            .flatMap(teamId -> manager.getSpawnPositions(teamId, false).stream()).distinct().toList();
        return spawns.isEmpty() ? super.getSpawnPoint(manager, player) : spawns.get(player.getRandom().nextInt(spawns.size()));
    }

    @Override
    public boolean hasMvpPerTeam()
    {
        return false;
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
