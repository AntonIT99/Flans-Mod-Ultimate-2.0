package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NpcSeatInteractionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void everySeatCanBeHitAtItsCurrentOffsetFarOutsideTheCentralHitbox()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        NpcVehicleState state = mock(NpcVehicleState.class);
        WeaponMount driver = mock(WeaponMount.class);
        WeaponMount passenger = mock(WeaponMount.class);
        when(vehicle.position()).thenReturn(new Vec3(30D, 4D, 40D));
        when(state.crewSeats(vehicle)).thenReturn(List.of(driver, passenger));
        when(state.seatOffset(vehicle, driver)).thenReturn(Optional.of(Vec3.ZERO));
        when(state.seatOffset(vehicle, passenger)).thenReturn(Optional.of(new Vec3(12D, 2D, 0D)));
        try (var states = mockStatic(NpcVehicleState.class))
        {
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            assertEquals(driver, NpcSeatInteraction.hit(vehicle, new Vec3(30D, 4.5D, 38D), new Vec3(30D, 4.5D, 42D)).orElseThrow().mount());
            assertEquals(passenger, NpcSeatInteraction.hit(vehicle, new Vec3(42D, 6.5D, 38D), new Vec3(42D, 6.5D, 42D)).orElseThrow().mount());
            // A hull/turret rotation moves the target with the rendered seat.
            when(state.seatOffset(vehicle, passenger)).thenReturn(Optional.of(new Vec3(0D, 2D, 12D)));
            assertTrue(NpcSeatInteraction.hit(vehicle, new Vec3(42D, 6.5D, 38D), new Vec3(42D, 6.5D, 42D)).isEmpty());
            assertEquals(passenger, NpcSeatInteraction.hit(vehicle, new Vec3(30D, 6.5D, 50D), new Vec3(30D, 6.5D, 54D)).orElseThrow().mount());
            assertTrue(NpcSeatInteraction.hit(vehicle, new Vec3(30D, 6.5D, 48D), new Vec3(30D, 6.5D, 50D)).isEmpty());
        }
    }

    @Test
    void reachBlocksAndInterveningEntitiesLimitSeatPicking()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        NpcVehicleState state = mock(NpcVehicleState.class);
        WeaponMount near = mock(WeaponMount.class);
        WeaponMount far = mock(WeaponMount.class);
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        Vec3 eye = new Vec3(0D, 0.5D, -2D);
        Vec3 end = eye.add(0D, 0D, 3D);
        when(vehicle.position()).thenReturn(Vec3.ZERO);
        // The nearest hit wins even when it is not first in the definition.
        when(state.crewSeats(vehicle)).thenReturn(List.of(far, near));
        when(state.seatOffset(vehicle, near)).thenReturn(Optional.of(Vec3.ZERO));
        when(state.seatOffset(vehicle, far)).thenReturn(Optional.of(new Vec3(0D, 0D, 2D)));
        when(player.level()).thenReturn(level);
        when(player.getEyePosition()).thenReturn(eye);
        when(player.getLookAngle()).thenReturn(new Vec3(0D, 0D, 1D));
        when(level.clip(any(ClipContext.class))).thenReturn(BlockHitResult.miss(end, Direction.NORTH, BlockPos.containing(end)));
        try (var states = mockStatic(NpcVehicleState.class); var platform = mockStatic(NpcPlatform.class); var projectiles = mockStatic(ProjectileUtil.class))
        {
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            platform.when(() -> NpcPlatform.entityInteractionRange(player)).thenReturn(3D);
            assertEquals(near, NpcSeatInteraction.pick(vehicle, player).orElseThrow().mount());
            platform.when(() -> NpcPlatform.entityInteractionRange(player)).thenReturn(1D);
            Vec3 shortEnd = eye.add(0D, 0D, 1D);
            when(level.clip(any(ClipContext.class))).thenReturn(BlockHitResult.miss(shortEnd, Direction.NORTH, BlockPos.containing(shortEnd)));
            assertTrue(NpcSeatInteraction.pick(vehicle, player).isEmpty());
            platform.when(() -> NpcPlatform.entityInteractionRange(player)).thenReturn(3D);
            when(level.clip(any(ClipContext.class))).thenReturn(new BlockHitResult(shortEnd, Direction.NORTH, BlockPos.containing(shortEnd), false));
            assertTrue(NpcSeatInteraction.pick(vehicle, player).isEmpty());
            when(level.clip(any(ClipContext.class))).thenReturn(BlockHitResult.miss(end, Direction.NORTH, BlockPos.containing(end)));
            Entity blocker = mock(Entity.class);
            projectiles.when(() -> ProjectileUtil.getEntityHitResult(eq(player), eq(eye), any(Vec3.class), any(), any(), anyDouble())).thenReturn(new EntityHitResult(blocker, shortEnd));
            assertTrue(NpcSeatInteraction.pick(vehicle, player).isEmpty());
        }
    }
}
