package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.IDriveableType;
import com.flansmodultimate.api.client.FlansModelPreviews;
import com.wolffsmod.npcs.model.FlanModelEntity;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/** Conservative static bounds relative to the model entity's feet. Client render thread only. */
public final class FlanModelBounds
{
    private FlanModelBounds() {}

    public static AABB of(FlanModelEntity model)
    {
        var type = model.getInfoType();
        var modelType = model.getModelType();
        float lift = modelType == null ? 0 : modelType.getShape().modelHeight();
        return combine(model.getBbWidth(), model.getBbHeight(), lift,
            type == null ? Optional.empty() : FlansModelPreviews.getBounds(type),
            type instanceof IDriveableType driveable ? driveable.getBounds() : Optional.empty());
    }

    static AABB combine(float width, float height, float lift, Optional<AABB> geometry, Optional<AABB> collision)
    {
        float halfWidth = width / 2;
        AABB bounds = new AABB(-halfWidth, 0, -halfWidth, halfWidth, height, halfWidth);
        if (geometry.isPresent())
            bounds = bounds.minmax(geometry.get().move(0, lift, 0));
        if (collision.isPresent())
            bounds = bounds.minmax(collision.get().move(0, lift, 0));
        return bounds;
    }
}
