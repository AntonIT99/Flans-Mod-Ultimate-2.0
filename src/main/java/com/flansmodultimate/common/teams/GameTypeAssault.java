package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Assault from the Labjac fork. The first side of the round defends, the second attacks; either
 * may be an alliance of several teams.
 *
 * <p>
 * The capture points are fought over one at a time, in letter order. Attackers have a limited
 * number of reinforcements: the score limit of the round, counted down by every attacker who
 * dies. Taking an objective gives some of them back and restarts the clock. The attackers win by
 * taking the last objective; the defenders win when the attackers run out of reinforcements or
 * of time.
 * </p>
 */
public final class GameTypeAssault extends GameTypeObjective
{
    private static final int DEFAULT_CAPTURE_TICKS = 500;
    private static final int DEFAULT_REINFORCEMENT_REFILL = 25;
    private static final int DEFAULT_STAGE_TIME_MINUTES = 12;
    private static final int DEFAULT_DEFENDER_DEPLOY_DELAY_SECONDS = 5;
    private static final int DEFAULT_DEFENDER_DEPLOY_DELAY_PER_OBJECTIVE_SECONDS = 2;
    private static final String SETTING_REINFORCEMENTS = "reinforcements";

    private int reinforcementRefill = DEFAULT_REINFORCEMENT_REFILL;
    private int stageTimeMinutes = DEFAULT_STAGE_TIME_MINUTES;
    private int defenderDeployDelaySeconds = DEFAULT_DEFENDER_DEPLOY_DELAY_SECONDS;
    private int defenderDeployDelayPerObjectiveSeconds = DEFAULT_DEFENDER_DEPLOY_DELAY_PER_OBJECTIVE_SECONDS;

    GameTypeAssault()
    {
        super("aslt", "Assault", TeamLimits.upToSides(2), DEFAULT_CAPTURE_TICKS);
    }

    @Override
    public ObjectiveMode getObjectiveMode()
    {
        return ObjectiveMode.ASSAULT;
    }

    @Override
    public int getAttackingTeamId(TeamsRound round)
    {
        return sideId(round, 1);
    }

    @Override
    public int getDefendingTeamId(TeamsRound round)
    {
        return sideId(round, 0);
    }

    @Override
    protected CaptureState initialState(TeamsRound round, List<ITeamCapturePoint> points, int index)
    {
        return CaptureState.ownedBy(getDefendingTeamId(round));
    }

    /** Index of the objective being fought over: the first one the attackers do not hold outright. */
    static int activeObjectiveIndex(List<CaptureState> states, int attackerSideId)
    {
        int index = 0;
        while (index < states.size() && states.get(index).isHeldBy(attackerSideId))
            index++;
        return index;
    }

    private int activeIndex(TeamsRound round, List<ITeamCapturePoint> points)
    {
        return activeObjectiveIndex(points.stream().map(ITeamCapturePoint::getCaptureState).toList(), getAttackingTeamId(round));
    }

    /** A map without capture points has nothing to take, so nobody wins it by default. */
    private boolean attackersWon(TeamsRound round, List<ITeamCapturePoint> points)
    {
        return !points.isEmpty() && activeIndex(round, points) >= points.size();
    }

    @Override
    protected void tickObjectives(TeamsManager manager, TeamsRound round, ServerLevel level, List<ITeamCapturePoint> points)
    {
        int active = activeIndex(round, points);
        for (int i = 0; i < points.size(); i++)
        {
            ITeamCapturePoint point = points.get(i);
            if (i == active)
                tickActiveObjective(manager, round, level, point);
            else
                // Objectives behind the front line stay taken; those ahead of it cannot be touched yet.
                point.setCaptureState(CaptureState.ownedBy(i < active ? getAttackingTeamId(round) : getDefendingTeamId(round)));
        }
    }

    private void tickActiveObjective(TeamsManager manager, TeamsRound round, ServerLevel level, ITeamCapturePoint point)
    {
        int attackerId = getAttackingTeamId(round);
        int defenderId = getDefendingTeamId(round);
        CaptureScan scan = CaptureScan.of(point, level, round);
        CaptureRules.Result result = CaptureRules.assault(point.getCaptureState(), attackerId, scan.count(attackerId), defenderId, scan.count(defenderId), captureTicks);
        point.setCaptureState(result.state());
        if (result.event() == CaptureRules.Event.NEUTRALIZED)
            pointNeutralized(manager, point);
        else if (result.event() == CaptureRules.Event.CAPTURED && result.state().ownerTeamId() == attackerId)
        {
            pointCaptured(manager, round, point, scan, attackerId, "message.flansmodultimate.teams.point_captured_reinforced");
            Team attackers = round.getSideLeader(attackerId);
            if (attackers != null)
                manager.addTeamScore(attackers, -Math.min(reinforcementRefill, manager.getTeamScore(attackers)));
            manager.setRoundTimeLeftTicks(Math.max(20, stageTimeMinutes * 60 * 20));
        }
        else if (result.event() == CaptureRules.Event.CAPTURED)
            pointCaptured(manager, round, point, scan, defenderId, "message.flansmodultimate.teams.point_recaptured");
    }

    @Override
    public void playerKilled(TeamsManager manager, ServerPlayer victim, DamageSource source)
    {
        Team victimTeam = manager.getPlayerTeam(victim);
        super.playerKilled(manager, victim, source);
        spendReinforcement(manager, victimTeam);
    }

    @Override
    public void combatantKilled(TeamsManager manager, LivingEntity victim, Team victimTeam, DamageSource source)
    {
        super.combatantKilled(manager, victim, victimTeam, source);
        spendReinforcement(manager, victimTeam);
    }

