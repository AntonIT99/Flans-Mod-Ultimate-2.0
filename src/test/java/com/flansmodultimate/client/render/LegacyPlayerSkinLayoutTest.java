package com.flansmodultimate.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Rebuilding an old 2:1 SkinOverride sheet into the layout the modern player model samples. */
class LegacyPlayerSkinLayoutTest
{
    @Test
    void legacySheetBecomesASquareOneKeepingItsOriginalHalf()
    {
        try (NativeImage legacy = legacySkin(1); NativeImage modern = PlayerSkinOverrides.toModernLayout(legacy, 1))
        {
            assertEquals(64, modern.getWidth());
            assertEquals(64, modern.getHeight());
            // The head, the body and the right limbs keep the exact place the old format gave them.
            assertEquals(legacy.getPixelRGBA(8, 8), modern.getPixelRGBA(8, 8));
            assertEquals(legacy.getPixelRGBA(4, 20), modern.getPixelRGBA(4, 20));
            assertEquals(legacy.getPixelRGBA(44, 20), modern.getPixelRGBA(44, 20));
        }
    }

    @Test
    void armsAndLegsAreMirroredOntoTheirMissingSide()
    {
        try (NativeImage legacy = legacySkin(1); NativeImage modern = PlayerSkinOverrides.toModernLayout(legacy, 1))
        {
            // Right leg: top and bottom faces, then front, right, back and left.
            assertMirrored(modern, 1, 4, 16, 20, 48, 4, 4);
            assertMirrored(modern, 1, 8, 16, 24, 48, 4, 4);
            assertMirrored(modern, 1, 0, 20, 24, 52, 4, 12);
            assertMirrored(modern, 1, 4, 20, 20, 52, 4, 12);
            assertMirrored(modern, 1, 8, 20, 16, 52, 4, 12);
            assertMirrored(modern, 1, 12, 20, 28, 52, 4, 12);
            // Right arm, likewise.
            assertMirrored(modern, 1, 44, 16, 36, 48, 4, 4);
            assertMirrored(modern, 1, 48, 16, 40, 48, 4, 4);
            assertMirrored(modern, 1, 40, 20, 40, 52, 4, 12);
            assertMirrored(modern, 1, 44, 20, 36, 52, 4, 12);
            assertMirrored(modern, 1, 48, 20, 32, 52, 4, 12);
            assertMirrored(modern, 1, 52, 20, 44, 52, 4, 12);
        }
    }

    @Test
    void onlyTheHeadKeepsASecondLayer()
    {
        try (NativeImage legacy = legacySkin(1); NativeImage modern = PlayerSkinOverrides.toModernLayout(legacy, 1))
        {
            // The hat the legacy sheet does carry survives.
            assertEquals(legacy.getPixelRGBA(40, 8), modern.getPixelRGBA(40, 8));
            // The trouser, jacket and sleeve layers the old format never had stay empty.
            assertEmpty(modern, 1, 0, 32, 64, 48);
            assertEmpty(modern, 1, 0, 48, 16, 64);
            assertEmpty(modern, 1, 48, 48, 64, 64);
        }
    }

    @Test
    void baseLayersAreForcedOpaqueAndAFullyOpaqueHatIsDropped()
    {
        try (NativeImage legacy = legacySkin(1))
        {
            // A half transparent body is what an editor leaves behind when a pack only paints a part of it.
            legacy.setPixelRGBA(4, 20, 0x40FF0000);
            // An old skin with no hat had that half of the sheet painted over instead of left empty.
            legacy.fillRect(32, 0, 32, 32, 0xFF00FF00);

            try (NativeImage modern = PlayerSkinOverrides.toModernLayout(legacy, 1))
            {
                assertEquals(0xFFFF0000, modern.getPixelRGBA(4, 20), "body base layer must be opaque");
                assertEquals(0xFFFF0000, modern.getPixelRGBA(23, 52), "mirrored limb must be opaque");
                assertTransparent(modern, 1, 32, 0, 64, 16);
            }
        }
    }

    @Test
    void hdSheetsKeepingTheRatioAreConvertedAtTheirOwnScale()
    {
        try (NativeImage legacy = legacySkin(4); NativeImage modern = PlayerSkinOverrides.toModernLayout(legacy, 4))
        {
            assertEquals(256, modern.getWidth());
            assertEquals(256, modern.getHeight());
            assertMirrored(modern, 4, 4, 20, 20, 52, 4, 12);
            assertMirrored(modern, 4, 44, 20, 36, 52, 4, 12);
            assertEmpty(modern, 4, 0, 32, 64, 48);
        }
    }

    /**
     * A 2:1 sheet whose every pixel differs, so a copy landing at the wrong place or facing the
     * wrong way cannot pass unnoticed. The hat is left translucent to keep it out of the way of
     * the check that drops a fully opaque one.
     */
    private static NativeImage legacySkin(int scale)
    {
        NativeImage legacy = new NativeImage(64 * scale, 32 * scale, true);
        for (int x = 0; x < legacy.getWidth(); x++)
            for (int y = 0; y < legacy.getHeight(); y++)
                legacy.setPixelRGBA(x, y, 0xFF000000 | (x << 8) | y);
        legacy.fillRect(32 * scale, 0, 32 * scale, 16 * scale, 0x7F123456);
        return legacy;
    }

    /** Asserts that a region was copied to another one flipped horizontally, as the old limbs were. */
    private static void assertMirrored(NativeImage image, int scale,
                                       int fromX, int fromY, int toX, int toY, int width, int height)
    {
        for (int x = 0; x < width * scale; x++)
            for (int y = 0; y < height * scale; y++)
                assertEquals(image.getPixelRGBA(fromX * scale + x, fromY * scale + y),
                    image.getPixelRGBA((toX + width) * scale - 1 - x, toY * scale + y),
                    "mirrored pixel " + x + ',' + y + " of the region at " + fromX + ',' + fromY);
    }

    private static void assertEmpty(NativeImage image, int scale, int x0, int y0, int x1, int y1)
    {
        forEachPixel(scale, x0, y0, x1, y1, (x, y) ->
            assertEquals(0, image.getPixelRGBA(x, y), "pixel " + x + ',' + y + " must be empty"));
    }

    private static void assertTransparent(NativeImage image, int scale, int x0, int y0, int x1, int y1)
    {
        forEachPixel(scale, x0, y0, x1, y1, (x, y) ->
            assertEquals(0, image.getPixelRGBA(x, y) >>> 24, "pixel " + x + ',' + y + " must be transparent"));
    }

    private static void forEachPixel(int scale, int x0, int y0, int x1, int y1, PixelCheck check)
    {
        for (int x = x0 * scale; x < x1 * scale; x++)
            for (int y = y0 * scale; y < y1 * scale; y++)
                check.accept(x, y);
    }

    @FunctionalInterface
    private interface PixelCheck
    {
        void accept(int x, int y);
    }
}
