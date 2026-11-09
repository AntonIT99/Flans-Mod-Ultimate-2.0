package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.crew.*;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;

import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SquadTransportSeatsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void realVehicleAdapterKeepsDriverAndGunnerSeatsOutOfPassengerCapacity()
    {
        ServerLevel level = mock(ServerLevel.class);
        Entity vehicle = vehicle(level);
        Entity driver = seat(vehicle, 0);
        when(((ISeat) driver).isDriverSeat()).thenReturn(true);
        Entity gunner = seat(vehicle, 1);
        IContentType gun = mock(IContentType.class);
        when(((ISeat) gunner).getGunType()).thenReturn(Optional.of(gun));
        Entity passenger = seat(vehicle, 3);
        Entity occupied = seat(vehicle, 7);
        Entity rider = mock(Entity.class);
        when(((ISeat) occupied).getRiddenByEntity()).thenReturn(rider);
        List<Entity> entities = List.of(driver, gunner, passenger, occupied);
        when(level.getEntities(eq(vehicle), any(AABB.class), any())).thenAnswer(call ->
        {
            Predicate<Entity> filter = call.getArgument(2);
            return entities.stream().filter(filter).toList();
        });
        try (var threats = mockStatic(NpcVehicleThreats.class))
        {
            List<SquadTransportSeats.Seat> seats = SquadTransportSeats.seats(vehicle);
            assertEquals(List.of(3, 7), seats.stream().map(SquadTransportSeats.Seat::index).toList());
            assertTrue(seats.get(0).free());
            assertFalse(seats.get(1).free());
        }
    }

    @Test
    void npcVehicleAdapterUsesAuthoredPassengerSeatsAndActualOccupancy()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        when(vehicle.position()).thenReturn(new Vec3(0D, 64D, 0D));
        NpcCrew.Seat driver = npcSeat(0, CrewPost.DRIVER);
        NpcCrew.Seat gunner = npcSeat(1, CrewPost.GUNNER);
        NpcCrew.Seat passenger = npcSeat(5, CrewPost.PASSENGER);
        NpcVehicleState state = mock(NpcVehicleState.class);
        FlanModelEntity flanModel = mock(FlanModelEntity.class);
        when(state.seatOffset(vehicle, passenger.mount())).thenReturn(Optional.of(new Vec3(1D, 0D, 0D)));
        try (var models = mockStatic(NpcModels.class); var crews = mockStatic(NpcCrew.class); var states = mockStatic(NpcVehicleState.class); var threats = mockStatic(NpcVehicleThreats.class))
        {
            models.when(() -> NpcModels.flan(vehicle)).thenReturn(flanModel);
            crews.when(() -> NpcCrew.seats(vehicle)).thenReturn(List.of(driver, gunner, passenger));
            crews.when(() -> NpcCrew.occupied(vehicle, 5)).thenReturn(true);
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            List<SquadTransportSeats.Seat> seats = SquadTransportSeats.seats(vehicle);
            assertEquals(1, seats.size());
            assertEquals(5, seats.get(0).index());
            assertSame(vehicle, seats.get(0).mount());
            assertFalse(seats.get(0).free());
        }
    }

    @Test
    void exitNeedsLoadedDrySupportedCollisionFreeSpace()
    {
        ServerLevel level = mock(ServerLevel.class);
        Entity vehicle = vehicle(level);
        EntityNPCInterface soldier = mock(EntityNPCInterface.class);
        when(soldier.getBbWidth()).thenReturn(0.6F);
        when(soldier.getBbHeight()).thenReturn(1.8F);
        BlockState support = mock(BlockState.class);
        when(support.isFaceSturdy(eq(level), any(), any())).thenReturn(true);
        when(level.getBlockState(any())).thenReturn(support);
        when(level.getFluidState(any())).thenReturn(Fluids.EMPTY.defaultFluidState());
        BlockHitResult clear = mock(BlockHitResult.class);
        when(clear.getType()).thenReturn(HitResult.Type.MISS);
        when(level.clip(any())).thenReturn(clear);
        try (var threats = mockStatic(NpcVehicleThreats.class))
        {
            assertTrue(SquadTransportSeats.exit(level, vehicle, soldier).isEmpty(), "unloaded chunks are never exit candidates");
            when(level.hasChunkAt(any(BlockPos.class))).thenReturn(true);
            assertTrue(SquadTransportSeats.exit(level, vehicle, soldier).isEmpty(), "blocked exits keep passengers aboard");
            when(level.noCollision(eq(soldier), any(AABB.class))).thenReturn(true);
            assertTrue(SquadTransportSeats.exit(level, vehicle, soldier).isPresent());
            when(level.getFluidState(any())).thenReturn(Fluids.WATER.defaultFluidState());
            assertTrue(SquadTransportSeats.exit(level, vehicle, soldier).isEmpty(), "planned exits do not put passengers into water");
        }
    }

    private static Entity vehicle(ServerLevel level)
    {
        Entity vehicle = mock(Entity.class);
        when(vehicle.level()).thenReturn(level);
        when(vehicle.position()).thenReturn(new Vec3(0D, 64D, 0D));
        when(vehicle.getBoundingBox()).thenReturn(new AABB(-1D, 64D, -1D, 1D, 66D, 1D));
        when(vehicle.getBbWidth()).thenReturn(2F);
        return vehicle;
    }

    private static Entity seat(Entity vehicle, int index)
    {
        Entity entity = mock(Entity.class, withSettings().extraInterfaces(ISeat.class));
        ISeat seat = (ISeat) entity;
        when(seat.getSeatIndex()).thenReturn(index);
        when(seat.getDriveable()).thenReturn(vehicle);
        when(seat.getGunType()).thenReturn(Optional.empty());
        Vec3 vehiclePosition = vehicle.position();
        when(entity.position()).thenReturn(vehiclePosition);
        return entity;
    }

    private static NpcCrew.Seat npcSeat(int index, CrewPost post)
    {
        WeaponMount mount = new WeaponMount(index, post == CrewPost.DRIVER, false, MountAim.REST, MountAim.REST, 1F, 1F, Vec3.ZERO, Vec3.ZERO, Optional.empty());
        return new NpcCrew.Seat(mount, post);
    }
}
