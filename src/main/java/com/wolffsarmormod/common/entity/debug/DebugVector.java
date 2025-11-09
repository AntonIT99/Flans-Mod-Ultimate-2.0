package com.wolffsarmormod.common.entity.debug;

import com.wolffsarmormod.ArmorMod;
import lombok.EqualsAndHashCode;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Entity for debugging purposes.
 * On the client side a line (Vector) between the position of the entity and its pointing location is rendered
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DebugVector extends DebugColor
{
    private static final EntityDataAccessor<Float> POINTING_X = SynchedEntityData.defineId(DebugVector.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> POINTING_Y = SynchedEntityData.defineId(DebugVector.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> POINTING_Z = SynchedEntityData.defineId(DebugVector.class, EntityDataSerializers.FLOAT);

    public DebugVector(EntityType<? extends DebugColor> type, Level level)
    {
        super(type, level);
    }

    /**
     * Spawns an EntityDebug Vector
     *
     * @param level Level for Entity Constructor
     * @param u Position where the Vector starts
     * @param v Position where the Vector ends
     * @param lifeTime Lifetime given in ticks
     * @param r Red Color Value
     * @param g Green Color Value
     * @param b Blue Color Value
     */
    public DebugVector(Level level, Vec3 u, Vec3 v, int lifeTime, float r, float g, float b)
    {
        super(ArmorMod.debugVectorEntity.get(), level, u, lifeTime);
        setPointing((float)v.x, (float)v.y, (float)v.z);
        setColor(r, g, b);
    }

    /**
     * @param level Level for Entity Constructor
     * @param u Position where the Vector starts
     * @param v Position where the Vector ends
     * @param lifeTime Lifetime given in ticks
     */
    public DebugVector(Level level, Vec3 u, Vec3 v, int lifeTime)
    {
        this(level, u, v, lifeTime, 1F, 1F, 1F);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(POINTING_X, 1F);
        entityData.define(POINTING_Y, 1F);
        entityData.define(POINTING_Z, 1F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        entityData.set(POINTING_X, tag.getFloat("pointing_x"));
        entityData.set(POINTING_Y, tag.getFloat("pointing_y"));
        entityData.set(POINTING_Z, tag.getFloat("pointing_z"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putFloat("pointing_x", getPointingX());
        tag.putFloat("pointing_y", getPointingY());
        tag.putFloat("pointing_z", getPointingZ());
    }

    /**
     * @param x The X value of the Position the Vector points to (Relative to Entity Position)
     */
    public void setPointingX(float x)
    {
        entityData.set(POINTING_X, x);
    }

    /**
     * @return The X value of the Position the Vector points to (Relative to Entity Position)
     */
    public float getPointingX()
    {
        return entityData.get(POINTING_X);
    }

    /**
     * @param y The Y value of the Position the Vector points to (Relative to Entity Position)
     */
    public void setPointingY(float y)
    {
        entityData.set(POINTING_Y, y);
    }

    /**
     * @return The Y value of the Position the Vector points to (Relative to Entity Position)
     */
    public float getPointingY()
    {
        return entityData.get(POINTING_Y);
    }

    /**
     * @param z The Z value of the Position the Vector points to (Relative to Entity Position)
     */
    public void setPointingZ(float z)
    {
        entityData.set(POINTING_Z, z);
    }

    /**
     * @return The Z value of the Position the Vector points to (Relative to Entity Position)
     */
    public float getPointingZ()
    {
        return entityData.get(POINTING_Z);
    }

    /**
     * All Parameters are relative to the position of the Entity.
     * These 3 Parameters describe the location of the Position the Vector points to.
     *
     * @param x The X Coordinate
     * @param y The Y Coordinate
     * @param z The Z Coordinate
     */
    public void setPointing(float x, float y, float z)
    {
        setPointingX(x);
        setPointingY(y);
        setPointingZ(z);
    }
}
