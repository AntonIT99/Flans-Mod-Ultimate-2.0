package com.flansmodultimate.apocalyse.common.world;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class ApocalypseBossFightManager
{
    private static final int ALTAR_SIZE = 4;
    private static final int BOSS_SPAWN_HEIGHT = 22;

    private ApocalypseBossFightManager()
    {
    }

    public static void tryActivate(Level level, BlockPos placedPos, @Nullable LivingEntity placer)
    {
        if (!(level instanceof ServerLevel serverLevel)
            || !level.dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL)
            || !ModCommonConfig.apocalypseMobsEnabled()
            || !level.getBlockState(placedPos.below()).is(Blocks.BEDROCK))
            return;

        for (int dx = 0; dx <= 3; dx += 3)
        {
            for (int dz = 0; dz <= 3; dz += 3)
            {
                BlockPos corner = placedPos.offset(-dx, 0, -dz);
                if (isBossAltar(level, corner))
                {
                    spawnBoss(serverLevel, corner, placer);
                    consumeAltarCubes(serverLevel, corner);
                    return;
                }
            }
        }
    }

    public static void generateAltar(ServerLevel level, BlockPos corner)
    {
        for (int dx = 0; dx < ALTAR_SIZE; dx++)
        {
            for (int dz = 0; dz < ALTAR_SIZE; dz++)
            {
                BlockPos floor = corner.offset(dx, -1, dz);
                boolean altarCorner = (dx == 0 || dx == 3) && (dz == 0 || dz == 3);
                level.setBlock(floor, altarCorner ? Blocks.BEDROCK.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState(), 3);
                for (int dy = 0; dy < 4; dy++)
                    level.setBlock(corner.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static boolean isBossAltar(Level level, BlockPos corner)
    {
        if (!isPowerCubeOnBedrock(level, corner)
            || !isPowerCubeOnBedrock(level, corner.offset(3, 0, 0))
            || !isPowerCubeOnBedrock(level, corner.offset(0, 0, 3))
            || !isPowerCubeOnBedrock(level, corner.offset(3, 0, 3)))
            return false;

        for (int dx = 1; dx <= 2; dx++)
        {
            for (int dz = 1; dz <= 2; dz++)
            {
                if (!level.getBlockState(corner.offset(dx, -1, dz)).is(Blocks.OBSIDIAN))
                    return false;
            }
        }
        return true;
    }

    private static boolean isPowerCubeOnBedrock(Level level, BlockPos pos)
    {
        return level.getBlockState(pos).is(ApocalypseContent.blockPowerCube.get()) && level.getBlockState(pos.below()).is(Blocks.BEDROCK);
    }

    private static void spawnBoss(ServerLevel level, BlockPos corner, @Nullable LivingEntity placer)
    {
        BlockPos center = corner.offset(2, BOSS_SPAWN_HEIGHT, 2);
        AABB existingBosses = new AABB(center).inflate(128.0D);
        if (!level.getEntitiesOfClass(SkullBossEntity.class, existingBosses).isEmpty())
            return;

        SkullBossEntity boss = ApocalypseContent.skullBoss.get().create(level);
        if (boss == null)
            return;

        boss.moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        if (placer != null)
            boss.setTarget(placer);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(center), MobSpawnType.TRIGGERED, null, null);
        level.addFreshEntity(boss);
        level.players().stream()
            .filter(player -> player.distanceToSqr(center.getX(), center.getY(), center.getZ()) < 256.0D * 256.0D)
            .forEach(player -> player.displayClientMessage(Component.translatable("message.flansmodultimate.apocalypse_boss_awakened"), false));
    }

    private static void consumeAltarCubes(ServerLevel level, BlockPos corner)
    {
        level.destroyBlock(corner, false);
        level.destroyBlock(corner.offset(3, 0, 0), false);
        level.destroyBlock(corner.offset(0, 0, 3), false);
        level.destroyBlock(corner.offset(3, 0, 3), false);
    }
}