    /** Every attacker who dies, player or combatant, costs a reinforcement, however it happened. */
    private void spendReinforcement(TeamsManager manager, @Nullable Team victimTeam)
    {
        TeamsRound round = manager.getCurrentRound().orElse(null);
        Team attackers = round == null ? null : round.getSideLeader(getAttackingTeamId(round));
        if (attackers != null && round.areAllied(attackers, victimTeam))
            manager.addTeamScore(attackers, 1);
    }

    @Override
    protected void scoreKill(TeamsManager manager, Team attackerTeam)
    {
        // Kills are not worth team score here; the score of the attackers counts their own losses.
    }

    @Override
    public boolean hasWinner(TeamsManager manager, Team team)
    {
        TeamsRound round = manager.getCurrentRound().orElse(null);
        if (round == null)
            return false;
        int sideId = round.getSideId(team);
        boolean attackersWon = attackersWon(round, manager.getCapturePoints());
        if (sideId == getAttackingTeamId(round))
            return attackersWon;
        return !attackersWon && sideId == getDefendingTeamId(round)
            && (manager.getRoundTimeLeftTicks() <= 0 || manager.getTeamScore(round.getSideLeader(getAttackingTeamId(round))) >= round.getScoreLimit());
    }

    @Nullable
    @Override
    public Team getWinner(TeamsManager manager)
    {
        return manager.getCurrentRound().map(round -> round.getSideLeader(attackersWon(round, manager.getCapturePoints()) ? getAttackingTeamId(round) : getDefendingTeamId(round))).orElse(null);
    }

    @Override
    public int getDeployDelaySeconds(TeamsManager manager, int teamId)
    {
        // Attackers return at once; defenders wait longer the more ground they have lost.
        TeamsRound round = manager.getCurrentRound().orElse(null);
        if (round == null || round.getSideId(teamId) != getDefendingTeamId(round))
            return 0;
        return defenderDeployDelaySeconds + activeIndex(round, manager.getCapturePoints()) * defenderDeployDelayPerObjectiveSeconds;
    }

    @Nullable
    @Override
    public Vec3 getSpawnPoint(TeamsManager manager, ServerPlayer player)
    {
        UUID preferred = preferredSpawn(player);
        TeamsRound round = manager.getCurrentRound().orElse(null);
        Vec3 frontline = round == null || HEADQUARTERS.equals(preferred) ? null : frontlineSpawn(manager, round, sideIdOf(manager, player));
        return frontline != null ? frontline : headquartersSpawn(manager, player, teamIdOf(manager, player));
    }

    /**
     * Where a side comes back close to the fighting: the defenders at the nearest objective they
     * still own, the attackers at the last one they took.
     */
    @Nullable
    private Vec3 frontlineSpawn(TeamsManager manager, TeamsRound round, int sideId)
    {
        List<ITeamCapturePoint> points = manager.getCapturePoints();
        int active = activeIndex(round, points);
        if (sideId == getDefendingTeamId(round))
        {
            for (int i = active; i < points.size(); i++)
                if (points.get(i).getOwnerId() == sideId)
                    return spawnAt(manager, points.get(i));
        }
        else if (sideId == getAttackingTeamId(round))
        {
            for (int i = Math.min(active - 1, points.size() - 1); i >= 0; i--)
                if (points.get(i).getOwnerId() == sideId)
                    return spawnAt(manager, points.get(i));
        }
        return null;
    }

    @Override
    public boolean setVariable(String variable, String value)
    {
        switch (variable.toLowerCase(Locale.ROOT))
        {
            case SETTING_REINFORCEMENTS -> reinforcementRefill = Math.max(1, Integer.parseInt(value));
            case "stagetime" -> stageTimeMinutes = Math.max(1, Integer.parseInt(value));
            case "defenderdeploydelay" -> defenderDeployDelaySeconds = Math.max(0, Integer.parseInt(value));
            case "defenderdeploydelayperobjective" -> defenderDeployDelayPerObjectiveSeconds = Math.max(0, Integer.parseInt(value));
            default -> {
                return super.setVariable(variable, value);
            }
        }
        return true;
    }

    @Override
    public Map<String, String> getVariables()
    {
        Map<String, String> variables = super.getVariables();
        variables.put(SETTING_REINFORCEMENTS, Integer.toString(reinforcementRefill));
        variables.put("stagetime", Integer.toString(stageTimeMinutes));
        variables.put("defenderdeploydelay", Integer.toString(defenderDeployDelaySeconds));
        variables.put("defenderdeploydelayperobjective", Integer.toString(defenderDeployDelayPerObjectiveSeconds));
        return variables;
    }

    @Override
    public void loadSettings(CompoundTag tag)
    {
        super.loadSettings(tag);
        reinforcementRefill = readInt(tag, SETTING_REINFORCEMENTS, DEFAULT_REINFORCEMENT_REFILL, 1);
        stageTimeMinutes = readInt(tag, "stage_time", DEFAULT_STAGE_TIME_MINUTES, 1);
        defenderDeployDelaySeconds = readInt(tag, "defender_deploy_delay", DEFAULT_DEFENDER_DEPLOY_DELAY_SECONDS, 0);
        defenderDeployDelayPerObjectiveSeconds = readInt(tag, "defender_deploy_delay_per_objective", DEFAULT_DEFENDER_DEPLOY_DELAY_PER_OBJECTIVE_SECONDS, 0);
    }

    @Override
    public void saveSettings(CompoundTag tag)
    {
        super.saveSettings(tag);
        tag.putInt(key(SETTING_REINFORCEMENTS), reinforcementRefill);
        tag.putInt(key("stage_time"), stageTimeMinutes);
        tag.putInt(key("defender_deploy_delay"), defenderDeployDelaySeconds);
        tag.putInt(key("defender_deploy_delay_per_objective"), defenderDeployDelayPerObjectiveSeconds);
    }
}
