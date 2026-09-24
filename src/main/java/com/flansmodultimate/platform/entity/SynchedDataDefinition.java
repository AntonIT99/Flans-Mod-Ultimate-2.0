package com.flansmodultimate.platform.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * Version boundary for declaring an entity's synchronized data. 1.20.1 defines accessors directly on
 * {@link SynchedEntityData}; 1.21 defines them on its builder.
 */
public final class SynchedDataDefinition
{
    private final SynchedEntityData data;

    public SynchedDataDefinition(SynchedEntityData data)
    {
        this.data = data;
    }

    public <T> void define(EntityDataAccessor<T> accessor, T value)
    {
        data.define(accessor, value);
    }
}
