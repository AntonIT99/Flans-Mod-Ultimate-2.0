package com.wolffsarmormod.common.entity.debug;

import com.wolffsarmormod.ArmorMod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DebugDot extends DebugColor
{
    public DebugDot(EntityType<? extends DebugColor> type, Level level)
    {
        super(type, level);
    }

    /**
     * Creates a white dot at the given location
     *
     * @param level   World for Entity Constructor
     * @param pos Position of the dot
     * @param lifeTime   Lifetime given in ticks
     */
    public DebugDot(Level level, Vec3 pos, int lifeTime)
    {
        this(level, pos, lifeTime, 1F, 1F, 1F);
    }

    /**
     * Creates a dot
     * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
     *
     * @param level   World for Entity Constructor
     * @param pos Position of the dot
     * @param lifeTime   Lifetime given in ticks
     * @param r   Red color value
     * @param g   Green color value
     * @param b   Blue color value
     */
    public DebugDot(Level level, Vec3 pos, int lifeTime, float r, float g, float b)
    {
        super(ArmorMod.debugDotEntity.get(), level, pos, lifeTime);
        setColor(r, g, b);
    }
}
