package com.flansmodultimate.common.driveables;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;

import java.util.Arrays;

/**
 * Client-side prediction for the driveable the local player is driving.
 *
 * <p>The driver's client simulates each of its own input steps at once with the
 * movement code the server runs, instead of waiting a round trip to see the
 * server's result. The server stays authoritative: every tick it reports its state
 * with the last input step it applied. This class matches each report to the
 * client's own prediction for that step and turns any disagreement into a
 * correction. Velocity and throttle take the correction at once; position and
 * attitude blend it in over a few ticks so it is not seen as a jump. A large
 * disagreement, such as a teleport, resynchronises immediately.</p>
 *
 * <p>The server applies whichever input arrived last once per tick, so the step it
 * acknowledges jitters by a tick or two against its own tick count. Reports are
 * therefore matched through a fixed step offset, anchored on the median of the first
 * reports. Held input moves the driveable the same whichever step a report is paired
 * with, so a fixed offset only has to be about right: one that followed the jitter
 * would move the comparison by a whole step of travel each time it changed, which
 * shows as the driveable shaking at speed. The offset is re-anchored only when the
 * latency has clearly changed for good.</p>
 *
 * <p>Pure bookkeeping that never touches the entity or the world. Reconciling costs
 * one history lookup: nothing is re-simulated.</p>
 */
public final class DriveablePrediction
{
    /** Predicted steps kept for matching; 3.2 seconds, far beyond any usable latency. */
    static final int HISTORY_SIZE = 64;
    /** Reports the step offset is the median of. */
    static final int OFFSET_WINDOW = 15;
    /** Reports needed before the step offset is trusted; fewer could match the wrong step. */
    static final int MIN_OFFSET_SAMPLES = 5;
    /** The median must lie at least this many steps from the anchor to re-anchor it... */
    static final int REANCHOR_STEPS = 2;
    /** ...for this many consecutive reports: a lasting latency change, not jitter. */
    static final int REANCHOR_REPORTS = 40;
    /** Share of the outstanding position and attitude correction applied each tick. */
    static final float BLEND_PER_TICK = 0.3F;
    /** A disagreement at least this far, in blocks, resynchronises instead of blending. */
    static final double RESYNC_DISTANCE = 4D;
    /** ...or this many ticks of travel at the server's speed, whichever is further. */
    static final double RESYNC_TRAVEL_TICKS = 4D;
    /** A disagreement in any angle of at least this many degrees resynchronises. */
    static final float RESYNC_ANGLE = 45F;
    /** Consecutive unmatched reports after which the prediction resynchronises. */
    static final int MAX_UNMATCHED_REPORTS = 40;
    /** The velocity error is extrapolated over at most this many steps. */
    static final int MAX_EXTRAPOLATED_STEPS = 10;
    private static final double POSITION_EPSILON = 1.0E-3D;
    private static final double VELOCITY_EPSILON = 1.0E-4D;
    private static final float ANGLE_EPSILON = 1.0E-2F;
    private static final float THROTTLE_EPSILON = 1.0E-4F;

    /** One input step, as sent to the server. */
    public record Frame(int sequence, int inputMask, float flightPitch, float flightRoll, boolean mouseControl) {}

    /** The movement state compared between client and server. Angles are in degrees. */
    public record State(double x, double y, double z, double vx, double vy, double vz,
                        float yaw, float pitch, float roll, float throttle)
    {
        boolean isFinite()
        {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Double.isFinite(vx)
                && Double.isFinite(vy) && Double.isFinite(vz) && Float.isFinite(yaw) && Float.isFinite(pitch)
                && Float.isFinite(roll) && Float.isFinite(throttle);
        }
    }

    /** Position and attitude to add to the live state this tick. */
    public record Blend(double x, double y, double z, float yaw, float pitch, float roll)
    {
        public static final Blend NONE = new Blend(0D, 0D, 0D, 0F, 0F, 0F);
    }

    public enum Outcome
    {
        /** No prediction exists for the reported step yet, or no longer. */
        UNMATCHED,
        /** The prediction agreed with the server. */
        AGREED,
        /** Velocity and throttle deltas are to be applied now; position and attitude blend in. */
        CORRECTED,
        /** The live state is to be replaced by the server's. */
        RESYNC
    }

    /**
     * What to do with a report. {@code vx}, {@code vy}, {@code vz} and {@code throttle} are the
     * deltas a {@link Outcome#CORRECTED} report applies at once; {@code resyncTo} is the state a
     * {@link Outcome#RESYNC} report replaces the live one with, carried forward to the live step.
     */
    public record Reconciliation(Outcome outcome, double vx, double vy, double vz, float throttle,
                                 @Nullable State resyncTo)
    {
        static final Reconciliation UNMATCHED = new Reconciliation(Outcome.UNMATCHED, 0D, 0D, 0D, 0F, null);
        static final Reconciliation AGREED = new Reconciliation(Outcome.AGREED, 0D, 0D, 0D, 0F, null);
    }

