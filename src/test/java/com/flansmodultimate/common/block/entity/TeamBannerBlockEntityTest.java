package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.common.block.TeamBannerBlock;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamBannerBlockEntityTest
{
    private static final BlockPos POS = new BlockPos(4, 70, 8);
    private static TeamBannerBlock bannerBlock;

    @BeforeAll
    @SuppressWarnings({"UnstableApiUsage", "deprecation", "unchecked"}) // Plain JUnit must open the registries normally managed by Forge's loader.
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        ForgeRegistry<Block> registry = (ForgeRegistry<Block>) ForgeRegistries.BLOCKS;
        registry.unfreeze();
        ((MappedRegistry<Block>) BuiltInRegistries.BLOCK).unfreeze();
        try
        {
            bannerBlock = new TeamBannerBlock(BlockBehaviour.Properties.of().noCollission());
            registry.register(ResourceLocation.fromNamespaceAndPath("flansmodultimate", "test_team_banner"), bannerBlock);
        }
        finally
        {
            BuiltInRegistries.BLOCK.freeze();
            registry.freeze();
        }
    }

    private static TeamBannerBlockEntity banner(BlockState state)
    {
        return new TeamBannerBlockEntity(mock(BlockEntityType.class), POS, state);
    }

    @Test
    void savedLinkIdentityAndColourReachBothReloadedAndNewlyTrackingClients()
    {
        TeamBannerBlockEntity original = banner(bannerBlock.defaultBlockState());
        UUID baseId = UUID.randomUUID();
        original.setBaseId(baseId);
        CompoundTag saved = new CompoundTag();
        original.saveData(saved, null);
        saved.putInt("team_colour", 0x336699);

        TeamBannerBlockEntity restored = banner(original.getBlockState());
        restored.loadData(saved, null);
        assertEquals(original.getObjectId(), restored.getObjectId());
        assertEquals(baseId, restored.getBaseId());
        assertEquals(0x336699, restored.getTeamColour());
        assertFalse(restored.isSpawnPoint());

        CompoundTag update = restored.createUpdateTag(null);
        assertEquals(baseId, update.getUUID("base_id"));
        assertEquals(0x336699, update.getInt("team_colour"));
        assertNotNull(restored.getUpdatePacket());

        restored.loadData(new CompoundTag(), null);
        assertNull(restored.getBaseId());
        assertEquals(TeamBannerBlockEntity.UNOWNED_COLOUR, restored.getTeamColour());
        assertNotEquals(original.getObjectId(), restored.getObjectId());
    }

    @Test
    void followsCapturesAndNeutralisationWithoutSendingUnchangedColours()
    {
        TeamBannerBlockEntity banner = banner(bannerBlock.defaultBlockState());
        ServerLevel level = mock(ServerLevel.class);
        banner.setLevel(level);
        UUID baseId = UUID.randomUUID();
        ITeamBase base = mock(ITeamBase.class);
        TeamsManager manager = mock(TeamsManager.class);
        Team first = mock(Team.class);
        Team second = mock(Team.class);
        when(first.getTeamColour()).thenReturn(0xDD2222);
        when(second.getTeamColour()).thenReturn(0x2244DD);
        when(manager.getBase(baseId)).thenReturn(Optional.of(base));
        when(manager.getTeamForBase(base)).thenReturn(first);

        try (var managers = mockStatic(TeamsManager.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            banner.setBaseId(baseId);
            assertEquals(0xDD2222, banner.getTeamColour());
            clearInvocations(level);

            when(level.getGameTime()).thenReturn(20L);
            TeamBannerBlockEntity.serverTick(level, POS, banner.getBlockState(), banner);
            verify(level, never()).sendBlockUpdated(any(), any(), any(), anyInt());

            when(manager.getTeamForBase(base)).thenReturn(second);
            TeamBannerBlockEntity.serverTick(level, POS, banner.getBlockState(), banner);
            assertEquals(0x2244DD, banner.getTeamColour());
            verify(level).sendBlockUpdated(POS, banner.getBlockState(), banner.getBlockState(), Block.UPDATE_CLIENTS);

            when(manager.getTeamForBase(base)).thenReturn(null);
            TeamBannerBlockEntity.serverTick(level, POS, banner.getBlockState(), banner);
            assertEquals(TeamBannerBlockEntity.UNOWNED_COLOUR, banner.getTeamColour());

            when(manager.getTeamForBase(base)).thenReturn(first);
            TeamBannerBlockEntity.serverTick(level, POS, banner.getBlockState(), banner);
            when(manager.getBase(baseId)).thenReturn(Optional.empty());
            TeamBannerBlockEntity.serverTick(level, POS, banner.getBlockState(), banner);
            assertEquals(TeamBannerBlockEntity.UNOWNED_COLOUR, banner.getTeamColour());
            assertEquals(baseId, banner.getBaseId());
        }
    }

    @Test
    void syncsAndSavesTheHoldingTeamSoClientsCanDrawItsFlagEvenWhenColoursMatch()
    {
        TeamBannerBlockEntity banner = banner(bannerBlock.defaultBlockState());
        ServerLevel level = mock(ServerLevel.class);
        banner.setLevel(level);
        UUID baseId = UUID.randomUUID();
        ITeamBase base = mock(ITeamBase.class);
        TeamsManager manager = mock(TeamsManager.class);
        Team first = mock(Team.class);
        Team second = mock(Team.class);
        when(first.getTeamColour()).thenReturn(0x2244DD);
        when(second.getTeamColour()).thenReturn(0x2244DD);
        when(first.getShortName()).thenReturn("blue");
        when(second.getShortName()).thenReturn("navy");
        when(manager.getBase(baseId)).thenReturn(Optional.of(base));
        when(manager.getTeamForBase(base)).thenReturn(first);

        try (var managers = mockStatic(TeamsManager.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            banner.setBaseId(baseId);
            assertEquals("blue", banner.getTeamName());
            clearInvocations(level);

            when(level.getGameTime()).thenReturn(20L);
            when(manager.getTeamForBase(base)).thenReturn(second);
            TeamBannerBlockEntity.serverTick(level, POS, banner.getBlockState(), banner);
            assertEquals("navy", banner.getTeamName());
            verify(level).sendBlockUpdated(POS, banner.getBlockState(), banner.getBlockState(), Block.UPDATE_CLIENTS);
            assertEquals("navy", banner.createUpdateTag(null).getString("team_name"));

            when(manager.getTeamForBase(base)).thenReturn(null);
            TeamBannerBlockEntity.serverTick(level, POS, banner.getBlockState(), banner);
            assertEquals("", banner.getTeamName());
        }

        CompoundTag saved = new CompoundTag();
        saved.putString("team_name", "blue");
        TeamBannerBlockEntity restored = banner(bannerBlock.defaultBlockState());
        restored.loadData(saved, null);
        assertEquals("blue", restored.getTeamName());
    }

    @Test
    void unloadKeepsTheLinkWhileLoadRegistersItAgainAndUnlinkTurnsItGrey()
    {
        TeamBannerBlockEntity banner = banner(bannerBlock.defaultBlockState());
        banner.setLevel(mock(ServerLevel.class));
        UUID baseId = UUID.randomUUID();
        TeamsManager manager = mock(TeamsManager.class);
        ITeamBase base = mock(ITeamBase.class);
        Team team = mock(Team.class);
        when(team.getTeamColour()).thenReturn(0xAA4400);
        when(manager.getBase(baseId)).thenReturn(Optional.of(base));
        when(manager.getTeamForBase(base)).thenReturn(team);
        try (var managers = mockStatic(TeamsManager.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            banner.setBaseId(baseId);
            banner.onLoad();
            verify(manager).registerObject(banner);
            banner.setRemoved();
            verify(manager).unregisterObject(banner.getObjectId());
            assertEquals(baseId, banner.getBaseId());
            verify(base, never()).removeObject(any());

            banner.clearRemoved();
            banner.onLoad();
            verify(manager, times(2)).registerObject(banner);
            banner.setBaseId(null);
            assertEquals(TeamBannerBlockEntity.UNOWNED_COLOUR, banner.getTeamColour());
        }
    }

    @Test
    void aNonServerWorldNeverResolvesOwnership()
    {
        TeamBannerBlockEntity banner = banner(bannerBlock.defaultBlockState());
        Level level = mock(Level.class);
        banner.setLevel(level);
        try (var managers = mockStatic(TeamsManager.class))
        {
            banner.setBaseId(UUID.randomUUID());
            TeamBannerBlockEntity.serverTick(level, POS, banner.getBlockState(), banner);
            banner.onLoad();
            managers.verifyNoInteractions();
        }
    }

    @Test
    void allFourWallOrientationsNeedSupportAndFallWhenItIsRemoved()
    {
        TeamBannerBlock block = bannerBlock;
        LevelAccessor level = mock(LevelAccessor.class);
        for (Direction facing : Direction.Plane.HORIZONTAL)
        {
            BlockState state = block.defaultBlockState().setValue(TeamBannerBlock.FACING, facing);
            BlockPos support = POS.relative(facing.getOpposite());
            when(level.getBlockState(support)).thenReturn(Blocks.STONE.defaultBlockState());
            assertTrue(state.canSurvive(level, POS));
            when(level.getBlockState(support)).thenReturn(Blocks.AIR.defaultBlockState());
            assertFalse(state.canSurvive(level, POS));
            assertTrue(block.updateShape(state, facing.getOpposite(), Blocks.AIR.defaultBlockState(), level, POS, support).isAir());
        }
    }

    @Test
    void breakingABannerRemovesItsBaseMembership()
    {
        TeamBannerBlock block = bannerBlock;
        BlockState state = block.defaultBlockState();
        TeamBannerBlockEntity banner = banner(state);
        UUID baseId = UUID.randomUUID();
        banner.setBaseId(baseId);
        ServerLevel level = mock(ServerLevel.class);
        when(level.getBlockEntity(POS)).thenReturn(banner);
        TeamsManager manager = mock(TeamsManager.class);
        ITeamBase base = mock(ITeamBase.class);
        when(manager.getBase(baseId)).thenReturn(Optional.of(base));
        try (var managers = mockStatic(TeamsManager.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            block.onRemove(state, level, POS, Blocks.AIR.defaultBlockState(), false);
            verify(base).removeObject(banner.getObjectId());
            verify(level).removeBlockEntity(POS);
        }
    }
}
