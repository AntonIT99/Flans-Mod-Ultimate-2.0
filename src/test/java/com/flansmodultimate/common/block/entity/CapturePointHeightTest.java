package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CapturePointHeightTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static CapturePointBlockEntity point()
    {
        return new CapturePointBlockEntity(mock(BlockEntityType.class), BlockPos.ZERO, Blocks.AIR.defaultBlockState());
    }

    @Test
    void heightIsSavedAndSentToNewlyTrackingClients()
    {
        CapturePointBlockEntity point = point();
        ServerLevel level = mock(ServerLevel.class);
        point.setLevel(level);
        point.setPoleHeight(17);
        verify(level).sendBlockUpdated(BlockPos.ZERO, point.getBlockState(), point.getBlockState(), 3);
        CompoundTag saved = new CompoundTag();
        point.saveData(saved, null);
        CapturePointBlockEntity restored = point();
        restored.loadData(saved, null);
        assertEquals(17, restored.getPoleHeight());
        CapturePointBlockEntity client = point();
        client.loadData(point.createUpdateTag(null), null);
        assertEquals(17, client.getPoleHeight());
        restored.roundCleanup();
        assertEquals(17, restored.getPoleHeight());
    }

    @Test
    void oldSavesUseTheConfiguredDefaultAndInvalidHeightsAreBounded()
    {
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::defaultFlagpoleHeight).thenReturn(8);
            CapturePointBlockEntity point = point();
            assertEquals(8, point.getPoleHeight());
            point.setPoleHeight(13);
            point.loadData(new CompoundTag(), null);
            assertEquals(8, point.getPoleHeight());
            point.setPoleHeight(-1);
            assertEquals(2, point.getPoleHeight());
            CompoundTag invalid = new CompoundTag();
            invalid.putInt("pole_height", 10000);
            point.loadData(invalid, null);
            assertEquals(64, point.getPoleHeight());
        }
    }
}
