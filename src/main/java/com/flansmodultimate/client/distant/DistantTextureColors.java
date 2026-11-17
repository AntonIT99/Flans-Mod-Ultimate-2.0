package com.flansmodultimate.client.distant;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.util.FlansLog;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** The average colour of a driveable's skin, which is all of it a distant box shape can show. Render thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantTextureColors
{
    /** Olive grey, for a skin that cannot be read. */
    private static final int FALLBACK = 0xFF5E6150;
    /** Remembered for a skin that cannot be read, so that it is not opened again; no average is transparent. */
    private static final int UNREADABLE = 0;
    /** Pixels sampled along each side of a skin. */
    private static final int SAMPLES_PER_SIDE = 64;
    private static final Map<ResourceLocation, Integer> colors = new HashMap<>();

    /** Opaque average colour of the skin a driveable of this type and paintjob is drawn with. */
    public static int of(DriveableType type, int paintjobId)
    {
        Paintjob paintjob = type.getPaintjob(paintjobId);
        ResourceLocation texture = paintjob != null && paintjob.getTexture() != null ? paintjob.getTexture() : type.getTexture();
        return texture == null || FlansModTextures.FALLBACK_TEXTURE.equals(texture) ? FALLBACK : sample(texture, FALLBACK);
    }

    /**
     * Opaque average colour of the skin an entity standing in for a driveable of this type is drawn with: the type's own
     * skin when the entity has none or its skin cannot be read, as one downloaded at run time.
     */
    public static int of(DriveableType type, @Nullable ResourceLocation skin)
    {
        int own = of(type, 0);
        return skin == null ? own : sample(skin, own);
    }

    private static int sample(ResourceLocation texture, int unreadable)
    {
        int color = colors.computeIfAbsent(texture, DistantTextureColors::read);
        return color == UNREADABLE ? unreadable : color;
    }

    public static void clear()
    {
        colors.clear();
    }

    private static int read(ResourceLocation texture)
    {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty())
            return UNREADABLE;

        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream))
        {
            long red = 0;
            long green = 0;
            long blue = 0;
            long count = 0;
            int stepX = Math.max(1, image.getWidth() / SAMPLES_PER_SIDE);
            int stepY = Math.max(1, image.getHeight() / SAMPLES_PER_SIDE);
            for (int y = 0; y < image.getHeight(); y += stepY)
            {
                for (int x = 0; x < image.getWidth(); x += stepX)
                {
                    // Native images hold ABGR; unused atlas space is transparent and must not grey the average
                    int pixel = image.getPixelRGBA(x, y);
                    if (FastColor.ABGR32.alpha(pixel) < 128)
                        continue;
                    red += FastColor.ABGR32.red(pixel);
                    green += FastColor.ABGR32.green(pixel);
                    blue += FastColor.ABGR32.blue(pixel);
                    count++;
                }
            }
            if (count == 0)
                return UNREADABLE;
            return 0xFF000000 | (int) (red / count) << 16 | (int) (green / count) << 8 | (int) (blue / count);
        }
        catch (Exception exception)
        {
            FlansLog.log.debug("Could not read {} for its distant colour", texture, exception);
            return UNREADABLE;
        }
    }
}
