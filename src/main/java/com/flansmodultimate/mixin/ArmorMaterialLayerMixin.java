package com.flansmodultimate.mixin;

import com.flansmodultimate.FlansMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;

@Mixin(ArmorMaterial.Layer.class)
public abstract class ArmorMaterialLayerMixin
{
    private static final String VANILLA_ARMOR_TEXTURE_PREFIX = "textures/models/armor/";
    private static final String FLANS_ARMOR_TEXTURE_PREFIX = "textures/armor/";
    private static final String VANILLA_LAYER_SUFFIX = "_layer_";

    @Inject(method = "texture", at = @At("RETURN"), cancellable = true)
    private void useLegacyArmorTextureFolder(boolean innerModel, CallbackInfoReturnable<ResourceLocation> cir)
    {
        ResourceLocation texture = cir.getReturnValue();
        if (texture.getNamespace().equals(FlansMod.MOD_ID) && texture.getPath().startsWith(VANILLA_ARMOR_TEXTURE_PREFIX))
        {
            cir.setReturnValue(texture.withPath(path -> {
                String fileName = path.substring(VANILLA_ARMOR_TEXTURE_PREFIX.length());
                int layerSuffix = fileName.lastIndexOf(VANILLA_LAYER_SUFFIX);
                if (layerSuffix >= 0)
                    fileName = fileName.substring(0, layerSuffix) + "_" + fileName.substring(layerSuffix + VANILLA_LAYER_SUFFIX.length());
                return FLANS_ARMOR_TEXTURE_PREFIX + fileName;
            }));
        }
    }
}
