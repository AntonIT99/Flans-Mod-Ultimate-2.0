package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcDriving.*;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.*;

class NpcSteeringPolicyTest
{
    private static final Settings TRACKED = new Settings(true, 4F, 3F, 0.05F, 0.125F, 0.03F, 0.4F, 0.45F, 0F, true, 110F, 10D);
    private static final Settings WHEELED = new Settings(false, 4F, 3F, 0.05F, 0.125F, 0.03F, 0.4F, 0.45F, 0F, true, 110F, 10D);

    @Test
    void defaultsPreserveImmediateTrackedTurnsAndTrafficCurve()
    {
        NpcTuning tuning = new NpcTuning();
        assertEquals(0, tuning.get(Value.STEERING_RESPONSE));
        assertEquals(1, tuning.get(Value.STATIONARY_TURN));
        assertEquals(0, tuning.get(Value.TRAFFIC_STOP_GAP));
        assertEquals(4F, new NpcDriving().drive(situation(0F, 90F, 0D), TRACKED).hullYaw());
        assertEquals(0.5F, NpcDriving.followLimit(4.5D, 6, 0));
    }

    @Test
    void responseBuildsGraduallyAndReversalsUnwindThePreviousRate()
    {
        NpcDriving driving = new NpcDriving();
        float yaw = 0F;
        for (int tick = 1; tick <= 4; tick++)
        {
            float next = driving.drive(situation(yaw, 90F, 0D), TRACKED, 4, true).hullYaw();
            assertEquals(tick, next - yaw, 1E-5F);
            yaw = next;
        }
        assertEquals(3F, driving.drive(situation(yaw, -90F, 0D), TRACKED, 4, true).hullYaw() - yaw, 1E-5F);
        driving.idle(0F, TRACKED);
        assertEquals(1F, driving.drive(situation(0F, 90F, 0D), TRACKED, 4, true).hullYaw());
        driving.reset();
        assertEquals(1F, driving.drive(situation(0F, 90F, 0D), TRACKED, 4, true).hullYaw());
    }

    @Test
    void rollingPermissionAndSpeedLimitsApplyImmediatelyEvenWithResidualSteering()
    {
        NpcDriving driving = new NpcDriving();
        for (int tick = 0; tick < 5; tick++)
            driving.drive(situation(0F, 90F, 1D), TRACKED, 4, true);
        assertEquals(0F, driving.drive(situation(0F, 90F, 0D), TRACKED, 4, false).hullYaw());
        Command rolling = driving.drive(situation(0F, 90F, 0.01D), TRACKED, 4, false);
        assertTrue(rolling.hullYaw() > 0F && rolling.hullYaw() <= Math.toDegrees(0.01D / 3D));
        assertTrue(rolling.throttle() > 0F, "rolling-only tracks must keep moving to turn");
        assertEquals(0F, new NpcDriving().drive(situation(0F, 90F, 0D), WHEELED, 0, true).hullYaw());
    }

    @Test
    void steeringDoesNotOvershootTheTargetAndHandlesYawWrap()
    {
        NpcDriving driving = new NpcDriving();
        for (int tick = 0; tick < 5; tick++)
            driving.drive(situation(0F, 90F, 0D), TRACKED, 4, true);
        assertEquals(0.1F, driving.drive(situation(0F, 0.1F, 0D), TRACKED, 4, true).hullYaw(), 1E-5F);
        assertEquals(-179F, new NpcDriving().drive(situation(179F, -179F, 0D), TRACKED, 0, true).hullYaw());
    }

    @Test
    void customGapChangesOnlyTheBrakingCurveAndFollowingCanStillBeDisabled()
    {
        assertEquals(0F, NpcDriving.followLimit(1D, 6, 10));
        assertEquals(0.5F, NpcDriving.followLimit(3.5D, 6, 10));
        assertEquals(1F, NpcDriving.followLimit(6D, 6, 10));
        assertEquals(0F, NpcDriving.followLimit(-1D, 6, 10));
        assertEquals(0F, NpcDriving.followLimit(6D, 6, 320));
        assertEquals(1F, NpcDriving.followLimit(7D, 6, 320));
        assertEquals(1F, NpcDriving.followLimit(0D, 0, 320));
    }

    @Test
    void policiesRoundTripAndOldSavesKeepDefaults()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.STEERING_RESPONSE, 12);
        tuning.set(Value.STATIONARY_TURN, 0);
        tuning.set(Value.TRAFFIC_STOP_GAP, 15);
        CompoundTag tag = new CompoundTag();
        tuning.save(tag);
        tuning.load(tag.copy());
        assertEquals(12, tuning.get(Value.STEERING_RESPONSE));
        assertEquals(0, tuning.get(Value.STATIONARY_TURN));
        assertEquals(15, tuning.get(Value.TRAFFIC_STOP_GAP));
        tuning.set(Value.STEERING_RESPONSE, 500);
        tuning.set(Value.TRAFFIC_STOP_GAP, -1);
        assertEquals(100, tuning.get(Value.STEERING_RESPONSE));
        assertEquals(0, tuning.get(Value.TRAFFIC_STOP_GAP));
        tuning.load(new CompoundTag());
        assertEquals(0, tuning.get(Value.STEERING_RESPONSE));
        assertEquals(1, tuning.get(Value.STATIONARY_TURN));
    }

    private static Situation situation(float yaw, float wanted, double speed)
    {
        return new Situation(yaw, wanted, 100D, 0F, Double.MAX_VALUE, 4D, speed, 1F, 0D, 0D);
    }
}
