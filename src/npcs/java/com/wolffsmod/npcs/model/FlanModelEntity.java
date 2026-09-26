package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.IContentType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Model entity of a Custom NPC that looks like a content-pack AA gun or driveable.
 *
 * <p>Custom NPCs creates it detached from the level, copies the NPC's position and rotation into it
 * and renders it in place of the NPC's body. It carries no behaviour of its own.</p>
 */
public class FlanModelEntity extends Mob
{
    public FlanModelEntity(EntityType<? extends FlanModelEntity> entityType, Level level)
    {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes();
    }

    @Nullable
    public FlanModelEntityType getModelType()
    {
        return getType() instanceof FlanModelEntityType modelType ? modelType : null;
    }

    @Nullable
    public IContentType getInfoType()
    {
        FlanModelEntityType modelType = getModelType();
        return modelType != null ? modelType.getInfoType() : null;
    }
}