    private final int[] historySteps = new int[HISTORY_SIZE];
    private final State[] history = new State[HISTORY_SIZE];
    private final int[] offsetSamples = new int[OFFSET_WINDOW];
    private final int[] offsetScratch = new int[OFFSET_WINDOW];
    private int offsetCount;
    private int offsetCursor;
    private int activeOffset;
    private int lastAcknowledged;
    private boolean anyAcknowledged;
    private boolean offsetAnchored;
    private int offsetDisagreements;
    private boolean anyRecorded;
    private int latestStep;
    private int unmatchedReports;

    // Correction still to be blended into the live position and attitude.
    private double pendingX, pendingY, pendingZ;
    private float pendingYaw, pendingPitch, pendingRoll;

    @Nullable private Frame queued;
    @Nullable private Frame current;
    private int previousInputMask;

    /** Queues the input for the next step. A newer frame replaces one not yet stepped. */
    public void submit(Frame frame)
    {
        queued = frame;
    }

    /**
     * Takes the queued input for this tick's step. {@code null} means the driver
     * sent nothing since the last step, so prediction should end.
     */
    @Nullable
    public Frame nextFrame()
    {
        previousInputMask = current == null ? 0 : current.inputMask();
        current = queued;
        queued = null;
        return current;
    }

    /** The input of the step being simulated, or {@code null} outside one. */
    @Nullable
    public Frame currentFrame()
    {
        return current;
    }

    /** Controls pressed since the previous step. */
    public int risingInputs()
    {
        return current == null ? 0 : current.inputMask() & ~previousInputMask;
    }

    /**
     * Stores the live state after simulating {@code step}. It is kept as the state
     * the live one is still being corrected towards, so a report is never compared
     * against a correction that merely has not finished blending in.
     */
    public void record(int step, State live)
    {
        State target = new State(live.x() + pendingX, live.y() + pendingY, live.z() + pendingZ,
            live.vx(), live.vy(), live.vz(), Mth.wrapDegrees(live.yaw() + pendingYaw),
            live.pitch() + pendingPitch, Mth.wrapDegrees(live.roll() + pendingRoll), live.throttle());
        int slot = slot(step);
        historySteps[slot] = step;
        history[slot] = target;
        latestStep = step;
        anyRecorded = true;
    }

    /** Matches a server report against the prediction for the same input step. */
    public Reconciliation reconcile(int serverStep, int acknowledgedStep, State server)
    {
        if (!server.isFinite())
            return Reconciliation.UNMATCHED;
        // A report whose step was already acknowledged only repeats an old input and
        // would skew the offset, so only reports that applied a new input are sampled.
        // The first acknowledgement may predate the prediction, so it only starts the count.
        if (anyAcknowledged && acknowledgedStep != lastAcknowledged)
            addOffsetSample(acknowledgedStep - serverStep);
        lastAcknowledged = acknowledgedStep;
        anyAcknowledged = true;
        if (offsetCount < MIN_OFFSET_SAMPLES)
            return Reconciliation.UNMATCHED;
        int step = serverStep + steadyOffset();
        State predicted = predictionFor(step);
        if (predicted == null)
            return ++unmatchedReports >= MAX_UNMATCHED_REPORTS ? resync(server, 0) : Reconciliation.UNMATCHED;
        unmatchedReports = 0;

        double dx = server.x() - predicted.x();
        double dy = server.y() - predicted.y();
        double dz = server.z() - predicted.z();
        double dvx = server.vx() - predicted.vx();
        double dvy = server.vy() - predicted.vy();
        double dvz = server.vz() - predicted.vz();
        float dYaw = Mth.wrapDegrees(server.yaw() - predicted.yaw());
        float dPitch = server.pitch() - predicted.pitch();
        float dRoll = Mth.wrapDegrees(server.roll() - predicted.roll());
        float dThrottle = server.throttle() - predicted.throttle();

        double speed = Math.sqrt(server.vx() * server.vx() + server.vy() * server.vy() + server.vz() * server.vz());
        double resyncDistance = Math.max(RESYNC_DISTANCE, speed * RESYNC_TRAVEL_TICKS);
        if (dx * dx + dy * dy + dz * dz >= resyncDistance * resyncDistance
            || Math.abs(dYaw) >= RESYNC_ANGLE || Math.abs(dPitch) >= RESYNC_ANGLE || Math.abs(dRoll) >= RESYNC_ANGLE)
            return resync(server, Math.min(latestStep - step, MAX_EXTRAPOLATED_STEPS));
        if (Math.abs(dx) < POSITION_EPSILON && Math.abs(dy) < POSITION_EPSILON && Math.abs(dz) < POSITION_EPSILON
            && Math.abs(dvx) < VELOCITY_EPSILON && Math.abs(dvy) < VELOCITY_EPSILON && Math.abs(dvz) < VELOCITY_EPSILON
            && Math.abs(dYaw) < ANGLE_EPSILON && Math.abs(dPitch) < ANGLE_EPSILON && Math.abs(dRoll) < ANGLE_EPSILON
            && Math.abs(dThrottle) < THROTTLE_EPSILON)
            return Reconciliation.AGREED;

        // Every later prediction started from the wrong state too. Shift each by the
        // error, carrying the velocity error forward over the steps since, so the
        // next report is compared against what the correction will make of it.
        for (int later = step; later - latestStep <= 0; later++)
        {
            int slot = slot(later);
            State stored = history[slot];
            if (stored == null || historySteps[slot] != later)
                continue;
            int elapsed = Math.min(later - step, MAX_EXTRAPOLATED_STEPS);
            history[slot] = new State(stored.x() + dx + dvx * elapsed, stored.y() + dy + dvy * elapsed,
                stored.z() + dz + dvz * elapsed, stored.vx() + dvx, stored.vy() + dvy, stored.vz() + dvz,
                Mth.wrapDegrees(stored.yaw() + dYaw), stored.pitch() + dPitch, Mth.wrapDegrees(stored.roll() + dRoll),
                stored.throttle() + dThrottle);
        }
        int elapsed = Math.min(latestStep - step, MAX_EXTRAPOLATED_STEPS);
        pendingX += dx + dvx * elapsed;
        pendingY += dy + dvy * elapsed;
        pendingZ += dz + dvz * elapsed;
        pendingYaw = Mth.wrapDegrees(pendingYaw + dYaw);
        pendingPitch += dPitch;
        pendingRoll = Mth.wrapDegrees(pendingRoll + dRoll);
        return new Reconciliation(Outcome.CORRECTED, dvx, dvy, dvz, dThrottle, null);
    }

