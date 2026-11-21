package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.raytracing.hits.BlockHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketBlockHitEffect;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockImpactTest
{
    @Test
    void physicalRoundContinuesAfterPenetratingAndFeedbackUsesWorldPosition() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        var level = mock(Level.class);
        var localPos = new BlockPos(1000, 0, 1000);
        var state = Blocks.STONE.defaultBlockState();
        when(level.getBlockState(localPos)).thenReturn(state);
        var impact = new Vec3(10, 20, 30);
        var normal = new Vec3(0, 0, 1);
        var hit = new BlockHit(new BlockHitResult(localPos.getCenter(), Direction.WEST, localPos, false), 0.5F, state, impact, normal);
        var shot = mock(FiredShot.class);
        var type = mock(BulletType.class, RETURNS_DEEP_STUBS);
        when(shot.getBulletType()).thenReturn(type);
        var bullet = mock(Bullet.class);
        when(bullet.getPenetrationLosses()).thenReturn(new ArrayList<>());
        var settings = mock(CommonConfigSnapshot.class);
        when(settings.enableBlockPenetration()).thenReturn(true);
        try (var config = mockStatic(ModCommonConfig.class); var packets = mockStatic(PacketHandler.class))
        {
            config.when(ModCommonConfig::get).thenReturn(settings);
            float remaining = ShootingHelper.handleBlockHit(level, hit, normal.reverse(), shot, 10F, bullet);
            assertTrue(remaining > 0F && remaining < 10F);
            assertEquals(1, bullet.getPenetrationLosses().size());
            verify(bullet, never()).handleBounceOrStop(any(), any(Vec3.class), any());
            assertTrue(ShootingHelper.handleBlockHit(level, hit, normal.reverse(), shot, 1F, bullet) <= 0F);
            packets.verify(() -> PacketHandler.sendToAllAround(any(PacketBlockHitEffect.class), eq(impact), eq(0D), any()), times(2));
        }
    }
}
