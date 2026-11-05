package com.flansmodultimate.api.entity;

import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.item.VehicleItem;
import com.flansmodultimate.common.item.PlaneItem;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlansEntityTypesSpawnTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void vehicleAndAircraftWrecksKeepTheRequestedHeading()
    {
        ServerLevel level = mock(ServerLevel.class);
        VehicleType vehicleType = mock(VehicleType.class);
        PlaneType planeType = mock(PlaneType.class);
        VehicleItem vehicleItem = mock(VehicleItem.class);
        PlaneItem planeItem = mock(PlaneItem.class);
        // Supply a registered stack item without mutating Minecraft's frozen test registry.
        when(vehicleItem.asItem()).thenReturn(Items.STONE);
        when(planeItem.asItem()).thenReturn(Items.STONE);
        when(vehicleType.getItem()).thenReturn(vehicleItem);
        when(planeType.getItem()).thenReturn(planeItem);
        Vehicle vehicle = mock(Vehicle.class);
        Plane plane = mock(Plane.class);
        when(vehicle.makeReplacementWreck()).thenReturn(true);
        when(plane.makeReplacementWreck()).thenReturn(true);
        doReturn(vehicle).when(vehicleItem).spawnDriveable(eq(level), anyDouble(), anyDouble(), anyDouble(), anyFloat(), isNull(), any());
        doReturn(plane).when(planeItem).spawnDriveable(eq(level), anyDouble(), anyDouble(), anyDouble(), anyFloat(), isNull(), any());
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::driveableWreckLifetimeTicks).thenReturn(1200);
            for (float heading : new float[]{0F, 90F, -90F, 179F, -179F})
            {
                for (boolean wreck : new boolean[]{false, true})
                {
                    if (wreck)
                    {
                        assertTrue(FlansEntityTypes.spawnDriveableWreck(level, vehicleType, Vec3.ZERO, heading, 0).isPresent());
                        assertTrue(FlansEntityTypes.spawnDriveableWreck(level, planeType, Vec3.ZERO, heading, 0).isPresent());
                    }
                    else
                    {
                        assertTrue(FlansEntityTypes.spawnDriveable(level, vehicleType, Vec3.ZERO, heading, 0.3F).isPresent());
                        assertTrue(FlansEntityTypes.spawnDriveable(level, planeType, Vec3.ZERO, heading, 0.3F).isPresent());
                    }
                    ArgumentCaptor<Float> yaw = ArgumentCaptor.forClass(Float.class);
                    verify(vehicleItem).spawnDriveable(eq(level), eq(0D), eq(0D), eq(0D), yaw.capture(), isNull(), any());
                    assertEquals(heading, LegacyDriveableCoordinates.renderedForwardYaw(yaw.getValue(), false), 0.001F);
                    verify(planeItem).spawnDriveable(eq(level), eq(0D), eq(0D), eq(0D), yaw.capture(), isNull(), any());
                    assertEquals(heading, LegacyDriveableCoordinates.renderedForwardYaw(yaw.getValue(), true), 0.001F);
                    clearInvocations(vehicleItem, planeItem);
                }
            }
        }
    }
}