    /** The share of the outstanding correction to apply to the live state this tick. */
    public Blend drainBlend()
    {
        if (pendingX == 0D && pendingY == 0D && pendingZ == 0D && pendingYaw == 0F && pendingPitch == 0F
            && pendingRoll == 0F)
            return Blend.NONE;
        boolean small = Math.abs(pendingX) < POSITION_EPSILON && Math.abs(pendingY) < POSITION_EPSILON
            && Math.abs(pendingZ) < POSITION_EPSILON && Math.abs(pendingYaw) < ANGLE_EPSILON
            && Math.abs(pendingPitch) < ANGLE_EPSILON && Math.abs(pendingRoll) < ANGLE_EPSILON;
        float share = small ? 1F : BLEND_PER_TICK;
        Blend blend = new Blend(pendingX * share, pendingY * share, pendingZ * share,
            pendingYaw * share, pendingPitch * share, pendingRoll * share);
        pendingX -= blend.x();
        pendingY -= blend.y();
        pendingZ -= blend.z();
        pendingYaw -= blend.yaw();
        pendingPitch -= blend.pitch();
        pendingRoll -= blend.roll();
        return blend;
    }

    private Reconciliation resync(State server, int elapsed)
    {
        Arrays.fill(history, null);
        anyRecorded = false;
        offsetCount = 0;
        offsetCursor = 0;
        offsetAnchored = false;
        anyAcknowledged = false;
        offsetDisagreements = 0;
        unmatchedReports = 0;
        pendingX = pendingY = pendingZ = 0D;
        pendingYaw = pendingPitch = pendingRoll = 0F;
        State carried = new State(server.x() + server.vx() * elapsed, server.y() + server.vy() * elapsed,
            server.z() + server.vz() * elapsed, server.vx(), server.vy(), server.vz(),
            server.yaw(), server.pitch(), server.roll(), server.throttle());
        return new Reconciliation(Outcome.RESYNC, 0D, 0D, 0D, 0F, carried);
    }

    @Nullable
    private State predictionFor(int step)
    {
        if (!anyRecorded || step - latestStep > 0 || latestStep - step >= HISTORY_SIZE)
            return null;
        int slot = slot(step);
        return historySteps[slot] == step ? history[slot] : null;
    }

    private void addOffsetSample(int offset)
    {
        offsetSamples[offsetCursor] = offset;
        offsetCursor = (offsetCursor + 1) % OFFSET_WINDOW;
        offsetCount = Math.min(offsetCount + 1, OFFSET_WINDOW);
    }

    private int steadyOffset()
    {
        int median = medianOffset();
        if (!offsetAnchored)
        {
            activeOffset = median;
            offsetAnchored = true;
            offsetDisagreements = 0;
        }
        else if (Math.abs(median - activeOffset) < REANCHOR_STEPS)
            offsetDisagreements = 0;
        else if (++offsetDisagreements >= REANCHOR_REPORTS)
        {
            activeOffset = median;
            offsetDisagreements = 0;
        }
        return activeOffset;
    }

    private int medianOffset()
    {
        System.arraycopy(offsetSamples, 0, offsetScratch, 0, offsetCount);
        Arrays.sort(offsetScratch, 0, offsetCount);
        return offsetScratch[offsetCount / 2];
    }

    private static int slot(int step)
    {
        return Math.floorMod(step, HISTORY_SIZE);
    }
}
