package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcCrewRecruitmentTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void recruiterUsesTheActualSeatIdBeforeItsLegacyRoleDefault()
    {
        NpcCrewSettings settings = new NpcCrewSettings();
        CrewSource fallback = new CrewSource(CrewSource.Kind.CLONE, 2, "Passenger", "", "");
        CrewSource override = new CrewSource(CrewSource.Kind.CLASS, 1, "", "team", "crew", CrewSource.RANDOM_SKIN);
        settings.setSource(CrewPost.PASSENGER, fallback);
        settings.setSource(7, override);
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        try (var crew = mockStatic(NpcCrew.class))
        {
            crew.when(() -> NpcCrew.recruiter(settings)).thenCallRealMethod();
            NpcCrew.Recruiter recruiter = NpcCrew.recruiter(settings);
            recruiter.recruit(level, vehicle, seat(3));
            recruiter.recruit(level, vehicle, seat(7));
            crew.verify(() -> NpcCrew.recruit(level, vehicle, fallback));
            crew.verify(() -> NpcCrew.recruit(level, vehicle, override));
        }
    }

    @Test
    void crewNowFillsOnlyEmptySeatsAndDeletesRecruitsThatCannotBoard()
    {
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        Entity occupant = mock(Entity.class, withSettings().extraInterfaces(NpcCrewHolder.class));
        EntityNPCInterface recruit = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcCrewHolder.class));
        EntityNPCInterface rejected = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcCrewHolder.class));
        when(vehicle.isAlive()).thenReturn(true);
        when(vehicle.getPassengers()).thenReturn(List.of(occupant));
        NpcCrew.Seat occupied = seat(0);
        NpcCrew.Seat empty = seat(3);
        NpcCrew.Seat blocked = seat(7);
        when(((NpcCrewHolder) occupant).wolffsmodnpcsCrewSeat()).thenReturn(0);
        when(recruit.startRiding(vehicle, true)).thenReturn(true);
        NpcVehicleState state = mock(NpcVehicleState.class);
        when(state.crewSeats(vehicle)).thenReturn(List.of(occupied.mount(), empty.mount(), blocked.mount()));
        NpcCrew.Recruiter recruiter = mock(NpcCrew.Recruiter.class);
        when(recruiter.recruit(level, vehicle, empty)).thenReturn(Optional.of(recruit));
        when(recruiter.recruit(level, vehicle, blocked)).thenReturn(Optional.of(rejected));
        try (var states = mockStatic(NpcVehicleState.class))
        {
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            assertEquals(1, NpcCrew.fill(level, vehicle, recruiter));
            verify(recruiter, never()).recruit(level, vehicle, occupied);
            verify(recruit, never()).delete();
            verify(rejected).delete();
        }
    }

    @Test
    void onlyAssignedCrewInHiddenSeatsAreHiddenAndLeavingMakesThemVisible()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        EntityNPCInterface crew = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcCrewHolder.class));
        EntityNPCInterface otherCrew = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcCrewHolder.class));
        Entity ordinaryRider = mock(Entity.class);
        when(((NpcCrewHolder) crew).wolffsmodnpcsCrewSeat()).thenReturn(3);
        when(((NpcCrewHolder) otherCrew).wolffsmodnpcsCrewSeat()).thenReturn(7);
        when(crew.getVehicle()).thenReturn(vehicle);
        when(otherCrew.getVehicle()).thenReturn(vehicle);
        when(ordinaryRider.getVehicle()).thenReturn(vehicle);
        when(vehicle.getPassengers()).thenReturn(List.of(crew, otherCrew));
        NpcCrewSettings settings = new NpcCrewSettings();
        vehicle.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) vehicle.stats).wolffsmodnpcsCrew()).thenReturn(settings);
        NpcVehicleState state = mock(NpcVehicleState.class);
        when(state.crewSeats(vehicle)).thenReturn(List.of(seat(3).mount(), seat(7).mount()));
        try (var states = mockStatic(NpcVehicleState.class))
        {
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            assertFalse(NpcCrew.hiddenAboard(crew));
            settings.setSeatVisible(3, false);
            assertTrue(NpcCrew.hiddenAboard(crew));
            assertFalse(NpcCrew.hiddenAboard(otherCrew));
            assertFalse(NpcCrew.hiddenAboard(ordinaryRider));
            when(crew.getVehicle()).thenReturn(null);
            assertFalse(NpcCrew.hiddenAboard(crew));
            // Visibility is derived from the current seat, never written to the crew member's invisibility flag.
            verify(crew, never()).setInvisible(anyBoolean());
            verify(crew, never()).discard();
        }
    }

    private static NpcCrew.Seat seat(int index)
    {
        return new NpcCrew.Seat(new WeaponMount(index, false, false, MountAim.REST, MountAim.REST, 1F, 1F, Vec3.ZERO, Vec3.ZERO, Optional.empty()), CrewPost.PASSENGER);
    }
}
