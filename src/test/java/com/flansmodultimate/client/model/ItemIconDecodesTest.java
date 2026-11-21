package com.flansmodultimate.client.model;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Each image must end up owned by exactly one atlas or freed once, whichever atlas reaches an icon first. */
class ItemIconDecodesTest
{
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("flansmod", "item/icon");
    private static final int COPY_SIZE = 16;

    private final AtomicInteger decodes = new AtomicInteger();
    private final ItemIconDecodes<Image> handoff = new ItemIconDecodes<>(null, (image, size) -> image.size > size ? new Image(size) : null, Image::close);

    @Test
    void iconAtlasFirstLeavesTheBlockAtlasItsCopy()
    {
        Image full = handoff.forIconAtlas(ICON, () -> decode(512), COPY_SIZE);
        Image copy = handoff.forBlockAtlas(ICON, () -> decode(512), COPY_SIZE);
        handoff.closeLeftovers();

        assertEquals(1, decodes.get());
        assertEquals(1, handoff.sharedDecodes());
        assertEquals(512, full.size);
        assertEquals(COPY_SIZE, copy.size);
        assertFalse(full.closed);
        assertFalse(copy.closed);
    }

    @Test
    void blockAtlasFirstLeavesTheIconAtlasTheFullImage()
    {
        Image copy = handoff.forBlockAtlas(ICON, () -> decode(512), COPY_SIZE);
        Image full = handoff.forIconAtlas(ICON, () -> decode(512), COPY_SIZE);
        handoff.closeLeftovers();

        assertEquals(1, decodes.get());
        assertEquals(1, handoff.sharedDecodes());
        assertEquals(COPY_SIZE, copy.size);
        assertEquals(512, full.size);
        assertFalse(full.closed);
    }

    @Test
    void smallIconsAreNeverSharedBecauseBothAtlasesKeepTheirImage()
    {
        Image block = handoff.forBlockAtlas(ICON, () -> decode(COPY_SIZE), COPY_SIZE);
        Image icon = handoff.forIconAtlas(ICON, () -> decode(COPY_SIZE), COPY_SIZE);

        assertEquals(2, decodes.get());
        assertNotSame(block, icon);
    }

    @Test
    void imagesNeitherAtlasTookAreFreedOnce()
    {
        AtomicReference<Image> decoded = new AtomicReference<>();
        handoff.forBlockAtlas(ICON, () ->
        {
            Image image = decode(512);
            decoded.set(image);
            return image;
        }, COPY_SIZE);

        handoff.closeLeftovers();
        handoff.closeLeftovers();

        assertTrue(decoded.get().closed, "the full image the icon atlas never took is freed");
    }

    @Test
    void anAtlasArrivingDuringTheOthersDecodeDecodesItselfAndNothingLeaks()
    {
        AtomicReference<Image> blockImage = new AtomicReference<>();
        Image full = handoff.forIconAtlas(ICON, () ->
        {
            blockImage.set(handoff.forBlockAtlas(ICON, () -> decode(512), COPY_SIZE));
            return decode(512);
        }, COPY_SIZE);
        handoff.closeLeftovers();

        assertEquals(2, decodes.get());
        assertEquals(0, handoff.sharedDecodes());
        assertEquals(COPY_SIZE, blockImage.get().size);
        assertFalse(blockImage.get().closed);
        assertFalse(full.closed);
    }

    private Image decode(int size)
    {
        decodes.incrementAndGet();
        return new Image(size);
    }

    private static final class Image
    {
        private final int size;
        private boolean closed;

        Image(int size)
        {
            this.size = size;
        }

        void close()
        {
            assertFalse(closed, "image closed twice");
            closed = true;
        }
    }
}
