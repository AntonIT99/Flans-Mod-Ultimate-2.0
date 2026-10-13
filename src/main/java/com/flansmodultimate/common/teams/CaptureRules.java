package com.flansmodultimate.common.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Capture arithmetic shared by Domination, Conquest and Assault, ported from the game types of
 * the Labjac fork. Each rule advances a point by one server tick and reports whether that tick
 * changed its owner; the game types own everything else.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CaptureRules
{
    public enum Event
    {
        NONE, CAPTURED, NEUTRALIZED
    }

    public record Result(CaptureState state, Event event)
    {
        static Result of(int owner, int capturing, float progress, boolean contested, Event event)
        {
            return new Result(new CaptureState(owner, capturing, progress, contested), event);
        }

        static Result unchanged(CaptureState state, boolean contested)
        {
            return of(state.ownerTeamId(), state.capturingTeamId(), state.progress(), contested, Event.NONE);
        }
    }

    /** With nobody on the point an unfinished capture drains away at the pace of one player. */
    public static CaptureState decay(CaptureState state, int captureTicks)
    {
        int capturing = state.capturingTeamId();
        float progress = state.progress();
        if (capturing == 0)
            return new CaptureState(state.ownerTeamId(), 0, progress, false);

        float delta = 1F / Math.max(1, captureTicks);
        if (state.ownerTeamId() == 0)
        {
            progress -= delta;
            if (progress <= 0F)
            {
                progress = 0F;
                capturing = 0;
            }
        }
        else
        {
            progress += delta;
            if (progress >= 1F)
            {
                progress = 1F;
                capturing = 0;
            }
        }
        return new CaptureState(state.ownerTeamId(), capturing, progress, false);
    }

    /**
     * Domination: a point only moves while a single team stands on it, at a pace set by how many
     * of them there are. Any enemy presence freezes it.
     */
    public static Result domination(CaptureState state, int onlyTeamId, int playersOnPoint, boolean contested, int captureTicks)
    {
        if (contested)
            return Result.unchanged(state, true);
        if (onlyTeamId == 0)
            return new Result(decay(state, captureTicks), Event.NONE);
        return advance(state, onlyTeamId, Math.max(1, playersOnPoint), false, captureTicks);
    }

    /**
     * Conquest: both teams may stand on the point; the side with more players makes progress at
     * the pace of its head-count advantage, and equal numbers hold it still.
     */
    public static Result pressure(CaptureState state, int firstTeamId, int firstCount, int secondTeamId, int secondCount, int captureTicks)
    {
        if (firstCount <= 0 && secondCount <= 0)
            return new Result(decay(state, captureTicks), Event.NONE);
        boolean contested = firstCount > 0 && secondCount > 0;
        int net = firstCount - secondCount;
        if (net == 0)
            return Result.unchanged(state, contested);
        return advance(state, net > 0 ? firstTeamId : secondTeamId, Math.abs(net), contested, captureTicks);
    }

    /**
     * The active objective of an Assault round. It follows the Conquest rule, except that
     * defenders outnumbering the attackers on a point they still own restore it at once instead
     * of having to win it back tick by tick.
     */
    public static Result assault(CaptureState state, int attackerTeamId, int attackers, int defenderTeamId, int defenders, int captureTicks)
    {
        if (attackers <= 0 && defenders <= 0)
            return new Result(decay(state, captureTicks), Event.NONE);
        boolean contested = attackers > 0 && defenders > 0;
        int net = attackers - defenders;
        if (net == 0)
            return Result.unchanged(state, contested);
        if (net < 0 && state.ownerTeamId() == defenderTeamId)
            return Result.of(defenderTeamId, 0, 1F, contested, Event.NONE);
        int active = net > 0 ? attackerTeamId : defenderTeamId;
        if (state.ownerTeamId() == active)
            return Result.unchanged(state, contested);
        return advance(state, active, Math.abs(net), contested, captureTicks);
    }

    /** One tick of a team working on a point nobody owns. */
    private static Result advanceNeutral(CaptureState state, int activeTeamId, float delta, boolean contested)
    {
        int capturing = state.capturingTeamId();
        float progress = state.progress();
        if (capturing != 0 && capturing != activeTeamId && progress > 0F)
        {
            // Somebody else left an unfinished capture, which has to be undone first.
            progress -= delta;
            return progress <= 0F ? Result.of(0, activeTeamId, 0F, contested, Event.NONE) : Result.of(0, capturing, progress, contested, Event.NONE);
        }
        progress += delta;
        return progress >= 1F ? Result.of(activeTeamId, 0, 1F, contested, Event.CAPTURED) : Result.of(0, activeTeamId, progress, contested, Event.NONE);
    }

    /** One tick of {@code activeTeamId} working on the point with the given head-count weight. */
    private static Result advance(CaptureState state, int activeTeamId, int weight, boolean contested, int captureTicks)
    {
        int owner = state.ownerTeamId();
        int capturing;
        float progress = state.progress();
        float delta = (float) weight / Math.max(1, captureTicks);
        Event event = Event.NONE;

        if (owner == activeTeamId)
        {
            // The owner repairing a point the enemy had started to take down.
            progress = Math.min(1F, progress + delta);
            capturing = progress >= 1F ? 0 : activeTeamId;
        }
        else if (owner == 0)
            return advanceNeutral(state, activeTeamId, delta, contested);
        else
        {
            progress -= delta;
            capturing = activeTeamId;
            if (progress <= 0F)
            {
                progress = 0F;
                owner = 0;
                event = Event.NEUTRALIZED;
            }
        }
        return Result.of(owner, capturing, progress, contested, event);
    }
}
