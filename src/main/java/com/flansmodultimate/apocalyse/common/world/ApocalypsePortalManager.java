package com.flansmodultimate.apocalyse.common.world;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.TeleporterEntity;
import com.flansmodultimate.config.ModApocalypseConfig;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.Optional;

public final class ApocalypsePortalManager
{
    private static final int PORTAL_SIZE = 4;
    private static final int PORTAL_HEIGHT_CLEARANCE = 3;

    private ApocalypsePortalManager()
    {
    }

    public static void tryActivatePortal(Level level, BlockPos placedPos)
    {
        if (!ModApocalypseConfig.apocalypsePortalsEnabled() || !ModApocalypseConfig.apocalypseDimensionEnabled())
            return;

        for (int dx = 0; dx <= 3; dx += 3)
        {
            for (int dz = 0; dz <= 3; dz += 3)
            {
                BlockPos corner = placedPos.offset(-dx, 0, -dz);
                if (isPortalFrame(level, corner))
                    spawnTeleporterIfAbsent(level, corner, null);
            }
        }
    }

    public static boolean isPortalFrame(Level level, BlockPos lowerLeftCorner)
    {
        if (!isPowerCube(level, lowerLeftCorner)
            || !isPowerCube(level, lowerLeftCorner.offset(3, 0, 0))
            || !isPowerCube(level, lowerLeftCorner.offset(0, 0, 3))
            || !isPowerCube(level, lowerLeftCorner.offset(3, 0, 3)))
            return false;

        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                if (!level.getBlockState(lowerLeftCorner.offset(dx * 3, -1, dz * 3)).is(Blocks.OBSIDIAN))
                    return false;
                if (!level.getBlockState(lowerLeftCorner.offset(1 + dx, -1, 1 + dz)).is(Blocks.OBSIDIAN))
                    return false;
            }
        }
        return true;
    }

    public static void teleportPlayer(ServerPlayer player, TeleporterEntity teleporter)
    {
        if (!ModApocalypseConfig.apocalypsePortalsEnabled() || !ModApocalypseConfig.apocalypseDimensionEnabled())
            return;

        ServerLevel sourceLevel = player.serverLevel();
        ServerLevel targetLevel;
        BlockPos searchCenter;
        boolean goingToApocalypse = !sourceLevel.dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL);

        if (goingToApocalypse)
        {
            targetLevel = player.server.getLevel(ApocalypseContent.APOCALYPSE_LEVEL);
            searchCenter = player.blockPosition();
            ApocalypseSavedData.get(sourceLevel).setEntryPoint(player.getUUID(), teleporter.getLowerLeftCorner());
        }
        else
        {
            targetLevel = player.server.getLevel(Level.OVERWORLD);
            searchCenter = ApocalypseSavedData.get(sourceLevel)
                .getEntryPoint(player.getUUID())
                .orElse(player.blockPosition());
        }

        if (targetLevel == null)
        {
            player.displayClientMessage(Component.translatable("message.flansmodultimate.apocalypse_dimension_unavailable"), true);
            return;
        }

        Optional<BlockPos> targetCorner = teleporter.getTargetTeleporter()
            .filter(pos -> targetLevel.isLoaded(pos) && isPortalFrame(targetLevel, pos))
            .or(() -> findOrCreatePortal(targetLevel, searchCenter, teleporter.getLowerLeftCorner()));

        if (targetCorner.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.flansmodultimate.apocalypse_portal_failed"), true);
            return;
        }

        BlockPos corner = targetCorner.get();
        teleporter.setTargetTeleporter(corner);
        double x = corner.getX() + 2.0D;
        double y = corner.getY() + 1.0D;
        double z = corner.getZ() + 2.0D;
        player.teleportTo(targetLevel, x, y, z, Collections.emptySet(), player.getYRot(), player.getXRot());
        player.setPortalCooldown();
    }

    public static Optional<BlockPos> findOrCreatePortal(ServerLevel level, BlockPos searchCenter, @Nullable BlockPos reciprocalTarget)
    {
        int radius = ModApocalypseConfig.apocalypseReturnRadius();
        for (int attempt = 0; attempt < 300; attempt++)
        {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.max(8.0D, radius * (0.5D + level.random.nextDouble() * 0.75D));
            int x = searchCenter.getX() + (int)Math.round(Math.cos(angle) * distance);
            int z = searchCenter.getZ() + (int)Math.round(Math.sin(angle) * distance);
            Optional<BlockPos> surface = findSurfacePortalCorner(level, x, z);
            if (surface.isPresent() && createPortal(level, surface.get(), reciprocalTarget))
                return surface;
        }
        return Optional.empty();
    }

    public static boolean createPortal(Level level, BlockPos lowerLeftCorner, @Nullable BlockPos reciprocalTarget)
    {
        if (!hasPortalClearance(level, lowerLeftCorner))
            return false;

        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                level.setBlock(lowerLeftCorner.offset(dx * 3, -1, dz * 3), Blocks.OBSIDIAN.defaultBlockState(), 3);
                level.setBlock(lowerLeftCorner.offset(dx * 3, 0, dz * 3), ApocalypseContent.blockPowerCube.get().defaultBlockState(), 3);
                level.setBlock(lowerLeftCorner.offset(1 + dx, -1, 1 + dz), Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
        }

        buildObsidianSupport(level, lowerLeftCorner);
        spawnTeleporterIfAbsent(level, lowerLeftCorner, reciprocalTarget);
        return true;
    }

    private static Optional<BlockPos> findSurfacePortalCorner(ServerLevel level, int x, int z)
    {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        for (int dy = 0; dy < 24; dy++)
        {
            BlockPos candidate = pos.below(dy);
            if (candidate.getY() <= level.getMinBuildHeight() + 2)
                break;
            if (level.getWorldBorder().isWithinBounds(candidate)
                && level.getWorldBorder().isWithinBounds(candidate.offset(3, 0, 3))
                && level.getBlockState(candidate).isAir()
                && level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP))
                return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private static boolean hasPortalClearance(Level level, BlockPos corner)
    {
        for (int dx = 0; dx < PORTAL_SIZE; dx++)
        {
            for (int dz = 0; dz < PORTAL_SIZE; dz++)
            {
                for (int dy = 0; dy < PORTAL_HEIGHT_CLEARANCE; dy++)
                {
                    BlockPos pos = corner.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.canBeReplaced())
                        return false;
                }
            }
        }
        return true;
    }

    private static void buildObsidianSupport(Level level, BlockPos corner)
    {
        for (int dx = 0; dx < PORTAL_SIZE; dx++)
        {
            for (int dz = 0; dz < PORTAL_SIZE; dz++)
            {
                for (BlockPos pos = corner.offset(dx, -1, dz); pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos.below()).isAir(); pos = pos.below())
                    level.setBlock(pos.below(), Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
        }
    }

    private static void spawnTeleporterIfAbsent(Level level, BlockPos lowerLeftCorner, @Nullable BlockPos target)
    {
        if (hasTeleporter(level, lowerLeftCorner))
            return;

        TeleporterEntity entity = new TeleporterEntity(ApocalypseContent.teleporter.get(), level);
        entity.setPortal(lowerLeftCorner, target);
        level.addFreshEntity(entity);
    }

    private static boolean hasTeleporter(Level level, BlockPos lowerLeftCorner)
    {
        AABB box = new AABB(lowerLeftCorner).inflate(5.0D);
        return !level.getEntitiesOfClass(TeleporterEntity.class, box, entity -> lowerLeftCorner.equals(entity.getLowerLeftCorner())).isEmpty();
    }

    private static boolean isPowerCube(Level level, BlockPos pos)
    {
        return level.getBlockState(pos).is(ApocalypseContent.blockPowerCube.get());
    }
}
