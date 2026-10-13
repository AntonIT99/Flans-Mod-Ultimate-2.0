package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Assault from the Labjac fork. The first team of the round defends, the second attacks.
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
    private static final int DEFENDER_TEAM_ID = FIRST_TEAM_ID;
    private static final int ATTACKER_TEAM_ID = SECOND_TEAM_ID;
    private static final String SETTING_REINFORCEMENTS = "reinforcements";

    private int reinforcementRefill = DEFAULT_REINFORCEMENT_REFILL;
    private int stageTimeMinutes = DEFAULT_STAGE_TIME_MINUTES;
    private int defenderDeployDelaySeconds = DEFAULT_DEFENDER_DEPLOY_DELAY_SECONDS;
    private int defenderDeployDelayPerObjectiveSeconds = DEFAULT_DEFENDER_DEPLOY_DELAY_PER_OBJECTIVE_SECONDS;

    GameTypeAssault()
    {
        super("aslt", "Assault", DEFAULT_CAPTURE_TICKS);
    }

    @Override
    public ObjectiveMode getObjectiveMode()
    {
        return ObjectiveMode.ASSAULT;
    }

    @Override
    public int getAttackingTeamId()
    {
        return ATTACKER_TEAM_ID;
    }

    @Override
    public int getDefendingTeamId()
    {
        return DEFENDER_TEAM_ID;
    }

    @Override
    protected CaptureState initialState(List<ITeamCapturePoint> points, int index)
    {
        return CaptureState.ownedBy(DEFENDER_TEAM_ID);
    }

    /** Index of the objective being fought over: the first one the attackers do not hold outright. */
    static int activeObjectiveIndex(List<CaptureState> states)
    {
        int index = 0;
        while (index < states.size() && states.get(index).isHeldBy(ATTACKER_TEAM_ID))
            index++;
        return index;
    }

    private static int activeIndex(List<ITeamCapturePoint> points)
    {
        return activeObjectiveIndex(points.stream().map(ITeamCapturePoint::getCaptureState).toList());
    }

    /** A map without capture points has nothing to take, so nobody wins it by default. */
    private static boolean attackersWon(List<ITeamCapturePoint> points)
    {
        return !points.isEmpty() && activeIndex(points) >= points.size();
    }

    @Override
    protected void tickObjectives(TeamsManager manager, TeamsRound round, ServerLevel level, List<ITeamCapturePoint> points)
    {
        int active = activeIndex(points);
        for (int i = 0; i < points.size(); i++)
        {
            ITeamCapturePoint point = points.get(i);
            if (i == active)
                tickActiveObjective(manager, round, level, point);
            else
                // Objectives behind the front line stay taken; those ahead of it cannot be touched yet.
                point.setCaptureState(CaptureState.ownedBy(i < active ? ATTACKER_TEAM_ID : DEFENDER_TEAM_ID));
        }
    }

    private void tickActiveObjective(TeamsManager manager, TeamsRound round, ServerLevel level, ITeamCapturePoint point)
    {
        CaptureScan scan = CaptureScan.of(point, level, round);
        CaptureRules.Result result = CaptureRules.assault(point.getCaptureState(), ATTACKER_TEAM_ID, scan.count(ATTACKER_TEAM_ID), DEFENDER_TEAM_ID, scan.count(DEFENDER_TEAM_ID), captureTicks);
        point.setCaptureState(result.state());
        if (result.event() == CaptureRules.Event.NEUTRALIZED)
            pointNeutralized(manager, point);
        else if (result.event() == CaptureRules.Event.CAPTURED && result.state().ownerTeamId() == ATTACKER_TEAM_ID)
        {
            pointCaptured(manager, round, point, scan, ATTACKER_TEAM_ID, "message.flansmodultimate.teams.point_captured_reinforced");
            Team attackers = round.getTeam(ATTACKER_TEAM_ID - FIRST_TEAM_ID);
            if (attackers != null)
                manager.addTeamScore(attackers, -Math.min(reinforcementRefill, manager.getTeamScore(attackers)));
            manager.setRoundTimeLeftTicks(Math.max(20, stageTimeMinutes * 60 * 20));
        }
        else if (result.event() == CaptureRules.Event.CAPTURED)
            pointCaptured(manager, round, point, scan, DEFENDER_TEAM_ID, "message.flansmodultimate.teams.point_recaptured");
    }

    @Override
    public void playerKilled(TeamsManager manager, ServerPlayer victim, DamageSource source)
    {
        Team victimTeam = manager.getPlayerTeam(victim);
        super.playerKilled(manager, victim, source);
        // Every attacker who dies costs a reinforcement, however it happened.
        Team attackers = manager.getCurrentRound().map(round -> round.getTeam(ATTACKER_TEAM_ID - FIRST_TEAM_ID)).orElse(null);
        if (attackers != null && attackers == victimTeam)
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
        Team attackers = round.getTeam(ATTACKER_TEAM_ID - FIRST_TEAM_ID);
        boolean attackersWon = attackersWon(manager.getCapturePoints());
        if (team == attackers)
            return attackersWon;
        return !attackersWon && team == round.getTeam(DEFENDER_TEAM_ID - FIRST_TEAM_ID) && (manager.getRoundTimeLeftTicks() <= 0 || manager.getTeamScore(attackers) >= round.getScoreLimit());
    }

    @Nullable
    @Override
    public Team getWinner(TeamsManager manager)
    {
        return manager.getCurrentRound().map(round -> round.getTeam((attackersWon(manager.getCapturePoints()) ? ATTACKER_TEAM_ID : DEFENDER_TEAM_ID) - FIRST_TEAM_ID)).orElse(null);
    }

    @Override
    public int getDeployDelaySeconds(TeamsManager manager, int teamId)
    {
        // Attackers return at once; defenders wait longer the more ground they have lost.
        if (teamId != DEFENDER_TEAM_ID)
            return 0;
        return defenderDeployDelaySeconds + activeIndex(manager.getCapturePoints()) * defenderDeployDelayPerObjectiveSeconds;
    }

    @Nullable
    @Override
    public Vec3 getSpawnPoint(TeamsManager manager, ServerPlayer player)
    {
        int teamId = teamIdOf(manager, player);
        UUID preferred = preferredSpawn(player);
        Vec3 frontline = HEADQUARTERS.equals(preferred) ? null : frontlineSpawn(manager, teamId);
        return frontline != null ? frontline : headquartersSpawn(manager, player, teamId);
    }

    /**
     * Where a team comes back close to the fighting: the defenders at the nearest objective they
     * still own, the attackers at the last one they took.
     */
    @Nullable
    private static Vec3 frontlineSpawn(TeamsManager manager, int teamId)
    {
        List<ITeamCapturePoint> points = manager.getCapturePoints();
        int active = activeIndex(points);
        if (teamId == DEFENDER_TEAM_ID)
        {
            for (int i = active; i < points.size(); i++)
                if (points.get(i).getOwnerId() == DEFENDER_TEAM_ID)
                    return spawnAt(manager, points.get(i));
        }
        else if (teamId == ATTACKER_TEAM_ID)
        {
            for (int i = Math.min(active - 1, points.size() - 1); i >= 0; i--)
                if (points.get(i).getOwnerId() == ATTACKER_TEAM_ID)
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
