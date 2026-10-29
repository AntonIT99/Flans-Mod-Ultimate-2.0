package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Domination from the Labjac fork: every capture point starts neutral, a point only changes
 * hands while one side stands on it alone, and the side holding the most points gains score until
 * one reaches the score limit. Players come back at any base their team owns or their side holds.
 */
public final class GameTypeDomination extends GameTypeObjective
{
    private static final int DEFAULT_CAPTURE_TICKS = 400;
    private static final int DEFAULT_TICKET_DRAIN_INTERVAL = 20;
    private static final int DEFAULT_KILL_TICKET_VALUE = 5;

    private int ticketDrainInterval = DEFAULT_TICKET_DRAIN_INTERVAL;
    private int killTicketValue = DEFAULT_KILL_TICKET_VALUE;
    private int ticketDrainTimer;

    GameTypeDomination()
    {
        super("dom", "Domination", TeamLimits.upToSides(TeamsRound.MAX_SIDES), DEFAULT_CAPTURE_TICKS);
    }

    @Override
    public ObjectiveMode getObjectiveMode()
    {
        return ObjectiveMode.DOMINATION;
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
        return CaptureState.NEUTRAL;
    }

    @Override
    protected void tickObjectives(TeamsManager manager, TeamsRound round, ServerLevel level, List<ITeamCapturePoint> points)
    {
        for (ITeamCapturePoint point : points)
        {
            CaptureScan scan = CaptureScan.of(point, level, round);
            int onlyTeam = scan.onlyTeamId();
            CaptureRules.Result result = CaptureRules.restrict(point.getCaptureState(),
                CaptureRules.domination(point.getCaptureState(), onlyTeam, scan.count(onlyTeam), scan.isContested(), captureTicks), round::mayCapture);
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
    public boolean setVariable(String variable, String value)
    {
        switch (variable.toLowerCase(Locale.ROOT))
        {
            case "ticketdraininterval" -> ticketDrainInterval = Math.max(1, Integer.parseInt(value));
            case "killticketvalue" -> killTicketValue = Math.max(0, Integer.parseInt(value));
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
        return variables;
    }

    @Override
    public void loadSettings(CompoundTag tag)
    {
        super.loadSettings(tag);
        ticketDrainInterval = readInt(tag, "ticket_drain_interval", DEFAULT_TICKET_DRAIN_INTERVAL, 1);
        killTicketValue = readInt(tag, "kill_ticket_value", DEFAULT_KILL_TICKET_VALUE, 0);
    }

    @Override
    public void saveSettings(CompoundTag tag)
    {
        super.saveSettings(tag);
        tag.putInt(key("ticket_drain_interval"), ticketDrainInterval);
        tag.putInt(key("kill_ticket_value"), killTicketValue);
    }
}
