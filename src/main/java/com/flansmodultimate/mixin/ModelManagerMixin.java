package com.flansmodultimate.mixin;

import com.flansmodultimate.client.model.ItemIconAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Hooks for {@link ModelManager}.
 * <ul>
 * <li>Item icon atlas: adds the atlas beside the vanilla ones and bakes Flan item models against it.</li>
 * </ul>
 */
@Mixin(ModelManager.class)
public abstract class ModelManagerMixin
{
    @ModifyArg(method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/AtlasSet;<init>(Ljava/util/Map;Lnet/minecraft/client/renderer/texture/TextureManager;)V"), index = 0)
    private Map<ResourceLocation, ResourceLocation> flansmodultimateAddItemIconAtlas(Map<ResourceLocation, ResourceLocation> atlases)
    {
        return ItemIconAtlas.withIconAtlas(atlases);
    }

    @ModifyArg(method = "loadModels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelBakery;bakeModels(Ljava/util/function/BiFunction;)V"))
    private BiFunction<ResourceLocation, Material, TextureAtlasSprite> flansmodultimateBakeItemIcons(BiFunction<ResourceLocation, Material, TextureAtlasSprite> sprites)
    {
        return ItemIconAtlas.iconSpriteGetter(sprites);
    }
}
