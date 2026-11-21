package com.flansmodultimate.client.model;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.content.ContentManager;
import com.flansmodultimate.util.FlansLog;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Lists the content-pack item icons ({@code flansmod:item/...}). The item icon atlas uses it with the configured size
 * limit. The block atlas uses it as a {@code fallback} after filtering the icons out, keeping 16 pixel copies for the
 * particles and block models that still sample icons there. Item holder icons keep their size there, since item holder
 * blocks draw them on their faces in the world.
 */
public final class ItemIconSpriteSource implements SpriteSource
{
    public static final Codec<ItemIconSpriteSource> CODEC = RecordCodecBuilder
        .create(instance -> instance.group(Codec.BOOL.optionalFieldOf("fallback", false).forGetter(source -> source.fallback)).apply(instance, ItemIconSpriteSource::new));
    private static final FileToIdConverter ICON_FILES = new FileToIdConverter("textures/item", ".png");
    private static final String ID_PREFIX = "item/";

    private final boolean fallback;

    private ItemIconSpriteSource(boolean fallback)
    {
        this.fallback = fallback;
    }

    @Override
    public void run(@NotNull ResourceManager resourceManager, @NotNull Output output)
    {
        int limit = fallback ? ItemIconAtlas.FALLBACK_ICON_SIZE : ItemIconAtlas.configuredIconSizeLimit();
        Set<String> fullSize = fallback ? ContentManager.itemHolderIcons() : Set.of();
        Set<ResourceLocation> listed = new HashSet<>();
        ICON_FILES.listMatchingResources(resourceManager).forEach((file, resource) ->
        {
            if (!FlansMod.FLANSMOD_ID.equals(file.getNamespace()))
                return;
            ResourceLocation name = ICON_FILES.fileToId(file);
            ResourceLocation id = name.withPrefix(ID_PREFIX);
            listed.add(id);
            if (limit <= 0 || fullSize.contains(name.getPath()))
                output.add(id, resource);
            else
                output.add(id, () -> load(id, resource, limit, fallback));
        });
        if (!fallback)
            ItemIconAtlas.setListedIcons(listed);
    }

    @Override
    @NotNull
    public SpriteSourceType type()
    {
        return ItemIconAtlas.spriteSourceType();
    }

    /** Icons with metadata (animations, custom loaders) keep the vanilla path and their size. */
    @Nullable
    // The Forge overload only adds custom-loader metadata, and icons with metadata never get here.
    @SuppressWarnings("deprecation")
    private static SpriteContents load(ResourceLocation id, Resource resource, int limit, boolean fallback)
    {
        try
        {
            if (resource.metadata() != ResourceMetadata.EMPTY)
                return SpriteLoader.loadSprite(id, resource);
        }
        catch (IOException e)
        {
            return SpriteLoader.loadSprite(id, resource);
        }

        NativeImage image;
        try (InputStream input = resource.open())
        {
            image = NativeImage.read(input);
        }
        catch (IOException e)
        {
            FlansLog.log.error("Using missing texture, unable to load {}: {}", id, e.toString());
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        if (Math.max(width, height) > limit)
        {
            // Block atlas copies stay square at the limit, so an odd icon size cannot lower that atlas's mipmap level
            int targetWidth = fallback ? limit : Math.max(1, Math.round(width * (float) limit / Math.max(width, height)));
            int targetHeight = fallback ? limit : Math.max(1, Math.round(height * (float) limit / Math.max(width, height)));
            NativeImage scaled = downscale(image, targetWidth, targetHeight);
            image.close();
            image = scaled;
            FlansLog.log.debug("Downscaled item icon {} from {}x{} to {}x{}", id, width, height, targetWidth, targetHeight);
        }
        return new SpriteContents(id, new FrameSize(image.getWidth(), image.getHeight()), image, AnimationMetadataSection.EMPTY);
    }

    /** Box-filters the image, weighting colours by alpha so transparent pixels do not darken the icon's edges. */
    static NativeImage downscale(NativeImage source, int width, int height)
    {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        NativeImage result = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++)
        {
            int y0 = y * sourceHeight / height;
            int y1 = Math.max(y0 + 1, (y + 1) * sourceHeight / height);
            for (int x = 0; x < width; x++)
            {
                int x0 = x * sourceWidth / width;
                int x1 = Math.max(x0 + 1, (x + 1) * sourceWidth / width);
                result.setPixelRGBA(x, y, average(source, x0, x1, y0, y1));
            }
        }
        return result;
    }

    /** Pixels are packed ABGR, as {@link NativeImage#getPixelRGBA} returns them. */
    private static int average(NativeImage source, int x0, int x1, int y0, int y1)
    {
        long alpha = 0;
        long red = 0;
        long green = 0;
        long blue = 0;
        long plainRed = 0;
        long plainGreen = 0;
        long plainBlue = 0;
        for (int y = y0; y < y1; y++)
        {
            for (int x = x0; x < x1; x++)
            {
                int pixel = source.getPixelRGBA(x, y);
                int a = pixel >>> 24;
                int r = pixel & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = (pixel >> 16) & 0xFF;
                alpha += a;
                red += (long) r * a;
                green += (long) g * a;
                blue += (long) b * a;
                plainRed += r;
                plainGreen += g;
                plainBlue += b;
            }
        }
        int count = (x1 - x0) * (y1 - y0);
        int r;
        int g;
        int b;
        if (alpha == 0)
        {
            r = (int) (plainRed / count);
            g = (int) (plainGreen / count);
            b = (int) (plainBlue / count);
        }
        else
        {
            r = (int) (red / alpha);
            g = (int) (green / alpha);
            b = (int) (blue / alpha);
        }
        int a = (int) ((alpha + count / 2) / count);
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
