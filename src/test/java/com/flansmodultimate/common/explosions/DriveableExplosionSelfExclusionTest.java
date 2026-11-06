package com.flansmodultimate.common.explosions;

import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.entity.Wheel;
import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.platform.PlatformEvents;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

class DriveableExplosionSelfExclusionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void destructionBlastCannotHitItsOwnHullThroughSeatsOrWheels()
    {
        checkProxyTargets(false);
    }

    @Test
    void explicitlyEnabledSelfDamageStillReachesTheHullOnce()
    {
        checkProxyTargets(true);
    }

    private void checkProxyTargets(boolean canDamageSelf)
    {
        Level level = mock(Level.class);
        when(level.damageSources()).thenReturn(mock(DamageSources.class));
        Vehicle source = mock(Vehicle.class);
        Vehicle neighbour = mock(Vehicle.class);
        when(source.getConfigType()).thenReturn(new VehicleType());
        when(neighbour.getConfigType()).thenReturn(new VehicleType());
        Seat ownSeat = mock(Seat.class);
        Wheel ownWheel = mock(Wheel.class);
        Seat neighbourSeat = mock(Seat.class);
        when(ownSeat.getDriveable()).thenReturn(source);
        when(ownWheel.getDriveable()).thenReturn(source);
        when(neighbourSeat.getDriveable()).thenReturn(neighbour);
        // The query already excludes the source hull; its separately registered proxies remain in range.
        List<Entity> targets = List.of(ownSeat, ownWheel, neighbourSeat, neighbour);
        when(level.getEntities(eq(canDamageSelf ? null : source), any(), any())).thenReturn(new ArrayList<>(targets));
        try (var events = mockStatic(PlatformEvents.class))
        {
            // Construct without running world effects, then exercise the real entity-damage pass.
            events.when(() -> PlatformEvents.onExplosionStart(eq(level), any())).thenReturn(true);
            TestExplosion explosion = new TestExplosion(level, source, canDamageSelf);
            explosion.doHurtEntities();
        }

        verify(source, times(canDamageSelf ? 1 : 0)).resolveExplosionTargets(Vec3.ZERO);
        verify(neighbour).resolveExplosionTargets(Vec3.ZERO);
        verify(source, never()).setDeltaMovement(any());
    }

    private static final class TestExplosion extends FlanExplosion
    {
        private TestExplosion(Level level, Entity source, boolean canDamageSelf)
        {
            super(level, source, null, 0D, 0D, 0D, new Stats(4F, 1F, 6F, new DamageStats(), 0F, 0F, new DamageStats()), false, true, false, 0, 0, canDamageSelf);
        }
    }
}
