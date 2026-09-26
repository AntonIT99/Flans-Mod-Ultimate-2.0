package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.api.IDriveableType;
import com.flansmodultimate.api.IMechaType;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * Hitbox and resting height of a content-pack model, read from its definition.
 *
 * <p>The NPC stands where the driveable itself would rest on its wheels, but never sinks below
 * its feet, since flying boats rest on their hull. The hitbox reaches from the ground to the top
 * of the core part, as wide as the core part is long, up to {@link #MAX_WIDTH}: a vanilla hitbox
 * cannot turn with the NPC, and a square as long as a ship would cover far more than the ship and
 * could not path. The main mod keeps its own driveable entity collision within the same width.
 * Mechas use their declared size. AA guns declare no size and keep the defaults of their kind.</p>
 *
 * @param modelHeight blocks between the NPC's feet and the model origin
 */
public record FlanModelShape(EntityDimensions dimensions, float modelHeight)
{
    private static final float MIN_SIZE = 0.1F;
    /** {@code Driveable#getDimensions} clamps the entity collision width the same way. */
    private static final float MAX_WIDTH = 4F;

    public static FlanModelShape of(IContentType type, FlanModelKind kind)
    {
        if (type instanceof IMechaType mechaType)
            return new FlanModelShape(scalable(mechaType.getWidth(), mechaType.getHeight()), 0F);
        if (type instanceof IDriveableType driveableType)
            return ofDriveable(driveableType, kind);
        return new FlanModelShape(kind.getDimensions(), kind.getModelHeight());
    }

    private static FlanModelShape ofDriveable(IDriveableType type, FlanModelKind kind)
    {
        float modelHeight = Math.max(0F, type.getRestingHeight());
        Optional<AABB> bounds = type.getCoreBounds().or(type::getBounds);
        if (bounds.isEmpty())
            return new FlanModelShape(kind.getDimensions(), modelHeight);

        AABB box = bounds.get();
        return new FlanModelShape(scalable(Math.max(box.getXsize(), box.getZsize()), modelHeight + box.maxY), modelHeight);
    }

    private static EntityDimensions scalable(double width, double height)
    {
        return EntityDimensions.scalable((float) Math.max(MIN_SIZE, Math.min(MAX_WIDTH, width)), (float) Math.max(MIN_SIZE, height));
    }
}
