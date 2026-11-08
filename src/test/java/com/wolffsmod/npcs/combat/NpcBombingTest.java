package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.drivers.WeaponAim;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NpcBombingTest
{
    private static final Vec3 ORIGIN = new Vec3(0D, 40D, 0D);
    private static final double GRAVITY = 0.05D;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static Entity carrier(Vec3 velocity)
    {
        Entity carrier = mock(Entity.class);
        Level level = mock(Level.class);
        when(carrier.level()).thenReturn(level);
        when(carrier.position()).thenReturn(ORIGIN);
        when(carrier.getDeltaMovement()).thenReturn(velocity);
        when(level.clip(any())).thenAnswer(call -> {
            Vec3 end = call.<ClipContext>getArgument(0).getTo();
            return BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end));
        });
        return carrier;
    }

    private static LivingEntity target(Vec3 position)
    {
        LivingEntity target = mock(LivingEntity.class);
        when(target.position()).thenReturn(position);
        when(target.getY()).thenReturn(position.y);
        return target;
    }

    private static Vec3 impact(Vec3 velocity)
    {
        return WeaponAim.fall(ORIGIN, velocity, GRAVITY, 1D, 0D).impact();
    }

    @Test
    void waitsForTheCloserTickInsteadOfDroppingAtTheEdgeOfTheWindow()
    {
        Vec3 velocity = new Vec3(3D, 0D, 0D);
        Entity carrier = carrier(velocity);
        LivingEntity target = target(impact(velocity).add(4D, 0D, 0D));
        assertFalse(NpcBombing.solve(carrier, ORIGIN, GRAVITY, 1D, 5D, target, Vec3.ZERO).drop());
        assertTrue(NpcBombing.solve(carrier, ORIGIN.add(velocity), GRAVITY, 1D, 5D, target, Vec3.ZERO).drop());
    }

    @Test
    void keepsAnImperfectDropWhenWaitingWouldLoseTheOnlyOpportunity()
    {
        Vec3 velocity = new Vec3(12D, 0D, 0D);
        Entity carrier = carrier(velocity);
        LivingEntity target = target(impact(velocity).add(4D, 0D, 0D));
        assertTrue(NpcBombing.solve(carrier, ORIGIN, GRAVITY, 1D, 5D, target, Vec3.ZERO).drop());
        assertFalse(NpcBombing.solve(carrier, ORIGIN.add(velocity), GRAVITY, 1D, 5D, target, Vec3.ZERO).drop());
    }

    @Test
    void acceptsCrossTrackErrorAtTheClosestPointOfAnImperfectPass()
    {
        Vec3 velocity = new Vec3(3D, 0D, 0D);
        LivingEntity target = target(impact(velocity).add(1D, 0D, 4D));
        assertTrue(NpcBombing.solve(carrier(velocity), ORIGIN, GRAVITY, 1D, 5D, target, Vec3.ZERO).drop());
    }

    @Test
    void aHeavyBombRetainsItsWiderFallbackWindow()
    {
        Vec3 velocity = new Vec3(3D, 0D, 0D);
        LivingEntity target = target(impact(velocity).add(0D, 0D, 10D));
        Entity carrier = carrier(velocity);
        assertTrue(NpcBombing.solve(carrier, ORIGIN, GRAVITY, 1D, Math.max(5D, NpcBombTargets.releaseRadius(24D)), target, Vec3.ZERO).drop());
        assertFalse(NpcBombing.solve(carrier, ORIGIN, GRAVITY, 1D, Math.max(5D, NpcBombTargets.releaseRadius(6D)), target, Vec3.ZERO).drop());
    }

    @Test
    void comparingTheNextTickAlsoAdvancesTheMovingTarget()
    {
        Vec3 velocity = new Vec3(3D, 0D, 0D);
        // Both move east at the same speed: waiting will not close this four-block error.
        LivingEntity target = target(new Vec3(4D, 0D, 0D));
        assertTrue(NpcBombing.solve(carrier(velocity), ORIGIN, GRAVITY, 1D, 5D, target, velocity).drop());
    }

    @Test
    void terrainInterceptingTheBombBeforeTheTargetInvalidatesTheFlatGroundHit()
    {
        Vec3 velocity = new Vec3(3D, 0D, 0D);
        Entity carrier = carrier(velocity);
        Vec3 obstacle = ORIGIN.add(velocity);
        Level level = carrier.level();
        doReturn(new BlockHitResult(obstacle, Direction.WEST, BlockPos.containing(obstacle), false)).when(level).clip(any());
        assertFalse(NpcBombing.solve(carrier, ORIGIN, GRAVITY, 1D, 5D, target(impact(velocity)), Vec3.ZERO).drop());
    }

    @Test
    void distantApproachesDoNotTraceEveryTrajectorySegmentThroughTheWorld()
    {
        Entity carrier = carrier(new Vec3(3D, 0D, 0D));
        NpcBombing.Solution solution = NpcBombing.solve(carrier, ORIGIN, GRAVITY, 1D, 5D, target(new Vec3(500D, 0D, 0D)), Vec3.ZERO);
        assertFalse(solution.drop());
        assertTrue(solution.release().x < 500D);
        verify(carrier.level(), never()).clip(any());
    }

    @Test
    void eachBayIsCheckedAtItsOwnLaunchPosition()
    {
        Vec3 velocity = new Vec3(3D, 0D, 0D);
        Entity carrier = carrier(velocity);
        LivingEntity target = target(impact(velocity));
        assertTrue(NpcBombing.solve(carrier, ORIGIN, GRAVITY, 1D, 5D, target, Vec3.ZERO).drop());
        assertFalse(NpcBombing.solve(carrier, ORIGIN.add(0D, 0D, 8D), GRAVITY, 1D, 5D, target, Vec3.ZERO).drop());
    }

    @Test
    void steersTheAirframeToCompensateForAnOffsetBombBay()
    {
        Vec3 velocity = new Vec3(3D, 0D, 0D);
        Entity carrier = carrier(velocity);
        LivingEntity target = target(impact(velocity));
        NpcBombing.Solution solution = NpcBombing.solve(carrier, ORIGIN.add(0D, 0D, 8D), GRAVITY, 1D, 5D, target, Vec3.ZERO);
        assertEquals(-8D, solution.release().z, 1.0E-9D);
        assertEquals(0D, solution.release().x, 1.0E-9D);
    }

    @Test
    void doesNotHoldAnAlreadyPreciseOrStationaryDropForAMarginalImprovement()
    {
        assertTrue(NpcBombing.releaseNow(0.8D, 0.2D, 5D));
        assertTrue(NpcBombing.releaseNow(16D, 16D, 5D));
        assertTrue(NpcBombing.releaseNow(16D, Double.POSITIVE_INFINITY, 5D));
        assertFalse(NpcBombing.releaseNow(26D, 16D, 5D));
    }
}
