package com.flansmodultimate.apocalyse.common.world;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.platform.entity.EntityPlatform;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ApocalypseBossFightManager
{
    private static final int BOSS_SPAWN_HEIGHT = 22;

    private ApocalypseBossFightManager()
    {
    }

    public static void tryActivate(Level level, BlockPos placedPos, @Nullable LivingEntity placer)
    {
        if (!(level instanceof ServerLevel serverLevel)
            || !level.dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL)
            || !ModApocalypseConfig.apocalypseMobsEnabled()
            || !level.getBlockState(placedPos.below()).is(Blocks.BEDROCK))
            return;

        if (tryActivateOriginPillars(serverLevel, placedPos, placer))
            return;

        // Altars from worlds generated before the origin pillars were restored.
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

    /**
     * The 1.12.2 summon: a power cube on the inner corner of each of the four bedrock pillars
     * around the origin wakes the boss at the centre, above the crossroads.
     */
    private static boolean tryActivateOriginPillars(ServerLevel level, BlockPos placedPos, @Nullable LivingEntity placer)
    {
        int corner = (int) ApocalypseStructures.PILLAR_INNER_EDGE + 1;
        if (Math.abs(placedPos.getX()) != corner || Math.abs(placedPos.getZ()) != corner)
            return false;

        for (int sx = -1; sx <= 1; sx += 2)
        {
            for (int sz = -1; sz <= 1; sz += 2)
            {
                if (!isPowerCubeOnBedrock(level, new BlockPos(corner * sx, placedPos.getY(), corner * sz)))
                    return false;
            }
        }

        BlockPos spawn = BlockPos.containing(0D, Math.min(ApocalypseStructures.BOSS_SPAWN_HEIGHT, level.getMaxBuildHeight() - 10D), 0D);
        // The 1.12.2 arena: the boss circles the origin between y=140 and y=220.
        spawnBossAt(level, spawn, Vec3.atBottomCenterOf(spawn), placer);
        for (int sx = -1; sx <= 1; sx += 2)
        {
            for (int sz = -1; sz <= 1; sz += 2)
                level.destroyBlock(new BlockPos(corner * sx, placedPos.getY(), corner * sz), false);
        }
        return true;
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
        BlockPos spawn = corner.offset(2, BOSS_SPAWN_HEIGHT, 2);
        // Keep the whole hover arc above the altar it was summoned from.
        spawnBossAt(level, spawn, Vec3.atBottomCenterOf(spawn.above(80)), placer);
    }

    private static void spawnBossAt(ServerLevel level, BlockPos center, Vec3 home, @Nullable LivingEntity placer)
    {
        AABB existingBosses = new AABB(center).inflate(128.0D);
        if (!level.getEntitiesOfClass(SkullBossEntity.class, existingBosses).isEmpty())
            return;

        SkullBossEntity boss = ApocalypseContent.skullBoss.get().create(level);
        if (boss == null)
            return;

        boss.moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        boss.setHome(home);
        if (placer != null)
            boss.setTarget(placer);
        EntityPlatform.finalizeSpawn(boss, level, level.getCurrentDifficultyAt(center), MobSpawnType.TRIGGERED);
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
