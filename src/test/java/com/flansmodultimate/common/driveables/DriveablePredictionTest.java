package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.DriveablePrediction.Outcome;
import com.flansmodultimate.common.driveables.DriveablePrediction.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveablePredictionTest
{
    private static final double EPSILON = 1.0E-6D;
    /** Client input steps run this far ahead of the server's tick count. */
    private static final int OFFSET = 100;

    @Test
    void framesAreSteppedOnceAndReportRisingControls()
    {
        DriveablePrediction prediction = new DriveablePrediction();
        prediction.submit(frame(1, DriveableInput.FORWARD));
        assertEquals(1, prediction.nextFrame().sequence());
        assertEquals(DriveableInput.FORWARD, prediction.risingInputs());
        prediction.submit(frame(2, DriveableInput.FORWARD | DriveableInput.TRIM));
        prediction.nextFrame();
        assertEquals(DriveableInput.TRIM, prediction.risingInputs());
        assertEquals(DriveableInput.FORWARD | DriveableInput.TRIM, prediction.currentFrame().inputMask());
        assertNull(prediction.nextFrame(), "A tick without new input ends the prediction");
    }

    @Test
    void reportsAreOnlyMatchedOnceTheStepOffsetIsKnown()
    {
        DriveablePrediction prediction = driving(OFFSET, OFFSET + 40);
        // The first acknowledgement only starts the count; it may predate the prediction.
        for (int report = 0; report < DriveablePrediction.MIN_OFFSET_SAMPLES; report++)
            assertEquals(Outcome.UNMATCHED, report(prediction, 10 + report, straight(10 + report + OFFSET)).outcome());
        assertEquals(Outcome.AGREED, report(prediction, 20, straight(20 + OFFSET)).outcome());
    }

    @Test
    void anAgreeingServerCausesNoCorrection()
    {
        DriveablePrediction prediction = warmedUp();
        assertEquals(Outcome.AGREED, report(prediction, 30, straight(30 + OFFSET)).outcome());
        assertSame(DriveablePrediction.Blend.NONE, prediction.drainBlend());
    }

    @Test
    void acknowledgementJitterDoesNotCauseCorrections()
    {
        DriveablePrediction prediction = warmedUp();
        // The server applies whichever input arrived last, so the acknowledged step
        // runs a tick ahead or behind its tick count now and then.
        int[] jitter = {0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1};
        for (int index = 0; index < jitter.length; index++)
        {
            int serverStep = 25 + index;
            DriveablePrediction.Reconciliation result = prediction.reconcile(serverStep,
                serverStep + OFFSET + jitter[index], straight(serverStep + OFFSET));
            assertEquals(Outcome.AGREED, result.outcome(), "report " + index);
        }
    }

    @Test
    void aSmallDisagreementBlendsInWithoutBeingCountedTwice()
    {
        DriveablePrediction prediction = warmedUp();
        State server = shifted(straight(30 + OFFSET), 0.5D, 0F);
        DriveablePrediction.Reconciliation result = report(prediction, 30, server);
        assertEquals(Outcome.CORRECTED, result.outcome());
        assertEquals(0D, result.vx(), EPSILON, "Only the position disagreed");

        double applied = 0D;
        DriveablePrediction.Blend first = prediction.drainBlend();
        assertEquals(0.5D * DriveablePrediction.BLEND_PER_TICK, first.x(), EPSILON);
        applied += first.x();
        // The next step is recorded before the rest has blended in, and the next
        // report agrees with the corrected path: nothing new to correct.
        prediction.record(41 + OFFSET, shifted(straight(41 + OFFSET), applied, 0F));
        assertEquals(Outcome.AGREED, report(prediction, 31, shifted(straight(31 + OFFSET), 0.5D, 0F)).outcome());
        for (int tick = 0; tick < 60; tick++)
            applied += prediction.drainBlend().x();
        assertEquals(0.5D, applied, EPSILON);
    }

    @Test
    void aVelocityErrorIsCarriedForwardToTheLiveStep()
    {
        DriveablePrediction prediction = warmedUp();
        // The server reports step 130 moving 0.2 faster than predicted. The live
        // state is ten steps later, so it is also two blocks further behind.
        State reported = straight(30 + OFFSET);
        State server = new State(reported.x(), reported.y(), reported.z(), reported.vx() + 0.2D, 0D, 0D,
            reported.yaw(), reported.pitch(), reported.roll(), reported.throttle());
        DriveablePrediction.Reconciliation result = report(prediction, 30, server);
        assertEquals(Outcome.CORRECTED, result.outcome());
        assertEquals(0.2D, result.vx(), EPSILON);
        double pending = 0D;
        for (int tick = 0; tick < 80; tick++)
            pending += prediction.drainBlend().x();
        assertEquals(0.2D * 10, pending, EPSILON);
    }

    @Test
    void attitudeCorrectionsWrapAcrossTheYawSeam()
    {
        DriveablePrediction prediction = new DriveablePrediction();
        for (int step = OFFSET; step <= OFFSET + 40; step++)
            prediction.record(step, withYaw(straight(step), 179F));
        for (int serverStep = 20; serverStep <= 20 + DriveablePrediction.MIN_OFFSET_SAMPLES; serverStep++)
            report(prediction, serverStep, withYaw(straight(serverStep + OFFSET), 179F));
        DriveablePrediction.Reconciliation result = report(prediction, 30, withYaw(straight(30 + OFFSET), -179F));
        assertEquals(Outcome.CORRECTED, result.outcome());
        float yaw = 0F;
        for (int tick = 0; tick < 80; tick++)
            yaw += prediction.drainBlend().yaw();
        assertEquals(2F, yaw, 1.0E-4F, "Two degrees across the seam, not 358 back");
    }

    @Test
    void aLargeDisagreementResynchronisesToTheServerCarriedForward()
    {
        DriveablePrediction prediction = warmedUp();
        State teleported = new State(500D, 70D, -20D, 1D, 0D, 0D, 90F, 0F, 0F, 0.5F);
        DriveablePrediction.Reconciliation result = report(prediction, 30, teleported);
        assertEquals(Outcome.RESYNC, result.outcome());
        // Ten steps separate the reported step from the live one.
        assertEquals(510D, result.resyncTo().x(), EPSILON);
        assertSame(DriveablePrediction.Blend.NONE, prediction.drainBlend());
        assertEquals(Outcome.UNMATCHED, report(prediction, 31, straight(31 + OFFSET)).outcome(),
            "History and offset start over after a resync");
    }

    @Test
    void aLargeAttitudeDisagreementResynchronises()
    {
        DriveablePrediction prediction = warmedUp();
        assertEquals(Outcome.RESYNC, report(prediction, 30,
            withYaw(straight(30 + OFFSET), DriveablePrediction.RESYNC_ANGLE + 1F)).outcome());
    }

    @Test
    void aPredictionThatNeverMatchesEventuallyResynchronises()
    {
        DriveablePrediction prediction = driving(OFFSET, OFFSET + 40);
        // Every report is for a step the client never predicted, far in its future.
        int reports = DriveablePrediction.MIN_OFFSET_SAMPLES + DriveablePrediction.MAX_UNMATCHED_REPORTS;
        for (int report = 0; report < reports - 1; report++)
            assertEquals(Outcome.UNMATCHED, prediction.reconcile(report, report + OFFSET + 500, straight(0)).outcome());
        assertEquals(Outcome.RESYNC, prediction.reconcile(reports, reports + OFFSET + 500, straight(0)).outcome());
    }

    @Test
    void nonFiniteReportsAreIgnored()
    {
        DriveablePrediction prediction = warmedUp();
        State broken = new State(Double.NaN, 0D, 0D, 0D, 0D, 0D, 0F, 0F, 0F, 0F);
        assertEquals(Outcome.UNMATCHED, report(prediction, 30, broken).outcome());
    }

    /** Client steps OFFSET..OFFSET + 40 along x at one block per step; server ticks 0..40 match them. */
    private static DriveablePrediction warmedUp()
    {
        DriveablePrediction prediction = driving(OFFSET, OFFSET + 40);
        warm(prediction);
        return prediction;
    }

    private static void warm(DriveablePrediction prediction)
    {
        for (int serverStep = 20; serverStep <= 20 + DriveablePrediction.MIN_OFFSET_SAMPLES; serverStep++)
            report(prediction, serverStep, straight(serverStep + OFFSET));
    }

    private static DriveablePrediction driving(int firstStep, int lastStep)
    {
        DriveablePrediction prediction = new DriveablePrediction();
        for (int step = firstStep; step <= lastStep; step++)
            prediction.record(step, straight(step));
        return prediction;
    }

    private static DriveablePrediction.Reconciliation report(DriveablePrediction prediction, int serverStep, State server)
    {
        return prediction.reconcile(serverStep, serverStep + OFFSET, server);
    }

    private static State straight(int step)
    {
        return new State(step, 64D, 0D, 1D, 0D, 0D, 0F, 0F, 0F, 0.5F);
    }

    private static State shifted(State state, double dx, float dYaw)
    {
        return new State(state.x() + dx, state.y(), state.z(), state.vx(), state.vy(), state.vz(),
            state.yaw() + dYaw, state.pitch(), state.roll(), state.throttle());
    }

    private static State withYaw(State state, float yaw)
    {
        return new State(state.x(), state.y(), state.z(), state.vx(), state.vy(), state.vz(),
            yaw, state.pitch(), state.roll(), state.throttle());
    }

    private static DriveablePrediction.Frame frame(int sequence, int mask)
    {
        return new DriveablePrediction.Frame(sequence, mask, 0F, 0F, false);
    }
}
