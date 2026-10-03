package com.flansmodultimate.platform.entity;

import org.jetbrains.annotations.NotNull;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Keeps the version-specific synchronized-data lifecycle out of gameplay entities. */
public abstract class FlanEntity extends Entity
{
    protected FlanEntity(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    @Override
    protected final void defineSynchedData(@NotNull SynchedEntityData.Builder builder)
    {
        defineEntityData(new SynchedDataDefinition(builder));
    }

    protected void defineEntityData(SynchedDataDefinition data) {}

    @Override
    public final void lerpTo(double x, double y, double z, float yaw, float pitch, int steps)
    {
        lerpEntity(x, y, z, yaw, pitch, steps, false);
    }

    /** Shared interpolation hook; only 1.20.1 supplies an explicit teleport flag. */
    protected void lerpEntity(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport)
    {
        super.lerpTo(x, y, z, yaw, pitch, steps);
    }

    @Override
    public final void onAddedToLevel()
    {
        super.onAddedToLevel();
        onEntityAdded();
    }

    protected void onEntityAdded() {}
}
