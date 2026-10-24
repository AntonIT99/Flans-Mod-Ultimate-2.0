package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Conquest from the Labjac fork: every side may stand on a capture point and the one with the most
 * players takes it. The side holding the most points gains score until one reaches the score limit.
 * Dead players choose on the deployment map whether to come back at headquarters or at a point
 * their side holds.
 */
public final class GameTypeConquest extends GameTypeObjective
{
    private static final int DEFAULT_CAPTURE_TICKS = 500;
    private static final int DEFAULT_TICKET_DRAIN_INTERVAL = 20;
    private static final int DEFAULT_KILL_TICKET_VALUE = 5;
    private static final int DEFAULT_DEPLOY_DELAY_SECONDS = 5;

    private int ticketDrainInterval = DEFAULT_TICKET_DRAIN_INTERVAL;
    private int killTicketValue = DEFAULT_KILL_TICKET_VALUE;
    private int deployDelaySeconds = DEFAULT_DEPLOY_DELAY_SECONDS;
    /** Starts each team with the point at its end of the line instead of every point neutral. */
    private boolean preCaptureEndPoints;
    private int ticketDrainTimer;

    GameTypeConquest()
    {
        super("con", "Conquest", TeamLimits.upToSides(TeamsRound.MAX_SIDES), DEFAULT_CAPTURE_TICKS);
    }

    @Override
    public ObjectiveMode getObjectiveMode()
    {
        return ObjectiveMode.CONQUEST;
    }

    @Override
    public void roundStarted(TeamsManager manager)
    {
        super.roundStarted(manager);
        ticketDrainTimer = 0;
    }

    @Override
    protected CaptureState initialState(TeamsRound round, List<ITeamCapturePoint> points, int index)
    {
        // A line of points only has two ends, so only two sides can start with one each.
        if (!preCaptureEndPoints || points.size() < 2 || round.getSideCount() != 2)
            return CaptureState.NEUTRAL;
        if (index == 0)
            return CaptureState.ownedBy(sideId(round, 0));
        return index == points.size() - 1 ? CaptureState.ownedBy(sideId(round, 1)) : CaptureState.NEUTRAL;
    }

    @Override
    protected void tickObjectives(TeamsManager manager, TeamsRound round, ServerLevel level, List<ITeamCapturePoint> points)
    {
        for (ITeamCapturePoint point : points)
        {
            CaptureScan scan = CaptureScan.of(point, level, round);
            CaptureRules.Result result = CaptureRules.restrict(point.getCaptureState(), CaptureRules.pressure(point.getCaptureState(), scan.counts(), captureTicks), round::mayCapture);
            point.setCaptureState(result.state());
            if (result.event() == CaptureRules.Event.CAPTURED)
                pointCaptured(manager, round, point, scan, result.state().ownerTeamId(), "message.flansmodultimate.teams.point_captured");
            else if (result.event() == CaptureRules.Event.NEUTRALIZED)
                pointNeutralized(manager, point);
        }

        if (++ticketDrainTimer >= ticketDrainInterval)
        {
            ticketDrainTimer = 0;
            drainTickets(manager, round, points);
        }
    }

    @Override
    protected void scoreKill(TeamsManager manager, Team attackerTeam)
    {
        manager.addTeamScore(attackerTeam, killTicketValue);
    }

    @Override
    public int getDeployDelaySeconds(TeamsManager manager, int teamId)
    {
        return deployDelaySeconds;
    }

    @Nullable
    @Override
    public Vec3 getSpawnPoint(TeamsManager manager, ServerPlayer player)
    {
        int teamId = teamIdOf(manager, player);
        UUID preferred = preferredSpawn(player);
        if (preferred != null && !HEADQUARTERS.equals(preferred))
        {
            ITeamCapturePoint point = manager.getCapturePoints().stream().filter(candidate -> candidate.getObjectId().equals(preferred)).findFirst().orElse(null);
            Vec3 spawn = point != null && point.getOwnerId() == sideIdOf(manager, player) ? spawnAt(manager, point) : null;
            if (spawn != null)
                return spawn;
            // The point was lost or lost its spawner; the choice falls back to headquarters for good.
            forgetPreferredSpawn(player);
        }
        return headquartersSpawn(manager, player, teamId);
    }

    @Override
    public boolean setVariable(String variable, String value)
    {
        switch (variable.toLowerCase(Locale.ROOT))
        {
            case "ticketdraininterval" -> ticketDrainInterval = Math.max(1, Integer.parseInt(value));
            case "killticketvalue" -> killTicketValue = Math.max(0, Integer.parseInt(value));
            case "deploydelay" -> deployDelaySeconds = Math.max(0, Integer.parseInt(value));
            case "precaptureendpoints", "startwithendpointscaptured" -> preCaptureEndPoints = Boolean.parseBoolean(value);
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
        variables.put("ticketdraininterval", Integer.toString(ticketDrainInterval));
        variables.put("killticketvalue", Integer.toString(killTicketValue));
        variables.put("deploydelay", Integer.toString(deployDelaySeconds));
        variables.put("precaptureendpoints", Boolean.toString(preCaptureEndPoints));
        return variables;
    }

    @Override
    public void loadSettings(CompoundTag tag)
    {
        super.loadSettings(tag);
        ticketDrainInterval = readInt(tag, "ticket_drain_interval", DEFAULT_TICKET_DRAIN_INTERVAL, 1);
        killTicketValue = readInt(tag, "kill_ticket_value", DEFAULT_KILL_TICKET_VALUE, 0);
        deployDelaySeconds = readInt(tag, "deploy_delay", DEFAULT_DEPLOY_DELAY_SECONDS, 0);
        preCaptureEndPoints = tag.getBoolean(key("pre_capture_end_points"));
    }

    @Override
    public void saveSettings(CompoundTag tag)
    {
        super.saveSettings(tag);
        tag.putInt(key("ticket_drain_interval"), ticketDrainInterval);
        tag.putInt(key("kill_ticket_value"), killTicketValue);
        tag.putInt(key("deploy_delay"), deployDelaySeconds);
        tag.putBoolean(key("pre_capture_end_points"), preCaptureEndPoints);
    }
}
