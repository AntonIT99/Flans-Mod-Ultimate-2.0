package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.IContentType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
    public static final String PAINTJOB_KEY = "FlanPaintjob";
    private int paintjobId;

    public int getPaintjobId()
    {
        return paintjobId;
    }

    @Nullable
    public ResourceLocation getModelTexture()
    {
        IContentType definition = getInfoType();
        if (definition == null)
            return null;
        return definition.getPaintjobVariants().stream()
            .filter(job -> job.id() == paintjobId && job.texture() != null)
            .map(job -> job.texture()).findFirst().orElse(definition.getTexture());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        paintjobId = Math.max(0, tag.getInt(PAINTJOB_KEY));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(PAINTJOB_KEY, paintjobId);
    }

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
