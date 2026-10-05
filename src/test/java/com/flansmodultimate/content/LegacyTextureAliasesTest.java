package com.flansmodultimate.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyTextureAliasesTest
{
    @TempDir Path root;

    @BeforeEach void configure() { ContentFileCache.configure(root.resolve("cache")); }
    @AfterEach void reset() { ContentFileCache.configure(null); }

    @Test
    void checksBothArmorLayersAndKeepsTheFirstOwnerWhenSharing() throws Exception
    {
        ContentPack a = pack("a"), b = pack("b"), c = pack("c");
        for (ContentPack pack : List.of(a, b, c))
        {
            png(pack, "uniform_1", 0xffff0000);
            png(pack, "uniform_2", pack == c ? 0xff0000ff : 0xffff0000);
        }
        LegacyTextureAliases aliases = new LegacyTextureAliases();
        aliases.prepare(List.of(a, b, c));
        for (ContentPack pack : List.of(a, b, c))
        {
            aliases.initialize(pack);
            aliases.findDuplicates(pack);
        }
        assertEquals("uniform", aliases.getArmorTextureReferences().get(b).get("uniform").get());
        assertEquals("uniform_2", aliases.getArmorTextureReferences().get(c).get("uniform").get());
    }

    @Test
    void preparingOnWorkersAssignsTheSameAliasesAsOneThread() throws Exception
    {
        List<ContentPack> packs = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++)
        {
            ContentPack pack = pack("p" + i);
            png(pack, "uniform_1", i % 2 == 0 ? 0xffff0000 : 0xff0000ff);
            png(pack, "uniform_2", 0xffff0000);
            png(pack, "own" + i + "_1", 0xff00ff00);
            packs.add(pack);
        }
        List<String> expected = aliases(packs, ContentLoadingWorkers.sequential());
        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(4))
        {
            assertEquals(expected, aliases(packs, workers));
        }
        assertEquals(List.of("uniform", "uniform_2", "uniform"), expected.subList(0, 3));
    }

    private static List<String> aliases(List<ContentPack> packs, ContentLoadingWorkers workers)
    {
        LegacyTextureAliases aliases = new LegacyTextureAliases();
        aliases.prepare(List.copyOf(packs), workers);
        List<String> result = new java.util.ArrayList<>();
        for (ContentPack pack : packs)
        {
            aliases.initialize(pack);
            aliases.findDuplicates(pack);
            result.add(aliases.getArmorTextureReferences().get(pack).get("uniform").get());
        }
        return result;
    }

    @Test
    void sharesEqualPixelsWithDifferentPngEncodings() throws Exception
    {
        ContentPack a = pack("a"), b = pack("b");
        png(a, "uniform_1", 0xffff0000);
        Path file = b.getAssetsPath().resolve("armor/uniform_1.png");
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        image.setRGB(0, 0, 0xffff0000);
        ImageIO.write(image, "png", file.toFile());
        LegacyTextureAliases aliases = new LegacyTextureAliases();
        aliases.prepare(List.of(a, b)); aliases.initialize(a); aliases.initialize(b);
        aliases.findDuplicates(a); aliases.findDuplicates(b);
        assertEquals("uniform", aliases.getArmorTextureReferences().get(b).get("uniform").get());
    }

    @Test
    void decodesANewCollisionAfterAnEarlierUniqueNameScan() throws Exception
    {
        ContentPack a = pack("a"), b = pack("b");
        png(a, "uniform_1", 0xffff0000);
        LegacyTextureAliases initial = new LegacyTextureAliases();
        initial.prepare(List.of(a)); initial.initialize(a); initial.findDuplicates(a);
        png(b, "uniform_1", 0xff0000ff);
        LegacyTextureAliases combined = new LegacyTextureAliases();
        combined.prepare(List.of(a, b)); combined.initialize(a); combined.initialize(b);
        combined.findDuplicates(a); combined.findDuplicates(b);
        assertEquals("uniform_2", combined.getArmorTextureReferences().get(b).get("uniform").get());
    }

    @Test
    void reservesFutureAuthoredNamesAndReturnsToOriginalAfterPackRemoval() throws Exception
    {
        ContentPack a = pack("a"), b = pack("b");
        png(a, "uniform_1", 0xffff0000);
        png(b, "uniform_1", 0xff0000ff);
        png(b, "uniform_2_1", 0xff00ff00);
        LegacyTextureAliases first = new LegacyTextureAliases();
        first.prepare(List.of(a, b));
        first.initialize(a); first.initialize(b);
        first.findDuplicates(a); first.findDuplicates(b);
        assertEquals("uniform_3", first.getArmorTextureReferences().get(b).get("uniform").get());
        LegacyTextureAliases remaining = new LegacyTextureAliases();
        remaining.prepare(List.of(b)); remaining.initialize(b); remaining.findDuplicates(b);
        assertEquals("uniform", remaining.getArmorTextureReferences().get(b).get("uniform").get());
    }

    private ContentPack pack(String name) { return new ContentPack(name, root.resolve(name)); }
    private static void png(ContentPack pack, String name, int color) throws Exception
    {
        Path file = pack.getAssetsPath().resolve("armor/" + name + ".png");
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, color);
        ImageIO.write(image, "png", file.toFile());
    }
}
