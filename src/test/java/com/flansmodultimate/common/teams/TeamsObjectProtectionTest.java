package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.block.CapturePointBlock;
import com.flansmodultimate.common.block.TeamBannerBlock;
import com.flansmodultimate.common.block.TeamObjectBlock;
import com.flansmodultimate.common.block.TeamSpawnerBlock;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsObjectProtectionTest
{
    private static java.util.List<TeamObjectBlock> blocks;

    @BeforeAll
    @SuppressWarnings({"UnstableApiUsage", "deprecation", "unchecked"}) // JUnit opens registries normally managed by Forge's loader.
    static void bootstrap()
    {
        TeamsMapTest.bootstrapRegistries();
        ForgeRegistry<Block> registry = (ForgeRegistry<Block>) ForgeRegistries.BLOCKS;
        registry.unfreeze();
        ((MappedRegistry<Block>) BuiltInRegistries.BLOCK).unfreeze();
        try
        {
            blocks = java.util.List.of(new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.PLAYER, BlockBehaviour.Properties.of()),
                new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.ITEM, BlockBehaviour.Properties.of()), new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.VEHICLE, BlockBehaviour.Properties.of()),
                new TeamBannerBlock(BlockBehaviour.Properties.of()), new CapturePointBlock(BlockBehaviour.Properties.of()));
            for (int i = 0; i < blocks.size(); i++)
                registry.register(ResourceLocation.fromNamespaceAndPath("flansmodultimate", "test_protected_object_" + i), blocks.get(i));
        }
        finally
        {
            BuiltInRegistries.BLOCK.freeze();
            registry.freeze();
        }
    }

    @Test
    void protectsAllInfrastructureOnlyDuringRounds()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            ServerLevel level = mock(ServerLevel.class);
            for (var block : blocks)
            {
                when(level.getBlockState(BlockPos.ZERO)).thenReturn(block.defaultBlockState());
                when(manager.isRoundRunning()).thenReturn(true);
                assertTrue(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
                assertFalse(block.canDropFromExplosion(block.defaultBlockState(), level, BlockPos.ZERO, mock(Explosion.class)));
                assertEquals(PushReaction.BLOCK, block.getPistonPushReaction(block.defaultBlockState()));
                when(manager.isRoundRunning()).thenReturn(false);
                assertFalse(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
            }
            when(manager.isRoundRunning()).thenReturn(true);
            when(level.getBlockState(BlockPos.ZERO)).thenReturn(Blocks.STONE.defaultBlockState());
            assertFalse(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
        }
    }

    @Test
    void onlyDirectOperatorBreakingGetsAnException()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isRoundRunning()).thenReturn(true);
            ServerLevel level = mock(ServerLevel.class);
            ServerPlayer player = mock(ServerPlayer.class);
            var block = blocks.get(0);
            var state = block.defaultBlockState();
            when(level.getBlockState(BlockPos.ZERO)).thenReturn(state);
            assertFalse(block.onDestroyedByPlayer(state, level, BlockPos.ZERO, player, false, Fluids.EMPTY.defaultFluidState()));
            verify(level, never()).setBlock(any(), any(), anyInt());
            when(player.hasPermissions(2)).thenReturn(true);
            when(level.setBlock(eq(BlockPos.ZERO), any(), anyInt())).thenAnswer(call -> !TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
            assertTrue(block.onDestroyedByPlayer(state, level, BlockPos.ZERO, player, false, Fluids.EMPTY.defaultFluidState()));
            // An operator-caused blast still has no direct-edit scope, even immediately after a permitted break.
            assertTrue(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
            block.onBlockExploded(state, level, BlockPos.ZERO, mock(Explosion.class));
            verify(level, times(1)).setBlock(any(), any(), anyInt());
        }
    }

    @Test
    @SuppressWarnings("AutoCloseableResource") // Mockito worlds are borrowed test fixtures, not owned closeable resources.
    void directRemovalCannotLeakToAnotherPositionOrSurviveAnException()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isRoundRunning()).thenReturn(true);
            ServerLevel level = mock(ServerLevel.class);
            ServerLevel otherLevel = mock(ServerLevel.class);
            var state = blocks.get(3).defaultBlockState();
            when(level.getBlockState(any())).thenReturn(state);
            when(otherLevel.getBlockState(any())).thenReturn(state);
            assertThrows(IllegalStateException.class, () -> TeamsObjectProtection.removeDirectly(level, BlockPos.ZERO, () ->
            {
                assertFalse(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
                assertTrue(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO.above()));
                assertTrue(TeamsObjectProtection.preventsRemoval(otherLevel, BlockPos.ZERO));
                throw new IllegalStateException("Failed edit");
            }));
            assertTrue(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
        }
    }
}
