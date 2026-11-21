package com.flansmodultimate.client.model;

import com.flansmodultimate.util.FlansLog;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shares each icon's decoded pixels between the item icon atlas and the block atlas within one resource reload. Both
 * atlases load in parallel; whichever reaches an icon first decodes it and leaves the other its part: the full image
 * for the icon atlas, or the small copy for the block atlas. Neither ever waits, so a race only costs a second decode.
 */
final class ItemIconDecodes<I>
{
    private static final Object ICON_ATLAS_CLAIM = new Object();
    private static final Object BLOCK_ATLAS_CLAIM = new Object();
    private static final Object DONE = new Object();
    private static final AtomicReference<ItemIconDecodes<NativeImage>> current = new AtomicReference<>();

    @Nullable
    private final ResourceManager resourceManager;
    private final BiFunction<I, Integer, I> blockCopy;
    private final Consumer<I> close;
    private final Map<ResourceLocation, Object> states = new ConcurrentHashMap<>();
    private final AtomicInteger shared = new AtomicInteger();
    private final AtomicInteger downscaled = new AtomicInteger();

    /** An image one atlas decoded and left for the other: the full icon, or the block atlas copy. */
    private record Left<I>(I image, boolean full)
    {}

    /** {@code blockCopy} returns the square copy for a size, or null when the image is no larger than it. */
    ItemIconDecodes(@Nullable ResourceManager resourceManager, BiFunction<I, Integer, I> blockCopy, Consumer<I> close)
    {
        this.resourceManager = resourceManager;
        this.blockCopy = blockCopy;
        this.close = close;
    }

    /** Both sources of one reload receive the same resource manager, so it identifies the reload. */
    static ItemIconDecodes<NativeImage> forReload(ResourceManager resourceManager)
    {
        while (true)
        {
            ItemIconDecodes<NativeImage> decodes = current.get();
            if (decodes != null && decodes.resourceManager == resourceManager)
                return decodes;
            ItemIconDecodes<NativeImage> next = new ItemIconDecodes<>(resourceManager, ItemIconSpriteSource::blockCopy, NativeImage::close);
            if (current.compareAndSet(decodes, next))
            {
                // A reload that failed before baking never finished its own
                if (decodes != null)
                    decodes.closeLeftovers();
                return next;
            }
        }
    }

    /** Called once every atlas of the reload is stitched: frees what neither atlas took and logs the outcome. */
    static void finishReload()
    {
        ItemIconDecodes<NativeImage> decodes = current.getAndSet(null);
        if (decodes == null)
            return;
        decodes.closeLeftovers();
        FlansLog.log.debug("Item icon atlas: {} icon decode(s) shared with the block atlas, {} icon(s) scaled down to the size limit", decodes.shared.get(), decodes.downscaled.get());
    }

    int sharedDecodes()
    {
        return shared.get();
    }

    void countDownscaled()
    {
        downscaled.incrementAndGet();
    }

    /** The full icon for the icon atlas; when this atlas decodes first, it leaves a copy of {@code copySize} for the block atlas. */
    @Nullable
    I forIconAtlas(ResourceLocation id, Supplier<I> decoder, int copySize)
    {
        Object previous = states.putIfAbsent(id, ICON_ATLAS_CLAIM);
        if (previous instanceof Left<?> left && left.full() && states.remove(id, previous))
        {
            shared.incrementAndGet();
            return image(left);
        }
        if (previous != null)
        {
            // The block atlas is still decoding: keep it from leaving the full image behind
            states.replace(id, BLOCK_ATLAS_CLAIM, DONE);
            return decoder.get();
        }

        I image = decoder.get();
        I copy = image != null && copySize > 0 ? blockCopy.apply(image, copySize) : null;
        if (copy == null || !states.replace(id, ICON_ATLAS_CLAIM, new Left<>(copy, false)))
        {
            if (copy != null)
                close.accept(copy);
            states.replace(id, ICON_ATLAS_CLAIM, DONE);
        }
        return image;
    }

    /** The block atlas copy no larger than {@code size}; when this atlas decodes first, it leaves the full icon for the icon atlas. */
    @Nullable
    I forBlockAtlas(ResourceLocation id, Supplier<I> decoder, int size)
    {
        Object previous = states.putIfAbsent(id, BLOCK_ATLAS_CLAIM);
        if (previous instanceof Left<?> left && !left.full() && states.remove(id, previous))
        {
            shared.incrementAndGet();
            return image(left);
        }
        if (previous != null)
        {
            // The icon atlas is still decoding: keep it from leaving a copy behind
            states.replace(id, ICON_ATLAS_CLAIM, DONE);
            return ownCopy(decoder.get(), size);
        }

        I image = decoder.get();
        I copy = image == null ? null : blockCopy.apply(image, size);
        if (copy == null)
        {
            // Small icons serve the block atlas as they are, so there is nothing to leave
            states.replace(id, BLOCK_ATLAS_CLAIM, DONE);
            return image;
        }
        if (!states.replace(id, BLOCK_ATLAS_CLAIM, new Left<>(image, true)))
            close.accept(image);
        return copy;
    }

    @Nullable
    private I ownCopy(@Nullable I image, int size)
    {
        if (image == null)
            return null;
        I copy = blockCopy.apply(image, size);
        if (copy == null)
            return image;
        close.accept(image);
        return copy;
    }

    /** Frees what neither atlas took; only called once both are done. */
    void closeLeftovers()
    {
        for (Object state : states.values())
        {
            if (state instanceof Left<?> left)
                close.accept(image(left));
        }
        states.clear();
    }

    // Only this instance stores images in the state map, so every left image has its type.
    @SuppressWarnings("unchecked")
    private I image(Left<?> left)
    {
        return (I) left.image();
    }
}
