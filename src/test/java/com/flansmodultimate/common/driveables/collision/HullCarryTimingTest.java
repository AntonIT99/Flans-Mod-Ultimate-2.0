package com.flansmodultimate.common.driveables.collision;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HullCarryTimingTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void timePacketsCannotMakeTheCameraReverseOrDoubleItsTurn(boolean hullFirst) throws ReflectiveOperationException
    {
        Harness harness = new Harness();
        // ClientboundSetTimePacket can repeat or rewind game time, independently of entity ticks.
        long[] worldTimes = {100L, 100L, 101L, 100L, 100L, 101L};
        for (long worldTime : worldTimes)
        {
            when(harness.level.getGameTime()).thenReturn(worldTime);
            HullCarry.beginClientTick();
            double previousX = harness.x;
            float previousYaw = harness.yaw;
            harness.tick(hullFirst, 0.5D, 4F, 0.125D);
            assertEquals(previousX, harness.entity.xo, 1.0E-6D);
            for (float partialTick : new float[]{0F, 0.25F, 0.5F, 1F})
            {
                float view = harness.yaw - harness.carry.viewYawLag(harness.entity.tickCount, partialTick);
                assertEquals(previousYaw + 4F * partialTick, view, 1.0E-6F);
                double position = harness.entity.xo + (harness.x - harness.entity.xo) * partialTick;
                assertEquals(previousX + 0.625D * partialTick, position, 1.0E-6D);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void losingContactDoesNotReplayThePreviousTurn(boolean hullFirst) throws ReflectiveOperationException
    {
        Harness harness = new Harness();
        when(harness.level.getGameTime()).thenReturn(100L);
        HullCarry.beginClientTick();
        harness.tick(hullFirst, 0.5D, 4F, 0D);
        HullCarry.beginClientTick();
        harness.startEntityTick();
        assertEquals(harness.x, harness.entity.xo, 1.0E-6D);
        assertEquals(0F, harness.carry.viewYawLag(harness.entity.tickCount, 0F), 1.0E-6F);
    }

    @Test
    void carriesBeforeAndAfterThePlayerTickStillShareOneInterpolationInterval() throws ReflectiveOperationException
    {
        Harness harness = new Harness();
        HullCarry.beginClientTick();
        harness.tick(true, 0.25D, 2F, 0D);
        harness.x += 0.25D;
        harness.yaw += 3F;
        HullCarry.noteCarry(harness.entity, 0.25D, 0D, 0D, 3F);
        assertEquals(0D, harness.entity.xo, 1.0E-6D);
        assertEquals(0.5D, harness.x, 1.0E-6D);
        assertEquals(5F, harness.carry.viewYawLag(harness.entity.tickCount, 0F), 1.0E-6F);
        assertEquals(2.5F, harness.carry.viewYawLag(harness.entity.tickCount, 0.5F), 1.0E-6F);
    }

    private static final class Harness
    {
        private final HullCarry carry = new HullCarry();
        private final Level level = mock(Level.class);
        private final Entity entity = mock(Entity.class, withSettings().extraInterfaces(HullCarriedEntity.class));
        private double x;
        private float yaw;

        @SuppressWarnings("java:S3011") // Mockito bypasses Level's constructor; exercise the client path with its real final side flag.
        private Harness() throws ReflectiveOperationException
        {
            var side = Level.class.getField("isClientSide");
            side.setAccessible(true);
            side.setBoolean(level, true);
            when(entity.level()).thenReturn(level);
            when(entity.getX()).thenAnswer(call -> x);
            when(entity.getYRot()).thenAnswer(call -> yaw);
            when(((HullCarriedEntity) entity).flansmodultimateHullCarry()).thenReturn(carry);
            doCallRealMethod().when(entity).setOldPosAndRot();
        }

        private void tick(boolean hullFirst, double displacement, float turn, double walking)
        {
            if (!hullFirst)
            {
                startEntityTick();
                x += walking;
            }
            x += displacement;
            yaw += turn;
            HullCarry.noteCarry(entity, displacement, 0D, 0D, turn);
            if (hullFirst)
            {
                startEntityTick();
                x += walking;
            }
        }

        private void startEntityTick()
        {
            entity.setOldPosAndRot();
            // EntityMixin invokes this immediately after vanilla stores the old position.
            carry.startTick(entity);
            entity.tickCount++;
        }
    }
}
