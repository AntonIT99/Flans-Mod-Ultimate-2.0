package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.DriveableType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriveableRepairsTest
{
    @BeforeAll
    static void bootstrapRegistries()
    {
        SharedConstants.tryDetectVersion();
        try
        {
            Bootstrap.bootStrap();
        }
        catch (ExceptionInInitializerError error)
        {
            // Plain JUnit lacks Forge's NetworkEvent transformer; vanilla registries are initialized first.
            Throwable cause = error;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (!(cause instanceof NoSuchMethodException) || !cause.getMessage().equals("net.minecraftforge.network.NetworkEvent.<init>()"))
                throw error;
        }
    }

    @Test
    void aircraftOnlyReceivePassiveRepairsWhenACarrierActuallySupportsThem()
    {
        Plane plane = mock(Plane.class);
        ServerLevel level = mock(ServerLevel.class);
        DriveableData data = mock(DriveableData.class);
        when(plane.level()).thenReturn(level);
        when(plane.getDriveableData()).thenReturn(data);
        DriveableType type = mock(DriveableType.class);
        when(plane.getConfigType()).thenReturn(type);
        when(plane.getSeats()).thenReturn(new Seat[0]);
        when(plane.getBoundingBox()).thenReturn(new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1));
        DriveablePart part = new DriveablePart(EnumDriveablePart.CORE, new CollisionBox(1000F, 0, 0, 0, 16, 16, 16));
        part.damage(100F, false);
        for (int tick = 0; tick < 100; ++tick)
            part.tick();
        when(data.getParts()).thenReturn(Map.of(EnumDriveablePart.CORE, part));
        when(plane.canRepairPart(EnumDriveablePart.CORE)).thenReturn(true);
        when(plane.repairPart(eq(EnumDriveablePart.CORE), anyFloat())).thenAnswer(invocation -> part.repair(invocation.getArgument(1)) > 0F);
        when(level.getEntitiesOfClass(eq(Driveable.class), any(), any())).thenReturn(List.of());
        DriveableRepairs repairs = new DriveableRepairs();
        for (int tick = 0; tick < 40; ++tick)
            repairs.tick(plane, false, false);
        assertEquals(900F, part.getHealth());
        Driveable carrier = mock(Driveable.class);
        when(level.getEntitiesOfClass(eq(Driveable.class), any(), any())).thenReturn(List.of(carrier));
        for (int tick = 0; tick < 20; ++tick)
            repairs.tick(plane, false, false);
        assertEquals(915F, part.getHealth());
        part.damage(1000F, false);
        repairs.tick(plane, false, false);
        assertTrue(part.isDestroyed());
    }

    @Test
    void unseatedShipCompartmentsUseOverallCrewAndBuoyancyRecoveryScalesWithHp()
    {
        Driveable ship = mock(Driveable.class);
        DriveableData data = mock(DriveableData.class);
        DriveableType type = mock(DriveableType.class);
        ServerLevel level = mock(ServerLevel.class);
        Seat seat = mock(Seat.class);
        SeatInfo info = mock(SeatInfo.class);
        net.minecraft.world.entity.LivingEntity occupant = mock(net.minecraft.world.entity.LivingEntity.class);
        when(ship.level()).thenReturn(level);
        when(ship.getDriveableData()).thenReturn(data);
        when(ship.getConfigType()).thenReturn(type);
        when(type.isEpicShip()).thenReturn(true);
        when(ship.getSeats()).thenReturn(new Seat[]{seat});
        when(seat.getSeatInfo()).thenReturn(info);
        when(info.getPart()).thenReturn(EnumDriveablePart.CORE);
        when(ship.isPartIntact(EnumDriveablePart.CORE)).thenReturn(true);
        when(seat.getRiddenByEntity()).thenReturn(occupant);
        DriveablePart bow = new DriveablePart(EnumDriveablePart.BOW, new CollisionBox(1000F, 0, 0, 0, 16, 16, 16));
        DriveablePart buoyancy = new DriveablePart(EnumDriveablePart.BUOYANCY, new CollisionBox(6000F, 0, 0, 0, 16, 16, 16));
        bow.damage(100F, false);
        buoyancy.damage(1000F, false);
        for (int tick = 0; tick < 100; ++tick)
        {
            bow.tick();
            buoyancy.tick();
        }
        Map<EnumDriveablePart, DriveablePart> parts = Map.of(EnumDriveablePart.BOW, bow, EnumDriveablePart.BUOYANCY, buoyancy);
        when(data.getParts()).thenReturn(parts);
        when(data.getPart(EnumDriveablePart.BUOYANCY)).thenReturn(buoyancy);
        when(ship.canRepairPart(any())).thenReturn(true);
        when(ship.repairPart(any(), anyFloat())).thenAnswer(invocation ->
        {
            EnumDriveablePart part = invocation.getArgument(0);
            return parts.get(part).repair(invocation.getArgument(1)) > 0F;
        });
        DriveableRepairs repairs = new DriveableRepairs();
        for (int tick = 0; tick < 20; ++tick)
            repairs.tick(ship, false, true);
        assertEquals(925F, bow.getHealth());
        assertEquals(5100F, buoyancy.getHealth());
        buoyancy.damage(1F, false);
        for (int tick = 0; tick < 40; ++tick)
            repairs.tick(ship, false, false);
        assertEquals(5099F, buoyancy.getHealth());
    }

    @Test
    void sessionRepairsEverySecondAndNeverRebuildsDestroyedParts()
    {
        Driveable driveable = mock(Driveable.class);
        DriveableData data = mock(DriveableData.class);
        DriveableType type = mock(DriveableType.class);
        ServerLevel level = mock(ServerLevel.class);
        when(driveable.level()).thenReturn(level);
        when(driveable.getDriveableData()).thenReturn(data);
        when(driveable.getConfigType()).thenReturn(type);
        when(driveable.getSeats()).thenReturn(new Seat[0]);
        DriveablePart part = new DriveablePart(EnumDriveablePart.CORE, new CollisionBox(1000F, 0, 0, 0, 16, 16, 16));
        part.damage(100F, false);
        for (int tick = 0; tick < 100; ++tick)
            part.tick();
        when(data.getParts()).thenReturn(Map.of(EnumDriveablePart.CORE, part));
        when(driveable.canRepairPart(EnumDriveablePart.CORE)).thenAnswer(invocation -> !part.isDestroyed() && part.getHealth() < part.getMaxHealth());
        when(driveable.repairPart(eq(EnumDriveablePart.CORE), anyFloat())).thenAnswer(invocation -> part.repair(invocation.getArgument(1)) > 0F);
        DriveableRepairs repairs = new DriveableRepairs();
        for (int tick = 0; tick < 19; ++tick)
            repairs.tick(driveable, true, false);
        assertEquals(900F, part.getHealth());
        repairs.tick(driveable, true, false);
        assertEquals(915F, part.getHealth());
        for (int tick = 0; tick < 20; ++tick)
            repairs.tick(driveable, true, false);
        assertEquals(930F, part.getHealth());
        part.damage(1F, false);
        repairs.tick(driveable, true, false);
        assertEquals(929F, part.getHealth());
        assertEquals(100, part.getRepairDelay());
        for (int tick = 0; tick < 100; ++tick)
            part.tick();
        for (int tick = 0; tick < 20; ++tick)
            repairs.tick(driveable, true, false);
        assertEquals(944F, part.getHealth());
        part.damage(1000F, false);
        for (int tick = 0; tick < 40; ++tick)
            repairs.tick(driveable, true, false);
        assertTrue(part.isDestroyed());
        assertEquals(0F, part.getHealth());
    }
}
