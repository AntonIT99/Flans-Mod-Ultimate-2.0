package com.flansmodultimate.common.driveables.collision;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HullCarryingTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aPlayerAtTheBowFollowsASmallTurnEvenWhenItsArcExceedsThreeBlocks(boolean partBody)
    {
        Harness harness = new Harness(partBody);
        harness.tick(Vec3.ZERO, 0F);
        harness.tick(Vec3.ZERO, 5F);

        Vec3 expected = new Vec3(-40D * Math.sin(Math.toRadians(5D)), 1D, 40D * Math.cos(Math.toRadians(5D)));
        assertEquals(expected.x, harness.feet.get().x, 1.0E-5D);
        assertEquals(expected.y, harness.feet.get().y, 1.0E-5D);
        assertEquals(expected.z, harness.feet.get().z, 1.0E-5D);
        assertEquals(5F, harness.player.getYRot(), 1.0E-4F);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void supportIsKeptWhileALargeShipTurnsMovesAndDescends(boolean partBody)
    {
        Harness harness = new Harness(partBody);
        harness.tick(Vec3.ZERO, 0F);
        for (int tick = 1; tick <= 12; tick++)
        {
            Vec3 origin = new Vec3(tick * 0.25D, tick * -0.05D, 0D);
            harness.tick(origin, tick * 5F);
            double radians = Math.toRadians(tick * 5D);
            assertEquals(origin.x - 40D * Math.sin(radians), harness.feet.get().x, 1.0E-5D);
            assertEquals(origin.y + 1D, harness.feet.get().y, 1.0E-5D);
            assertEquals(40D * Math.cos(radians), harness.feet.get().z, 1.0E-5D);
            assertEquals(tick * 5F, harness.player.getYRot(), 1.0E-4F);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aTeleportDoesNotDragAPlayerWithTheHull(boolean partBody)
    {
        Harness harness = new Harness(partBody);
        harness.tick(Vec3.ZERO, 0F);
        harness.tick(new Vec3(4D, 0D, 0D), 0F);

        assertEquals(new Vec3(0D, 1D, 40D), harness.feet.get());
        assertEquals(0F, harness.player.getYRot());
    }

    private static final class Harness
    {
        private final AtomicReference<Vec3> feet = new AtomicReference<>(new Vec3(0D, 1D, 40D));
        private final Entity player = mock(Entity.class);
        private final Driveable owner = mock(Driveable.class);
        private final AtomicReference<Vec3> origin = new AtomicReference<>(Vec3.ZERO);
        private final Map<EnumDriveablePart, CollisionBox> boxes = Map.of(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(100F, -1F, 0F, -50F, 2F, 1F, 100F, 0F, 0F));
        private final PartBodyCollisionHelper body;
        private final DriveableCollisionHelper driveable;
        private float yaw;

        private Harness(boolean partBody)
        {
            Level level = mock(Level.class);
            DriveableType type = mock(DriveableType.class);
            when(level.getWorldBorder()).thenReturn(new WorldBorder());
            when(level.getBlockCollisions(any(), any())).thenReturn(List.of());
            when(level.getEntities(nullable(Entity.class), any(AABB.class), any())).thenReturn(List.of(player));
            when(owner.level()).thenReturn(level);
            when(owner.getConfigType()).thenReturn(type);
            when(owner.getX()).thenAnswer(call -> origin.get().x);
            when(owner.getY()).thenAnswer(call -> origin.get().y);
            when(owner.getZ()).thenAnswer(call -> origin.get().z);
            when(owner.getYaw()).thenAnswer(call -> yaw - 90F);
            when(owner.getCollisionTurretPivot()).thenReturn(Vec3.ZERO);
            when(owner.getCollisionTurretOffset()).thenReturn(Vec3.ZERO);
            when(owner.isPartIntact(any())).thenReturn(true);
            when(player.level()).thenReturn(level);
            when(player.position()).thenAnswer(call -> feet.get());
            when(player.getX()).thenAnswer(call -> feet.get().x);
            when(player.getY()).thenAnswer(call -> feet.get().y);
            when(player.getZ()).thenAnswer(call -> feet.get().z);
            when(player.getBoundingBox()).thenAnswer(call -> new AABB(feet.get().x - 0.3D, feet.get().y, feet.get().z - 0.3D, feet.get().x + 0.3D, feet.get().y + 1.8D, feet.get().z + 0.3D));
            doAnswer(call ->
            {
                feet.set(new Vec3(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
                return null;
            }).when(player).setPos(anyDouble(), anyDouble(), anyDouble());
            AtomicReference<Float> viewYaw = new AtomicReference<>(0F);
            when(player.getYRot()).thenAnswer(call -> viewYaw.get());
            doAnswer(call ->
            {
                viewYaw.set(call.getArgument(0));
                return null;
            }).when(player).setYRot(anyFloat());
            body = partBody ? new PartBodyCollisionHelper(owner, () -> true) : null;
            driveable = partBody ? null : new DriveableCollisionHelper(PartBodyCollisionHelper.profile(boxes, 1F));
        }

        private void tick(Vec3 position, float heading)
        {
            origin.set(position);
            yaw = heading;
            owner.tickCount++;
            if (body != null)
                body.tick(boxes, 1, position, heading, 0F, 0F, 1F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> true);
            else
                driveable.tick(owner);
        }
    }
}
