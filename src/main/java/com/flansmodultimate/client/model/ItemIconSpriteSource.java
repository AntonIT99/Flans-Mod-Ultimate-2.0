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
 * blocks draw them on their faces in the world. Both atlases load in parallel and share each icon's decoded pixels
 * through {@link ItemIconDecodes}, so an icon is usually decoded once.
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
        Set<String> fullSize = ContentManager.itemHolderIcons();
        ItemIconDecodes<NativeImage> decodes = ItemIconDecodes.forReload(resourceManager);
        Set<ResourceLocation> listed = new HashSet<>();
        ICON_FILES.listMatchingResources(resourceManager).forEach((file, resource) ->
        {
            if (!FlansMod.FLANSMOD_ID.equals(file.getNamespace()))
                return;
            ResourceLocation name = ICON_FILES.fileToId(file);
            ResourceLocation id = name.withPrefix(ID_PREFIX);
            listed.add(id);
            boolean blockFaces = fullSize.contains(name.getPath());
            if (fallback && blockFaces)
                output.add(id, resource);
            else if (fallback)
                output.add(id, () -> loadFallback(id, resource, decodes));
            else
                output.add(id, () -> loadIcon(id, resource, limit, !blockFaces, decodes));
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

    /** Icons with metadata (animations, custom loaders) keep the vanilla path, their size and their own decode. */
    private static boolean hasMetadata(Resource resource)
    {
        try
        {
            return resource.metadata() != ResourceMetadata.EMPTY;
        }
        catch (IOException e)
        {
            return true;
        }
    }

    @Nullable
    private static SpriteContents loadIcon(ResourceLocation id, Resource resource, int limit, boolean blockCopy, ItemIconDecodes<NativeImage> decodes)
    {
        if (hasMetadata(resource))
            return SpriteLoader.loadSprite(id, resource);
        NativeImage image = decodes.forIconAtlas(id, () -> decode(id, resource), blockCopy ? ItemIconAtlas.FALLBACK_ICON_SIZE : 0);
        if (image == null)
            return null;
        if (limit > 0 && Math.max(image.getWidth(), image.getHeight()) > limit)
        {
            float scale = (float) limit / Math.max(image.getWidth(), image.getHeight());
            NativeImage scaled = downscale(image, Math.max(1, Math.round(image.getWidth() * scale)), Math.max(1, Math.round(image.getHeight() * scale)));
            image.close();
            image = scaled;
            decodes.countDownscaled();
        }
        return contents(id, image);
    }

    @Nullable
    private static SpriteContents loadFallback(ResourceLocation id, Resource resource, ItemIconDecodes<NativeImage> decodes)
    {
        if (hasMetadata(resource))
            return SpriteLoader.loadSprite(id, resource);
        NativeImage image = decodes.forBlockAtlas(id, () -> decode(id, resource), ItemIconAtlas.FALLBACK_ICON_SIZE);
        return image == null ? null : contents(id, image);
    }

    // The Forge overload only adds custom-loader metadata, and icons with metadata never get here.
    @SuppressWarnings("deprecation")
    private static SpriteContents contents(ResourceLocation id, NativeImage image)
    {
        return new SpriteContents(id, new FrameSize(image.getWidth(), image.getHeight()), image, AnimationMetadataSection.EMPTY);
    }

    @Nullable
    private static NativeImage decode(ResourceLocation id, Resource resource)
    {
        try (InputStream input = resource.open())
        {
            return NativeImage.read(input);
        }
        catch (IOException e)
        {
            FlansLog.log.error("Using missing texture, unable to load {}: {}", id, e.toString());
            return null;
        }
    }

    /**
     * The square block atlas copy of an icon larger than {@code size}, which keeps an odd icon size from lowering that
     * atlas's mipmap level, or null when the icon is small enough to use as it is.
     */
    @Nullable
    static NativeImage blockCopy(NativeImage image, int size)
    {
        return Math.max(image.getWidth(), image.getHeight()) > size ? downscale(image, size, size) : null;
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
