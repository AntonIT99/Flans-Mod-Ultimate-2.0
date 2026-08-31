package com.flansmodultimate.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.FlansMod;
import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Corrects models whose declared texture size does not match the texture they are drawn with.
 * <p>
 * Model classes hardcode the texture size they were exported against and bake their UVs against
 * it. When a pack ships a texture of a different size (typically a wrong height, e.g. a model
 * declaring 1024x1024 for a 1024x64 image), the UVs are off by a constant factor on that axis.
 * The real image size is read once per texture and the model UVs are re-normalised against it, so
 * the pixel positions written in the model class keep pointing at the same place on the texture.
 *
 * @see ModelRendererTurbo#applyActualTextureSize(float, float)
 */
@OnlyIn(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelTextureFitter
{
    private record TextureSize(int width, int height) {}

    private static final Map<ResourceLocation, Optional<TextureSize>> textureSizes = new ConcurrentHashMap<>();

    public static void clear()
    {
        textureSizes.clear();
    }

    /**
     * Re-normalises every part of the model against the real size of the given texture.
     *
     * @param model   the model to correct, may be {@code null}
     * @param texture the texture the model is rendered with, may be {@code null} or empty
     */
    public static void fitToTexture(@Nullable IModelBase model, @Nullable ResourceLocation texture)
    {
        if (model == null || texture == null || texture.getPath().isEmpty())
            return;

        TextureSize size = getTextureSize(texture);
        if (size == null)
            return;

        boolean rescaled = false;
        for (ModelRenderer modelRenderer : model.getBoxList())
        {
            if (modelRenderer instanceof ModelRendererTurbo modelRendererTurbo
                && modelRendererTurbo.applyActualTextureSize(size.width(), size.height()))
            {
                rescaled = true;
            }
        }

        if (rescaled)
        {
            FlansMod.log.warn("Model {} declares a texture size that does not match {} ({}x{}). Its texture coordinates have been rescaled.",
                model.getClass().getName(), texture, size.width(), size.height());
        }
    }

    @Nullable
    private static TextureSize getTextureSize(ResourceLocation texture)
    {
        return textureSizes.computeIfAbsent(texture, ModelTextureFitter::readTextureSize).orElse(null);
    }

    private static Optional<TextureSize> readTextureSize(ResourceLocation texture)
    {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty())
            return Optional.empty();

        // Only the image header is decoded, the pixels are never read.
        try (InputStream stream = resource.get().open();
             ImageInputStream imageInput = ImageIO.createImageInputStream(stream))
        {
            if (imageInput == null)
                return Optional.empty();

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext())
                return Optional.empty();

            ImageReader reader = readers.next();
            try
            {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return width > 0 && height > 0 ? Optional.of(new TextureSize(width, height)) : Optional.empty();
            }
            finally
            {
                reader.dispose();
            }
        }
        catch (Exception e)
        {
            FlansMod.log.warn("Could not read the size of texture {}: {}", texture, e.getMessage());
            return Optional.empty();
        }
    }
}
