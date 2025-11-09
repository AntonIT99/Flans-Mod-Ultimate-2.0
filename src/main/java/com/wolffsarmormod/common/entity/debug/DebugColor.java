package com.wolffsarmormod.common.entity.debug;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Class Skeleton for DebugEntities which use a color
 */
public abstract class DebugColor extends Entity
{
    public static final float DEFAULT_HITBOX_SIZE = 0.25F;
    public static final int RENDER_DISTANCE = 128;

    private static final EntityDataAccessor<Float> COLOR_RED = SynchedEntityData.defineId(DebugColor.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_GREEN = SynchedEntityData.defineId(DebugColor.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_BLUE = SynchedEntityData.defineId(DebugColor.class, EntityDataSerializers.FLOAT);

    protected int life = 1000;

    protected DebugColor(EntityType<? extends DebugColor> type, Level level)
    {
        super(type, level);
    }

    protected DebugColor(EntityType<? extends DebugColor> type, Level level, Vec3 position, int lifeTime)
    {
        super(type, level);
        setPos(position);
        life = lifeTime;
    }

    @Override
    public void tick()
    {
        life--;
        if (life <= 0)
            discard();
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(COLOR_RED, 1F);
        entityData.define(COLOR_GREEN, 1F);
        entityData.define(COLOR_BLUE, 1F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        this.setColorRed(tag.getFloat("color_red"));
        this.setColorGreen(tag.getFloat("color_green"));
        this.setColorBlue(tag.getFloat("color_blue"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.putFloat("color_red", getColorRed());
        tag.putFloat("color_green", getColorGreen());
        tag.putFloat("color_blue", getColorBlue());
    }

    /**
     * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
     *
     * @param red Red color value
     */
    public void setColorRed(float red)
    {
        entityData.set(COLOR_RED, red);
    }

    /**
     * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
     *
     * @return Red color value
     */
    public float getColorRed()
    {
        return entityData.get(COLOR_RED);
    }

    /**
     * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
     *
     * @param green Green color value
     */
    public void setColorGreen(float green)
    {
        entityData.set(COLOR_GREEN, green);
    }

    /**
     * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
     *
     * @return Green color value
     */
    public float getColorGreen()
    {
        return entityData.get(COLOR_GREEN);
    }

    /**
     * Color values range from 0 (Nonexistend) to 1 (Fully Visible)
     *
     * @param blue Blue color value
     */
    public void setColorBlue(float blue)
    {
        entityData.set(COLOR_BLUE, blue);
    }

    /**
     * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
     *
     * @return Blue color value
     */
    public float getColorBlue()
    {
        return entityData.get(COLOR_BLUE);
    }

    /**
     * Combined Setter for all three color values
     * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
     *
     * @param red   Red color value
     * @param green Green color value
     * @param blue  Blue color value
     */
    public void setColor(float red, float green, float blue)
    {
        setColorRed(red);
        setColorGreen(green);
        setColorBlue(blue);
    }
}
