package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.FlansAircraftCrashes;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.teams.BotDriveables;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriveableCrewTest
{
    @Test
    void botGunnersSuppressHandheldFireAndReleaseTheirTriggerWithoutADriver() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        ServerLevel level = mock(ServerLevel.class);
        Entity vehicle = mock(Entity.class);
        Entity seatEntity = mock(Entity.class, withSettings().extraInterfaces(ISeat.class));
        ISeat seat = (ISeat) seatEntity;
        when(npc.level()).thenReturn(level);
        when(npc.getVehicle()).thenReturn(seatEntity);
        when(seat.getDriveable()).thenReturn(vehicle);
        when(seat.getGunType()).thenReturn(Optional.of(mock(IContentType.class)));
        when(vehicle.level()).thenReturn(level);
        when(vehicle.getBoundingBox()).thenReturn(new AABB(0D, 0D, 0D, 3D, 3D, 3D));
        when(level.getEntities(eq(vehicle), any(AABB.class), any())).thenReturn(List.of());
        try (var equipment = mockStatic(NpcEquipment.class); var bots = mockStatic(BotDriveables.class))
        {
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.DRIVE_DRIVEABLES)).thenReturn(true);
            bots.when(() -> BotDriveables.mayUse(npc, seat)).thenReturn(true);
            assertTrue(DriveableCrew.crewSeat(npc).isPresent());
            assertTrue(NpcDrivers.holdsFire(npc));
            NpcDriver driver = new NpcDriver();
            for (int tick = 0; tick < DriveableCrew.DRIVERLESS_TICKS; tick++)
                assertTrue(DriveableCrew.tick(npc, seat, vehicle, driver));
            assertFalse(DriveableCrew.tick(npc, seat, vehicle, driver));
            verify(seat, atLeastOnce()).setAutomatedTriggers(false, false);
            bots.when(() -> BotDriveables.mayUse(npc, seat)).thenReturn(false);
            assertFalse(DriveableCrew.tick(npc, seat, vehicle, new NpcDriver()));
        }
    }

    @Test
    void gunnersLeaveAnAircraftAboutToBeLostWhileThereIsHeightToJump() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        ServerLevel level = mock(ServerLevel.class);
        Entity plane = mock(Entity.class);
        Entity seatEntity = mock(Entity.class, withSettings().extraInterfaces(ISeat.class));
        ISeat seat = (ISeat) seatEntity;
        IDriveableType type = mock(IDriveableType.class);
        when(type.getDriveableKind()).thenReturn(DriveableKind.PLANE);
        when(seat.getDriveableType()).thenReturn(Optional.of(type));
        when(npc.level()).thenReturn(level);
        when(npc.getY()).thenReturn(100D);
        when(level.getHeight(any(), anyInt(), anyInt())).thenReturn(64);
        try (var types = mockStatic(FlansEntityTypes.class); var crashes = mockStatic(FlansAircraftCrashes.class))
        {
            crashes.when(() -> FlansAircraftCrashes.crashMode(plane)).thenReturn(Optional.empty());
            types.when(() -> FlansEntityTypes.getHealthShare(plane)).thenReturn(OptionalDouble.of(0.5D));
            assertFalse(NpcDrivers.abandons(npc, seat, plane), "an aircraft still worth flying keeps its gunners");
            types.when(() -> FlansEntityTypes.getHealthShare(plane)).thenReturn(OptionalDouble.of(0.1D));
            assertTrue(NpcDrivers.abandons(npc, seat, plane), "one about to be lost is left while there is height");
            when(npc.getY()).thenReturn(66D);
            assertFalse(NpcDrivers.abandons(npc, seat, plane), "too low for a parachute to open");
            when(npc.getY()).thenReturn(100D);
            when(type.getDriveableKind()).thenReturn(DriveableKind.VEHICLE);
            assertFalse(NpcDrivers.abandons(npc, seat, plane), "a ground vehicle's crew ride it out");
        }
    }
}
