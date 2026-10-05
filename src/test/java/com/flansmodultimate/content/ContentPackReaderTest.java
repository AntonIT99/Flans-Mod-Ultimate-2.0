package com.flansmodultimate.content;

import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ContentPackReaderTest
{
    @TempDir Path root;

    private record Read(String name, EnumType type, List<String> lines) {}

    @Test
    void parallelReadingProducesWhatOneThreadReads() throws Exception
    {
        ContentPack folder = new ContentPack("folder", root.resolve("folder"));
        for (int i = 0; i < 60; i++)
            write(folder.getPath().resolve("guns/sub" + (i % 3) + "/gun" + i + ".txt"), "ShortName gun" + i + "\nName Gun " + i);
        write(folder.getPath().resolve("parts/engine.txt"), "﻿ShortName engine");
        write(folder.getPath().resolve("parts/readme.md"), "not a definition");
        write(folder.getPath().resolve("unknown/ignored.txt"), "ShortName ignored");
        write(folder.getPath().resolve(ContentPackPaths.ID_ALIAS_FILE), "{}");
        write(folder.getPath().resolve(ContentPackPaths.GUI_TEXTURES_ALIAS_FILE), "{}");
        Files.write(folder.getPath().resolve("guns/latin1.txt"), "Name Sturmgeschütz".getBytes(StandardCharsets.ISO_8859_1));

        ContentPackReader.Result sequential = ContentPackReader.read(folder, ContentLoadingWorkers.sequential());
        ContentPackReader.Result parallel;
        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(4))
        {
            parallel = ContentPackReader.read(folder, workers);
        }

        assertEquals(62, sequential.typeFiles().size());
        assertEquals(contents(sequential), contents(parallel));
        assertEquals(Set.of(ContentPackPaths.ID_ALIAS_FILE, ContentPackPaths.GUI_TEXTURES_ALIAS_FILE), parallel.aliasFiles());
        Read engine = contents(parallel).stream().filter(read -> read.name().equals("engine.txt")).findFirst().orElseThrow();
        assertEquals(List.of("ShortName engine"), engine.lines());
        assertEquals(EnumType.PART, engine.type());
        assertTrue(contents(parallel).contains(new Read("latin1.txt", EnumType.GUN, List.of("Name Sturmgeschütz"))));
    }

    @Test
    void packsAreHandedOverInOrderWhileLaterOnesAreReadAhead() throws Exception
    {
        List<IContentProvider> packs = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            Path archive = root.resolve("pack" + i + ".zip");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive)))
            {
                for (int file = 0; file <= i; file++)
                {
                    zip.putNextEntry(new ZipEntry("bullets/bullet" + file + ".txt"));
                    zip.write(("ShortName pack" + i + "_" + file).getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            packs.add(new ContentPack(archive.getFileName().toString(), archive));
        }

        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(3))
        {
            ContentPackReader reader = new ContentPackReader(packs, workers);
            for (int i = 0; i < packs.size(); i++)
            {
                List<TypeFile> files = reader.take(packs.get(i)).typeFiles();
                assertEquals(i + 1, files.size());
                for (TypeFile file : files)
                {
                    assertSame(packs.get(i), file.getContentPack());
                    assertTrue(file.getLines().get(0).startsWith("ShortName pack" + i + "_"));
                }
            }
        }
    }

    @Test
    void anUnreadablePackYieldsNoDefinitions()
    {
        ContentPack broken = new ContentPack("broken.zip", root.resolve("missing.zip"));
        assertEquals(List.of(), ContentPackReader.read(broken, ContentLoadingWorkers.sequential()).typeFiles());
    }

    private static List<Read> contents(ContentPackReader.Result result)
    {
        return result.typeFiles().stream().map(file -> new Read(file.getName(), file.getType(), file.getLines())).toList();
    }

    private static void write(Path file, String text) throws Exception
    {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
