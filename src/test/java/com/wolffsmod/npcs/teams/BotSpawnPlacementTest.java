package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.FlanModelShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotSpawnPlacementTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static ServerLevel ground()
    {
        ServerLevel level = mock(ServerLevel.class);
        ServerChunkCache chunks = mock(ServerChunkCache.class);
        when(level.getChunkSource()).thenReturn(chunks);
        when(chunks.hasChunk(anyInt(), anyInt())).thenReturn(true);
        when(level.getMinBuildHeight()).thenReturn(-64);
        when(level.getMaxBuildHeight()).thenReturn(320);
        when(level.getWorldBorder()).thenReturn(new WorldBorder());
        when(level.getAllEntities()).thenReturn(List.of());
        when(level.getBlockState(any())).thenAnswer(call -> call.getArgument(0, BlockPos.class).getY() == 63 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        when(level.noCollision(any(AABB.class))).thenReturn(true);
        return level;
    }

    private static FlanModelEntityType vehicle()
    {
        FlanModelEntityType type = mock(FlanModelEntityType.class);
        IDriveableType definition = mock(IDriveableType.class);
        when(type.getInfoType()).thenReturn(definition);
        when(definition.getBounds()).thenReturn(Optional.of(new AABB(-6D, 0D, -2D, 6D, 3D, 2D)));
        when(type.getShape()).thenReturn(new FlanModelShape(EntityDimensions.scalable(4F, 3F), 0F));
        return type;
    }

    @Test
    void vehicleAvoidsTerrainAndEntitiesUsingItsFullFootprint()
    {
        ServerLevel level = ground();
        FlanModelEntityType type = vehicle();
        Vec3 origin = new Vec3(0.5D, 65D, 0.5D);
        AABB wall = new AABB(4D, 64D, -1D, 5D, 68D, 2D);
        when(level.noCollision(any(AABB.class))).thenAnswer(call -> !call.getArgument(0, AABB.class).intersects(wall));
        LivingEntity blocker = mock(LivingEntity.class);
        when(blocker.isAlive()).thenReturn(true);
        AABB occupied = new AABB(-7D, 64D, -7D, 0D, 68D, 0D);
        when(blocker.getBoundingBox()).thenReturn(occupied);
        when(level.getAllEntities()).thenReturn(List.of(blocker));
        Vec3 first = BotSpawnPlacement.vehicle(level, type, origin, 180F).orElseThrow();
        AABB footprint = BotSpawnPlacement.vehicleBounds(type, 180F).move(first);
        assertFalse(footprint.intersects(wall));
        assertFalse(footprint.intersects(occupied));
        assertNotEquals(origin, first);
        assertEquals(64D, first.y);
        assertTrue(footprint.getXsize() >= 12D, "The capped movement hitbox cannot describe vehicle clearance");
    }

    @Test
    void blockedOrUnloadedAreasWaitInsteadOfForcingTheOrigin()
    {
        ServerLevel level = ground();
        when(level.getChunkSource().hasChunk(anyInt(), anyInt())).thenReturn(false);
        assertTrue(BotSpawnPlacement.vehicle(level, vehicle(), new Vec3(0D, 64D, 0D), 0F).isEmpty());
        verify(level, never()).getBlockState(any());
        when(level.getChunkSource().hasChunk(anyInt(), anyInt())).thenReturn(true);
        when(level.noCollision(any(AABB.class))).thenReturn(false);
        assertTrue(BotSpawnPlacement.soldier(level, new Vec3(0D, 64D, 0D)).isEmpty());
    }

    @Test
    void sequentialDeploymentWaitsForThePreviousVehicleToLeaveItsActualStagingArea()
    {
        ServerLevel level = ground();
        LivingEntity vehicle = mock(LivingEntity.class);
        UUID spawner = UUID.randomUUID();
        CompoundTag data = new CompoundTag();
        when(vehicle.getPersistentData()).thenReturn(data);
        when(vehicle.isAlive()).thenReturn(true);
        when(vehicle.getX()).thenReturn(30D);
        when(vehicle.getY()).thenReturn(64D);
        when(vehicle.getZ()).thenReturn(0D);
        when(vehicle.getBoundingBox()).thenReturn(new AABB(29D, 64D, -1D, 31D, 67D, 1D));
        BotSpawnPlacement.recordSpawner(vehicle, spawner);
        when(level.getAllEntities()).thenReturn(List.of(vehicle));
        assertTrue(BotSpawnPlacement.stagingOccupied(level, spawner, Vec3.ZERO));
        when(vehicle.getBoundingBox()).thenReturn(new AABB(49D, 64D, -1D, 51D, 67D, 1D));
        assertFalse(BotSpawnPlacement.stagingOccupied(level, spawner, Vec3.ZERO));
    }

    @Test
    void completeWavesSpreadAcrossEveryOriginBeforeReusingOneEvenWithATacticalTarget()
    {
        List<Vec3> origins = List.of(Vec3.ZERO, new Vec3(10D, 0D, 0D), new Vec3(20D, 0D, 0D), new Vec3(30D, 0D, 0D));
        Map<Vec3, Integer> uses = new HashMap<>();
        Random random = new Random(42);
        for (int index = 0; index < 40; index++)
        {
            Vec3 selected = BotSpawnPlacement.order(origins, uses, Vec3.ZERO, random).get(0);
            uses.merge(selected, 1, Integer::sum);
        }
        origins.forEach(origin -> assertEquals(10, uses.get(origin)));
    }

    @Test
    void shipsRequireWaterAndCannotExtendTheSearchPastSixtyFourBlocksFromTheSpawner()
    {
        ServerLevel level = ground();
        FlanModelEntityType ship = vehicle();
        when(((IDriveableType) ship.getInfoType()).isShip()).thenReturn(true);
        doAnswer(call -> call.getArgument(0, BlockPos.class).getY() == 63 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState()).when(level).getBlockState(any());
        when(level.getFluidState(any())).thenAnswer(call -> level.getBlockState(call.getArgument(0)).getFluidState());
        when(level.noCollision(any(AABB.class))).thenAnswer(call -> call.getArgument(0, AABB.class).getCenter().x > 66D);
        Vec3 origin = new Vec3(0.5D, 64D, 0.5D);
        try (var teams = mockStatic(FlansTeams.class))
        {
            teams.when(() -> FlansTeams.findShipSpawn(level, origin)).thenReturn(Optional.of(new Vec3(60.5D, 64D, 0.5D)));
            assertTrue(BotSpawnPlacement.vehicle(level, ship, origin, 0F).isEmpty());
            teams.when(() -> FlansTeams.findShipSpawn(level, origin)).thenReturn(Optional.empty());
            assertTrue(BotSpawnPlacement.vehicle(level, ship, origin, 0F).isEmpty());
        }
    }
}
