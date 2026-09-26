package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.IAAGunType;
import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.api.IDriveableType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.EntityDimensions;

/**
 * The content-pack definitions a Custom NPC can take the shape of.
 *
 * <p>The dimensions, from the main mod's entity registrations, and the model heights, from the
 * previous NPC Vehicles mod, only apply where {@link FlanModelShape} finds nothing in the
 * definition to measure.</p>
 */
public enum FlanModelKind
{
    AA_GUN(EntityDimensions.scalable(2F, 2F), 0F),
    VEHICLE(EntityDimensions.scalable(2.5F, 2F), 0.625F),
    PLANE(EntityDimensions.scalable(3F, 2F), 0.625F),
    MECHA(EntityDimensions.scalable(2F, 4F), 0F);

    private final EntityDimensions dimensions;
    private final float modelHeight;

    FlanModelKind(EntityDimensions dimensions, float modelHeight)
    {
        this.dimensions = dimensions;
        this.modelHeight = modelHeight;
    }

    @Nullable
    public static FlanModelKind of(IContentType type)
    {
        if (type instanceof IAAGunType)
            return AA_GUN;
        if (type instanceof IDriveableType driveableType)
        {
            return switch (driveableType.getDriveableKind())
            {
                case VEHICLE -> VEHICLE;
                case PLANE -> PLANE;
                case MECHA -> MECHA;
            };
        }
        return null;
    }

    public EntityDimensions getDimensions()
    {
        return dimensions;
    }

    /** Blocks between the NPC's feet and the model origin. */
    public float getModelHeight()
    {
        return modelHeight;
    }
}
