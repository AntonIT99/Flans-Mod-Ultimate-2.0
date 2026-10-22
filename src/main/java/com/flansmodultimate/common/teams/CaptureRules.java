package com.flansmodultimate.common.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.function.IntPredicate;

/**
 * Capture arithmetic shared by Domination, Conquest and Assault, ported from the game types of
 * the Labjac fork. Each rule advances a point by one server tick and reports whether that tick
 * changed its owner; the game types own everything else.
 *
 * <p>
 * Presence on a point is a weight rather than a head count: a player weighs 1 and a combatant
 * what its addon gave it, so the pace of a capture follows the summed weights.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CaptureRules
{
    /** Weights closer than this count as equal, so sums of fractional weights balance as they should. */
    private static final float EQUAL_WEIGHT = 1.0E-4F;

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

    /**
     * Holds back a tick's result that would let a side take a point it may not take: a defender of a round played attacker
     * against defender pushes a point it does not hold, or ends up holding one it did not. Such a point stays as it was,
     * contested. A defender still pushes back a capture of a point it holds.
     *
     * @param mayCapture
     *            whether a side id may take points it does not hold
     */
    public static Result restrict(CaptureState before, Result result, IntPredicate mayCapture)
    {
        CaptureState after = result.state();
        int pushing = after.capturingTeamId();
        boolean barredPush = pushing != 0 && pushing != before.ownerTeamId() && !mayCapture.test(pushing);
        boolean barredTake = after.ownerTeamId() != 0 && after.ownerTeamId() != before.ownerTeamId() && !mayCapture.test(after.ownerTeamId());
        return barredPush || barredTake ? Result.unchanged(before, true) : result;
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
     * Domination: a point only moves while a single team stands on it, at a pace set by its weight
     * there. Any enemy presence freezes it.
     */
    public static Result domination(CaptureState state, int onlyTeamId, float weightOnPoint, boolean contested, int captureTicks)
    {
        if (contested)
            return Result.unchanged(state, true);
        if (onlyTeamId == 0 || weightOnPoint <= 0F)
            return new Result(decay(state, captureTicks), Event.NONE);
        return advance(state, onlyTeamId, weightOnPoint, false, captureTicks);
    }

    /**
     * Conquest: both teams may stand on the point; the side with more weight makes progress at
     * the pace of its advantage, and equal weights hold it still.
     */
    public static Result pressure(CaptureState state, int firstTeamId, float firstWeight, int secondTeamId, float secondWeight, int captureTicks)
    {
        if (firstWeight <= 0F && secondWeight <= 0F)
            return new Result(decay(state, captureTicks), Event.NONE);
        boolean contested = firstWeight > 0F && secondWeight > 0F;
        float net = firstWeight - secondWeight;
        if (Math.abs(net) < EQUAL_WEIGHT)
            return Result.unchanged(state, contested);
        return advance(state, net > 0F ? firstTeamId : secondTeamId, Math.abs(net), contested, captureTicks);
    }

    /**
     * Conquest between any number of sides: the side with the most weight on the point makes
     * progress at the pace of its lead over the next strongest one, and a shared lead holds it still.
     *
     * @param weights
     *            weight on the point by side id
     */
    public static Result pressure(CaptureState state, Map<Integer, ? extends Number> weights, int captureTicks)
    {
        int leadingSideId = 0;
        float most = 0F;
        float nextBest = 0F;
        for (Map.Entry<Integer, ? extends Number> entry : weights.entrySet())
        {
            float weight = entry.getValue().floatValue();
            if (weight > most)
            {
                nextBest = most;
                most = weight;
                leadingSideId = entry.getKey();
            }
            else if (weight > nextBest)
                nextBest = weight;
        }
        return pressure(state, leadingSideId, most, 0, nextBest, captureTicks);
    }

    /**
     * The active objective of an Assault round. It follows the Conquest rule, except that
     * defenders outnumbering the attackers on a point they still own restore it at once instead
     * of having to win it back tick by tick.
     */
    public static Result assault(CaptureState state, int attackerTeamId, float attackers, int defenderTeamId, float defenders, int captureTicks)
    {
        if (attackers <= 0F && defenders <= 0F)
            return new Result(decay(state, captureTicks), Event.NONE);
        boolean contested = attackers > 0F && defenders > 0F;
        float net = attackers - defenders;
        if (Math.abs(net) < EQUAL_WEIGHT)
            return Result.unchanged(state, contested);
        if (net < 0F && state.ownerTeamId() == defenderTeamId)
            return Result.of(defenderTeamId, 0, 1F, contested, Event.NONE);
        int active = net > 0F ? attackerTeamId : defenderTeamId;
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

    /** One tick of {@code activeTeamId} working on the point with the given weight. */
    private static Result advance(CaptureState state, int activeTeamId, float weight, boolean contested, int captureTicks)
    {
        int owner = state.ownerTeamId();
        int capturing;
        float progress = state.progress();
        float delta = weight / Math.max(1, captureTicks);
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
