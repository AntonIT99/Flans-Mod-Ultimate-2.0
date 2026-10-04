package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a content-pack model entity.
 *
 * <p>It has to be a living entity renderer: Custom NPCs only draws an NPC through the model and the
 * texture of such a renderer, and takes this texture as the NPC's skin when the model is picked.</p>
 */
public class FlanModelRenderer extends LivingEntityRenderer<FlanModelEntity, FlanModelEntityModel>
{
    public FlanModelRenderer(EntityRendererProvider.Context context, FlanModelEntityType entityType)
    {
        super(context, new FlanModelEntityModel(entityType), 0F);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull FlanModelEntity entity)
    {
        ResourceLocation texture = entity.getModelTexture();
        return texture != null ? texture : MissingTextureAtlasSprite.getLocation();
    }
}
