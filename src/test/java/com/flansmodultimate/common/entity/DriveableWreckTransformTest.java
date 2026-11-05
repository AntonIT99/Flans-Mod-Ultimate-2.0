package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.driveables.physics.DriveablePrediction;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriveableWreckTransformTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void becomingAWreckKeepsTheServerPositionAndAllThreeAngles()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            when(ModCommonConfig.get().driveableWreckLifetime()).thenReturn(300);
            Vehicle vehicle = new Vehicle(EntityType.SNOWBALL, world.level);
            vehicle.configType = new VehicleType();
            vehicle.setPos(12.25D, 64.5D, -37.75D);
            vehicle.setOrientation(137F, -12F, 23F);
            vehicle.setOnGround(true);
            Vec3 position = vehicle.position();

            assertTrue(vehicle.becomeWreck());
            assertEquals(position, vehicle.position());
            assertEquals(137F, vehicle.getYaw());
            assertEquals(-12F, vehicle.getPitch());
            assertEquals(23F, vehicle.getRoll());
        }
    }

    @Test
    void destructionEndsPredictionWithoutReplacingTheVisiblePose() throws ReflectiveOperationException
    {
        Level level = mock(Level.class);
        Vehicle vehicle = new Vehicle(EntityType.SNOWBALL, level);
        vehicle.setPos(12D, 64D, -37D);
        vehicle.setOrientation(137F, -12F, 23F);
        // A mocked level skips the constructor that sets this final side flag.
        var clientSide = Level.class.getDeclaredField("isClientSide");
        clientSide.setAccessible(true);
        clientSide.setBoolean(level, true);
        DriveablePrediction.Frame input = new DriveablePrediction.Frame(1, 0, 0F, 0F, false);
        vehicle.submitPredictedInput(input);
        vehicle.setPos(13D, 64.5D, -38D);
        vehicle.setOrientation(145F, -10F, 20F);
        vehicle.lerpTo(12D, 64D, -37D, 227F, 12F, 3, false);

        vehicle.getEntityData().set(Driveable.DATA_WRECK_AGE, 0);
        assertEquals(new Vec3(13D, 64.5D, -38D), vehicle.position());
        assertEquals(145F, vehicle.getYaw());
        assertEquals(-10F, vehicle.getPitch());
        assertEquals(20F, vehicle.getRoll());

        // Late input must not restart prediction or snap the wreck back to an older movement target.
        vehicle.submitPredictedInput(input);
        assertEquals(new Vec3(13D, 64.5D, -38D), vehicle.position());
        vehicle.lerpTo(14D, 64D, -39D, 235F, 10F, 3, true);
        assertEquals(new Vec3(14D, 64D, -39D), vehicle.position(), "wrecks must accept authoritative movement immediately");
        assertEquals(145F, vehicle.getYaw());
        assertEquals(-10F, vehicle.getPitch());
        assertEquals(23F, vehicle.getRoll());
    }
}
