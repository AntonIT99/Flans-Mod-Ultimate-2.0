package com.wolffsarmormod.util;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.platform.NativeImage;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.types.InfoType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

public class ResourceUtils
{
    private ResourceUtils() {}

    public static class ItemModel
    {
        String parent;
        @SerializedName("gui_light")
        String guiLight;
        Textures textures;

        protected ItemModel (String parent, @Nullable String guiLight, Textures textures)
        {
            this.parent = parent;
            this.guiLight = guiLight;
            this.textures = textures;
        }

        public static ItemModel create(InfoType config)
        {
            return new ItemModel("item/generated", null, new ResourceUtils.Textures(ArmorMod.FLANSMOD_ID + ":item/" + config.getIcon()));
        }
    }

    public static class Textures
    {
        String layer0;

        protected Textures(String layer0)
        {
            this.layer0 = layer0;
        }
    }

    @Nullable
    public static ResourceLocation loadExternalTexture(Path path, String resourceLocPath, String itemName)
    {
        try
        {
            File file = path.toFile();
            if (!file.exists())
            {
                ArmorMod.log.error("File not found: {}", path);
                return null;
            }

            FileInputStream fileInputStream = new FileInputStream(file);
            NativeImage nativeImage = NativeImage.read(fileInputStream);
            fileInputStream.close();

            DynamicTexture texture = new DynamicTexture(nativeImage);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ArmorMod.MOD_ID, resourceLocPath + itemName);
            Minecraft.getInstance().getTextureManager().register(location, texture);

            return location;
        }
        catch (Exception e)
        {
            ArmorMod.log.error("Failed to load file: {}", path);
            return null;
        }
    }
}
