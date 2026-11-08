package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.driveables.damage.*;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static org.mockito.Mockito.*;

class AircraftCrashExplosionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void airborneWreckExplodesOnFirstImpactOnly() throws ReflectiveOperationException
    {
        Plane plane = mock(Plane.class);
        ServerLevel level = mock(ServerLevel.class);
        when(plane.level()).thenReturn(level);
        when(plane.getDamageClass()).thenReturn(DriveableDamageClass.PLANE);
        when(plane.position()).thenReturn(Vec3.ZERO);
        when(plane.getBoundingBox()).thenReturn(new AABB(0D, 0D, 0D, 1D, 1D, 1D));
        plane.configType = new PlaneType();
        var load = Driveable.class.getDeclaredField("wreckLoad");
        load.setAccessible(true);
        load.set(plane, new WreckLoad(0F, 1F));
        var impact = Driveable.class.getDeclaredMethod("landWreck", double.class, boolean.class);
        impact.setAccessible(true);
        try (var effects = mockStatic(AircraftCrashEffects.class); var sources = mockStatic(FlanDamageSources.class))
        {
            impact.invoke(plane, 3D, false);
            impact.invoke(plane, 3D, false);
            verify(plane, times(1)).createExplosion(any(DriveableExplosion.class), eq(Vec3.ZERO));
        }
    }

    @ParameterizedTest
    @CsvSource({"true,false,0", "false,true,-0.6"})
    void fatalImpactsExplodeButGentleLandingsDoNot(boolean wall, boolean fatal, double descent) throws ReflectiveOperationException
    {
        Plane plane = mock(Plane.class);
        ServerLevel level = mock(ServerLevel.class);
        when(plane.level()).thenReturn(level);
        when(plane.getUpVector()).thenReturn(new Vec3(0D, 1D, 0D));
        when(plane.position()).thenReturn(Vec3.ZERO);
        when(plane.getFuel()).thenReturn(100F);
        var impact = Plane.class.getDeclaredMethod("handleGroundImpact", PlaneType.class, Vec3.class);
        impact.setAccessible(true);
        try (var effects = mockStatic(AircraftCrashEffects.class))
        {
            plane.verticalCollision = true;
            impact.invoke(plane, new PlaneType(), new Vec3(4D, -0.1D, 0D));
            verify(plane, never()).createExplosion(any(), any());
            plane.horizontalCollision = wall;
            plane.destroyed = fatal;
            impact.invoke(plane, new PlaneType(), new Vec3(4D, descent, 0D));
            verify(plane).createExplosion(any(DriveableExplosion.class), eq(Vec3.ZERO));
        }
    }
}
