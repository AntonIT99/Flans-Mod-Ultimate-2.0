package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
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
 * <p>
 * Custom NPCs creates it detached from the level, copies the NPC's position and rotation into it
 * and renders it in place of the NPC's body. It carries no behaviour of its own.
 * </p>
 */
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FlanModelEntity extends Mob implements ILivingVisualEffects
{
    public static final String PAINTJOB_KEY = "FlanPaintjob";
    private int paintjobId;
    private boolean hurtFlash = true;
    private boolean deathRotation = true;

    public void setVisualEffects(boolean hurt, boolean death)
    {
        hurtFlash = hurt;
        deathRotation = death;
    }

    @Override
    public boolean flansmodultimateHurtFlash()
    {
        return hurtFlash;
    }

    @Override
    public boolean flansmodultimateDeathRotation()
    {
        return deathRotation;
    }

    @Nullable
    public ResourceLocation getModelTexture()
    {
        IContentType definition = getInfoType();
        if (definition == null)
            return null;
        return definition.getPaintjobVariants().stream().filter(job -> job.id() == paintjobId && job.texture() != null).map(PaintjobVariant::texture).findFirst().orElse(definition.getTexture());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        paintjobId = Math.max(0, tag.getInt(PAINTJOB_KEY));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
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
