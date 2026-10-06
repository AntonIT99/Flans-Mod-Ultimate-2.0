package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.ResourceUtils;
import com.flansmodultimate.util.SoundJsonProcessor;
import com.flansmodultimate.util.SoundLengthIndex;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.flansmodultimate.content.ContentPackPaths.FOLDER_SOUND;
import static com.flansmodultimate.content.ContentPackPaths.FOLDER_SOUNDS;

/** Normalizes modern sounds, copies legacy sounds and builds measured sound-length metadata. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ContentPackSounds
{
    static void generate(IContentProvider provider)
    {
        Path assetsDir = provider.getAssetsPath();
        Path soundDir = assetsDir.resolve(FOLDER_SOUND);
        Path soundsDir = assetsDir.resolve(FOLDER_SOUNDS);
        copyLegacySoundFolder(soundDir, soundsDir);
        normalizeSoundsFolder(soundsDir);

        Path soundsJsonFile = provider.getAssetsPath().resolve("sounds.json");
        if (Files.isRegularFile(soundsJsonFile))
            SoundJsonProcessor.process(soundsJsonFile, FlansMod.FLANSMOD_ID, soundsDir);

        SoundLengthIndex.generate(soundsDir, assetsDir.resolve(SoundLengthIndex.FILE_NAME));
    }

    private static void copyLegacySoundFolder(Path soundDir, Path soundsDir)
    {
        if (!Files.isDirectory(soundDir))
            return;
        if (!FileUtils.tryCreateDirectories(soundsDir))
            return;

        List<Path> sourceFiles;
        try (Stream<Path> stream = Files.walk(soundDir))
        {
            sourceFiles = stream.filter(Files::isRegularFile).filter(FileUtils::isOgg)
                .sorted(Comparator.comparing(path -> soundDir.relativize(path).toString().toLowerCase(Locale.ROOT))).toList();
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not scan legacy sound folder {}", soundDir, e);
            return;
        }

        Set<String> existingSounds = collectExistingSoundNames(soundsDir);
        for (Path source : sourceFiles)
            copyLegacySound(source, soundDir, soundsDir, existingSounds);
    }

    private static Set<String> collectExistingSoundNames(Path soundsDir)
    {
        if (!Files.isDirectory(soundsDir))
            return new HashSet<>();

        try (Stream<Path> stream = Files.walk(soundsDir))
        {
            return stream.filter(Files::isRegularFile).filter(FileUtils::isOgg).map(path -> soundNameKey(normalizeSoundRelativePath(soundsDir.relativize(path))))
                .collect(Collectors.toCollection(HashSet::new));
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not scan sounds folder {}", soundsDir, e);
            return new HashSet<>();
        }
    }

    private static void copyLegacySound(Path source, Path soundDir, Path soundsDir, Set<String> existingSounds)
    {
        Path relativePath = normalizeSoundRelativePath(soundDir.relativize(source));
        String soundNameKey = soundNameKey(relativePath);
        if (existingSounds.contains(soundNameKey))
            return;

        Path target = soundsDir.resolve(relativePath).normalize();
        if (!target.startsWith(soundsDir))
            return;

        try
        {
            Files.createDirectories(target.getParent());
            if (Files.notExists(target))
            {
                Files.copy(source, target);
                existingSounds.add(soundNameKey);
            }
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not copy legacy sound {} to {}", source, target, e);
        }
    }

    private static void normalizeSoundsFolder(Path soundsDir)
    {
        if (!Files.isDirectory(soundsDir))
            return;

        List<Path> soundFiles;
        try (Stream<Path> stream = Files.walk(soundsDir))
        {
            soundFiles = stream.filter(Files::isRegularFile).sorted(Comparator.comparing((Path path) -> soundsDir.relativize(path).getNameCount()).reversed()).toList();
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not scan sounds folder {}", soundsDir, e);
            return;
        }

        for (Path soundFile : soundFiles)
            normalizeSoundFile(soundsDir, soundFile);
        deleteEmptySubdirectories(soundsDir);
        normalizeSoundDirectories(soundsDir);
        deleteEmptySubdirectories(soundsDir);
    }

    private static void normalizeSoundFile(Path soundsDir, Path source)
    {
        Path relativePath = soundsDir.relativize(source);
        Path normalizedRelativePath = normalizeSoundRelativePath(relativePath);
        if (relativePath.toString().equals(normalizedRelativePath.toString()))
            return;

        Path target = soundsDir.resolve(normalizedRelativePath).normalize();
        if (!target.startsWith(soundsDir))
            return;

        try
        {
            Files.createDirectories(target.getParent());
            Path finalTarget = resolveSoundNormalizeTarget(source, target);
            if (finalTarget == null)
            {
                Files.delete(source);
                return;
            }
            FileUtils.moveWithCaseOnlyHopIfNeeded(source, finalTarget);
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not normalize sound file {}", source, e);
        }
    }

    @Nullable
    private static Path resolveSoundNormalizeTarget(Path source, Path target) throws IOException
    {
        if (Files.exists(target))
        {
            if (Files.isSameFile(source, target))
                return target;
            if (!FileUtils.isDifferentFileContent(source, target, false))
                return null;
            return FileUtils.ensureUnique(target);
        }
        return target;
    }

    private static Path normalizeSoundRelativePath(Path relativePath)
    {
        Path normalizedPath = null;
        int count = relativePath.getNameCount();
        for (int i = 0; i < count; i++)
        {
            String segment = relativePath.getName(i).toString();
            String normalizedSegment = (i == count - 1) ? normalizeSoundFileName(segment) : ResourceUtils.sanitize(segment);
            normalizedPath = normalizedPath == null ? Path.of(normalizedSegment) : normalizedPath.resolve(normalizedSegment);
        }
        return normalizedPath == null ? Path.of(StringUtils.EMPTY) : normalizedPath;
    }

    private static String normalizeSoundFileName(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && fileName.substring(dot).equalsIgnoreCase(FileUtils.OGG_EXTENSION))
            return ResourceUtils.sanitize(fileName.substring(0, dot)) + FileUtils.OGG_EXTENSION;
        return ResourceUtils.sanitize(fileName);
    }

    private static String soundNameKey(Path relativePath)
    {
        return relativePath.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static void normalizeSoundDirectories(Path root)
    {
        List<Path> soundDirectories;
        try (Stream<Path> stream = Files.walk(root))
        {
            soundDirectories = stream.filter(Files::isDirectory).filter(path -> !path.equals(root))
                .sorted(Comparator.comparing((Path path) -> root.relativize(path).getNameCount()).reversed()).toList();
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not scan sound directories under {}", root, e);
            return;
        }

        for (Path directory : soundDirectories)
            normalizeSoundDirectory(directory);
    }

    private static void normalizeSoundDirectory(Path source)
    {
        if (!Files.isDirectory(source) || source.getParent() == null)
            return;

        String normalizedName = ResourceUtils.sanitize(source.getFileName().toString());
        if (StringUtils.isBlank(normalizedName))
            return;

        Path target = source.resolveSibling(normalizedName).normalize();
        if (source.getFileName().toString().equals(normalizedName))
            return;

        try
        {
            if (Files.exists(target))
            {
                if (Files.isSameFile(source, target))
                    FileUtils.moveWithCaseOnlyHopIfNeeded(source, target);
                else
                    FileUtils.deleteDirectoryIfEmpty(source);
            }
            else
            {
                FileUtils.moveWithCaseOnlyHopIfNeeded(source, target);
            }
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not normalize sound directory {}", source, e);
        }
    }

    private static void deleteEmptySubdirectories(Path root)
    {
        try (Stream<Path> stream = Files.walk(root))
        {
            stream.filter(Files::isDirectory).filter(path -> !path.equals(root)).sorted(Comparator.reverseOrder()).forEach(FileUtils::deleteDirectoryIfEmpty);
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not clean empty sound directories under {}", root, e);
        }
    }
}
