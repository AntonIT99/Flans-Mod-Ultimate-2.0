package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcCrewPassengerTest
{
    private static final WeaponMount DRIVER = mount(0, true, Optional.empty());
    private static final WeaponMount GUNNER = mount(1, false, Optional.of(mock(IContentType.class)));
    private static final WeaponMount FIRST_PASSENGER = mount(2, false, Optional.empty());
    private static final WeaponMount SECOND_PASSENGER = mount(3, false, Optional.empty());

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void playersTakeGunnersThenPassengerSeatsWhileOtherRidersStillTakeTheDriverFirst()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        Player first = mock(Player.class);
        Entity mounted = mock(Entity.class);
        Player second = mock(Player.class);
        Player third = mock(Player.class);
        Player fourth = mock(Player.class);
        when(vehicle.getPassengers()).thenReturn(List.of(first, mounted, second, third, fourth));
        when(first.getVehicle()).thenReturn(vehicle);
        when(second.getVehicle()).thenReturn(vehicle);
        try (var states = vehicleWith(vehicle, DRIVER, FIRST_PASSENGER, GUNNER, SECOND_PASSENGER))
        {
            assertEquals(Optional.of(GUNNER), NpcCrew.seatOf(vehicle, first));
            assertEquals(Optional.of(DRIVER), NpcCrew.seatOf(vehicle, mounted));
            assertEquals(Optional.of(FIRST_PASSENGER), NpcCrew.seatOf(vehicle, second));
            assertEquals(Optional.of(SECOND_PASSENGER), NpcCrew.seatOf(vehicle, third));
            assertEquals(Optional.empty(), NpcCrew.seatOf(vehicle, fourth));
            // Only the player in the gunner's seat mans a gun.
            assertEquals(Optional.of(GUNNER), NpcCrew.mannedGun(first));
            assertEquals(Optional.empty(), NpcCrew.mannedGun(second));
        }
    }

    @Test
    void aPlayerFitsWhileAnySeatIncludingTheDriversIsFree()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        Player aboard = mock(Player.class);
        Player boarding = mock(Player.class);
        Entity mounted = mock(Entity.class);
        when(vehicle.getPassengers()).thenReturn(List.of(aboard));
        try (var states = vehicleWith(vehicle, DRIVER, GUNNER, FIRST_PASSENGER))
        {
            assertEquals(Optional.of(true), NpcCrew.canCarry(vehicle, boarding));
        }
        try (var states = vehicleWith(vehicle, DRIVER, GUNNER))
        {
            assertEquals(Optional.of(true), NpcCrew.canCarry(vehicle, boarding));
            assertEquals(Optional.of(true), NpcCrew.canCarry(vehicle, mounted));
            when(vehicle.getPassengers()).thenReturn(List.of(aboard, boarding));
            assertEquals(Optional.of(DRIVER), NpcCrew.seatOf(vehicle, boarding));
            assertEquals(Optional.of(false), NpcCrew.canCarry(vehicle, mounted));
        }
        try (var states = vehicleWith(vehicle))
        {
            assertEquals(Optional.empty(), NpcCrew.canCarry(vehicle, boarding));
        }
    }

    @Test
    void onlyAllyPlayersWhoDoNotSneakAreCarried()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        Player ally = mock(Player.class);
        Player enemy = mock(Player.class);
        Player sneaking = mock(Player.class);
        when(vehicle.isAlive()).thenReturn(true);
        when(vehicle.getPassengers()).thenReturn(List.of());
        when(sneaking.isSecondaryUseActive()).thenReturn(true);
        when(ally.startRiding(vehicle, true)).thenReturn(true);
        try (var states = vehicleWith(vehicle, DRIVER); var drivers = mockStatic(NpcDrivers.class))
        {
            drivers.when(() -> NpcDrivers.ally(vehicle, ally)).thenReturn(true);
            drivers.when(() -> NpcDrivers.ally(vehicle, sneaking)).thenReturn(true);
            assertTrue(NpcCrew.carry(vehicle, ally));
            assertFalse(NpcCrew.carry(vehicle, enemy));
            assertFalse(NpcCrew.carry(vehicle, sneaking));
            verify(enemy, never()).startRiding(any(), anyBoolean());
            verify(sneaking, never()).startRiding(any(), anyBoolean());
        }
    }

    @Test
    void theSeatKeyIncludesTheFreeDriverSeatWhileOthersKeepTheirs()
    {
        EntityNPCInterface vehicle = holderVehicle();
        Player moving = player(vehicle, "moving");
        Player staying = player(vehicle, "staying");
        when(vehicle.getPassengers()).thenReturn(List.of(moving, staying));
        try (var states = vehicleWith(vehicle, DRIVER, GUNNER, FIRST_PASSENGER, SECOND_PASSENGER))
        {
            assertEquals(Optional.of(GUNNER), NpcCrew.seatOf(vehicle, moving));
            // The first passenger seat is taken, so the player moves past it.
            assertTrue(NpcCrew.changeSeat(vehicle, moving));
            assertEquals(Optional.of(SECOND_PASSENGER), NpcCrew.seatOf(vehicle, moving));
            // The other player does not slide into the gunner's seat the first one left.
            assertEquals(Optional.of(FIRST_PASSENGER), NpcCrew.seatOf(vehicle, staying));
            // Past the last seat the player wraps round to the free driver's seat.
            assertTrue(NpcCrew.changeSeat(vehicle, moving));
            assertEquals(Optional.of(DRIVER), NpcCrew.seatOf(vehicle, moving));
            assertEquals(Optional.empty(), NpcCrew.mannedGun(moving));
            assertEquals(Optional.of(FIRST_PASSENGER), NpcCrew.seatOf(vehicle, staying));
            assertTrue(NpcCrew.changeSeat(vehicle, moving));
            assertEquals(Optional.of(GUNNER), NpcCrew.seatOf(vehicle, moving));
            assertEquals(Optional.of(GUNNER), NpcCrew.mannedGun(moving));
        }
        try (var states = vehicleWith(vehicle, DRIVER, GUNNER, FIRST_PASSENGER))
        {
            Entity driver = mock(Entity.class, withSettings().extraInterfaces(NpcCrewHolder.class));
            when(((NpcCrewHolder) driver).wolffsmodnpcsCrewSeat()).thenReturn(DRIVER.seat());
            when(vehicle.getPassengers()).thenReturn(List.of(moving, staying, driver));
            // With every other seat taken there is nowhere to go.
            assertFalse(NpcCrew.changeSeat(vehicle, staying));
            assertEquals(Optional.of(FIRST_PASSENGER), NpcCrew.seatOf(vehicle, staying));
            assertEquals(Optional.of(DRIVER), NpcCrew.seatOf(vehicle, driver));
            assertFalse(NpcCrew.canCarry(vehicle, mock(Player.class)).orElseThrow());
        }
    }

    @Test
    void aPlayerGettingOnAgainNoLongerHoldsTheSeatTheyMovedToBefore()
    {
        EntityNPCInterface vehicle = holderVehicle();
        Player player = player(vehicle, "player");
        when(vehicle.getPassengers()).thenReturn(List.of(player));
        try (var states = vehicleWith(vehicle, DRIVER, GUNNER, FIRST_PASSENGER); var drivers = mockStatic(NpcDrivers.class))
        {
            assertTrue(NpcCrew.changeSeat(vehicle, player));
            assertEquals(Optional.of(FIRST_PASSENGER), NpcCrew.seatOf(vehicle, player));
            when(player.getVehicle()).thenReturn(null);
            drivers.when(() -> NpcDrivers.ally(vehicle, player)).thenReturn(true);
            assertTrue(NpcCrew.carry(vehicle, player));
            assertEquals(Optional.of(GUNNER), NpcCrew.seatOf(vehicle, player));
        }
    }

    /** @return a vehicle that keeps the seats its riders moved to, as Custom NPCs' entity does */
    private static EntityNPCInterface holderVehicle()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcCrewHolder.class));
        AtomicReference<CompoundTag> seats = new AtomicReference<>(new CompoundTag());
        NpcCrewHolder holder = (NpcCrewHolder) vehicle;
        when(holder.wolffsmodnpcsRiderSeats()).thenAnswer(invocation -> seats.get());
        doAnswer(invocation ->
        {
            seats.set(invocation.getArgument(0));
            return null;
        }).when(holder).wolffsmodnpcsRiderSeats(any());
        when(vehicle.isAlive()).thenReturn(true);
        return vehicle;
    }

    private static Player player(EntityNPCInterface vehicle, String id)
    {
        Player player = mock(Player.class);
        when(player.getStringUUID()).thenReturn(id);
        when(player.getVehicle()).thenReturn(vehicle);
        when(player.startRiding(vehicle, true)).thenReturn(true);
        return player;
    }

    private static MockedStatic<NpcVehicleState> vehicleWith(EntityNPCInterface vehicle, WeaponMount... mounts)
    {
        NpcVehicleState state = mock(NpcVehicleState.class);
        when(state.crewSeats(vehicle)).thenReturn(List.of(mounts));
        var states = mockStatic(NpcVehicleState.class);
        states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
        return states;
    }

    private static WeaponMount mount(int seat, boolean driver, Optional<IContentType> gun)
    {
        return new WeaponMount(seat, driver, false, MountAim.REST, MountAim.REST, 1F, 1F, Vec3.ZERO, Vec3.ZERO, gun);
    }
}
