package com.flansmodultimate.platform.entity;

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
    protected final void defineSynchedData()
    {
        defineEntityData(new SynchedDataDefinition(entityData));
    }

    protected void defineEntityData(SynchedDataDefinition data)
    {}

    @Override
    public final void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport)
    {
        lerpEntity(x, y, z, yaw, pitch, steps, teleport);
    }

    /** Shared interpolation hook; only 1.20.1 supplies an explicit teleport flag. */
    protected void lerpEntity(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport)
    {
        super.lerpTo(x, y, z, yaw, pitch, steps, teleport);
    }

    @Override
    public final void onAddedToWorld()
    {
        super.onAddedToWorld();
        onEntityAdded();
    }

    protected void onEntityAdded()
    {}
}
