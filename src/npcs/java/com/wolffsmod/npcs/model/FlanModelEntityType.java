package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.IContentType;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;

/**
 * Entity type standing for one content-pack definition, so that it gets its own entry in the
 * Custom NPCs model list. It cannot be summoned, as Custom NPCs only creates it as a detached model
 * entity of an NPC, but it stays serializable: Custom NPCs expects its entities to have an encode id.
 */
public class FlanModelEntityType extends EntityType<FlanModelEntity>
{
    @Getter
    private final String shortName;
    @Getter
    private final FlanModelKind kind;
    @Getter
    private final FlanModelShape shape;
    @Nullable
    private String descriptionId;

    public FlanModelEntityType(String shortName, FlanModelKind kind, FlanModelShape shape)
    {
        super(FlanModelEntity::new, MobCategory.MISC, true, false, true, false, ImmutableSet.of(),
            shape.dimensions(), 1F, 8, 3, FeatureFlags.VANILLA_SET);
        this.shortName = shortName;
        this.kind = kind;
        this.shape = shape;
    }

    @Nullable
    public IContentType getInfoType()
    {
        return FlansModApi.getType(shortName).orElse(null);
    }

    /** Named like the definition's item, so the model list shows the localized vehicle name. */
    @Override
    @NotNull
    public String getDescriptionId()
    {
        if (descriptionId == null)
        {
            IContentType type = getInfoType();
            Item item = type != null ? type.getItem() : null;
            if (item == null)
                return super.getDescriptionId();
            descriptionId = item.getDescriptionId();
        }
        return descriptionId;
    }
}
