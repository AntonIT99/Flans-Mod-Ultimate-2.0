package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.apocalyse.common.world.ApocalypsePortalManager;
import com.flansmodultimate.config.ModApocalypseConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TeleporterEntity extends Entity
{
    private static final String NBT_X = "x";
    private static final String NBT_Y = "y";
    private static final String NBT_Z = "z";
    private static final String NBT_TARGET_X = "target_x";
    private static final String NBT_TARGET_Y = "target_y";
    private static final String NBT_TARGET_Z = "target_z";
    @Getter
    private BlockPos lowerLeftCorner = BlockPos.ZERO;
    @Nullable
    private BlockPos targetTeleporter;

    public TeleporterEntity(EntityType<? extends TeleporterEntity> type, Level level)
    {
        super(type, level);
        noPhysics = true;
    }

    public void setPortal(BlockPos lowerLeftCorner, @Nullable BlockPos targetTeleporter)
    {
        this.lowerLeftCorner = lowerLeftCorner.immutable();
        this.targetTeleporter = targetTeleporter == null ? null : targetTeleporter.immutable();
        setPos(lowerLeftCorner.getX() + 2.0D, lowerLeftCorner.getY() + 0.5D, lowerLeftCorner.getZ() + 2.0D);
    }

    public Optional<BlockPos> getTargetTeleporter()
    {
        return Optional.ofNullable(targetTeleporter);
    }

    public void setTargetTeleporter(BlockPos targetTeleporter)
    {
        this.targetTeleporter = targetTeleporter.immutable();
    }

    @Override
    public void tick()
    {
        super.tick();

        if (lowerLeftCorner == null || lowerLeftCorner.equals(BlockPos.ZERO))
            lowerLeftCorner = blockPosition().offset(-2, 0, -2);

        Level level = level();

        if (!level.isClientSide)
        {
            if (!ModApocalypseConfig.apocalypsePortalsEnabled() || !ApocalypsePortalManager.isPortalFrame(level(), lowerLeftCorner))
            {
                discard();
                return;
            }

            AABB touchBox = getBoundingBox().inflate(0.25D, 1.0D, 0.25D);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, touchBox, p -> !p.isSpectator() && !p.isOnPortalCooldown()))
                ApocalypsePortalManager.teleportPlayer(player, this);
        }
        else
        {
            spawnPortalParticles(level);
        }
    }

    private void spawnPortalParticles(Level level)
    {
        for (int i = 0; i < 6; i++)
        {
            double dx = random.nextGaussian();
            double dy = random.nextGaussian() * 0.5D;
            double dz = random.nextGaussian();
            level.addParticle(ParticleTypes.PORTAL, getX() + dx, getY() + 1.0D + dy, getZ() + dz, dx, dy, dz);
        }
    }

    @Override
    protected void defineSynchedData()
    {
        // No synced fields. The entity is fully reconstructed from save/spawn position.
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        lowerLeftCorner = new BlockPos(tag.getInt(NBT_X), tag.getInt(NBT_Y), tag.getInt(NBT_Z));
        if (tag.contains(NBT_TARGET_X)) targetTeleporter = new BlockPos(tag.getInt(NBT_TARGET_X), tag.getInt(NBT_TARGET_Y), tag.getInt(NBT_TARGET_Z));
        setPos(lowerLeftCorner.getX() + 2.0D, lowerLeftCorner.getY() + 0.5D, lowerLeftCorner.getZ() + 2.0D);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        tag.putInt(NBT_X, lowerLeftCorner.getX()); tag.putInt(NBT_Y, lowerLeftCorner.getY()); tag.putInt(NBT_Z, lowerLeftCorner.getZ());
        if (targetTeleporter != null)
        {
            tag.putInt(NBT_TARGET_X, targetTeleporter.getX()); tag.putInt(NBT_TARGET_Y, targetTeleporter.getY()); tag.putInt(NBT_TARGET_Z, targetTeleporter.getZ());
        }
    }
}
