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
        repairs.tick(plane, false, false);
        assertEquals(901F, part.getHealth());
        part.damage(1000F, false);
        repairs.tick(plane, false, false);
        assertTrue(part.isDestroyed());
    }

    @Test
    void sessionRepairsAtTickFifteenThenEveryTwentyOneTicksAndNeverRebuildsDestroyedParts()
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
        when(data.getParts()).thenReturn(Map.of(EnumDriveablePart.CORE, part));
        when(driveable.canRepairPart(EnumDriveablePart.CORE)).thenAnswer(invocation -> !part.isDestroyed() && part.getHealth() < part.getMaxHealth());
        when(driveable.repairPart(eq(EnumDriveablePart.CORE), anyFloat())).thenAnswer(invocation -> part.repair(invocation.getArgument(1)) > 0F);
        DriveableRepairs repairs = new DriveableRepairs();
        for (int tick = 0; tick < 14; ++tick)
            repairs.tick(driveable, true, false);
        assertEquals(900F, part.getHealth());
        repairs.tick(driveable, true, false);
        assertEquals(906F, part.getHealth());
        for (int tick = 0; tick < 21; ++tick)
            repairs.tick(driveable, true, false);
        assertEquals(912F, part.getHealth());
        part.damage(1000F, false);
        for (int tick = 0; tick < 40; ++tick)
            repairs.tick(driveable, true, false);
        assertTrue(part.isDestroyed());
        assertEquals(0F, part.getHealth());
    }
}
