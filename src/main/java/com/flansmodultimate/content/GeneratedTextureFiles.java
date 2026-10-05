package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.ResourceUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Copies legacy PNGs and only removes destinations explicitly created and still owned by us. */
final class GeneratedTextureFiles
{
    private static final int VERSION = 2;
    private static final String MANIFEST = ".flansmod_generated_textures.json";
    private static final Gson GSON = new Gson();
    private record Copy(String source, ContentFileCache.Stamp input, ContentFileCache.Stamp output, boolean owned, String digest) {}
    private record Manifest(int version, Map<String, Copy> files) {}

    private GeneratedTextureFiles() {}

    static void copy(Path source, Path destination)
    {
        copy(source, destination, Map.of(), false);
    }

    static void copy(Path source, Path destination, Map<String, DynamicReference> aliases, boolean armor)
    {
        try
        {
            Manifest previous = read(destination);
            Map<String, Copy> generated = new TreeMap<>();
            Set<String> planned = new HashSet<>();
            for (Path input : sources(source))
            {
                String desired = targetName(input, aliases, armor);
                String target = desired;
                for (int suffix = 1; ; suffix++)
                {
                    Path output = destination.resolve(target);
                    Copy old = previous.files().get(target);
                    boolean owned = stillOwned(output, old);
                    if (!planned.contains(target) && (owned || !Files.exists(output)
                        || !FileUtils.isDifferentFileContent(input, output, true)))
                    {
                        boolean created = !Files.exists(output);
                        Files.createDirectories(output.getParent());
                        if (created || (owned && FileUtils.isDifferentFileContent(input, output, true)))
                            FileUtils.copyAsPng(input, output);
                        generated.put(target, new Copy(input.getFileName().toString(), ContentFileCache.stamp(input),
                            ContentFileCache.stamp(output), created || owned, created || owned ? ContentFileCache.digest(output) : null));
                        planned.add(target);
                        break;
                    }
                    target = FilenameUtils.getBaseName(desired) + "-" + suffix + FileUtils.PNG_EXTENSION;
                }
            }
            for (Map.Entry<String, Copy> old : previous.files().entrySet())
                if (!generated.containsKey(old.getKey()) && stillOwned(destination.resolve(old.getKey()), old.getValue()))
                    Files.deleteIfExists(destination.resolve(old.getKey()));
            if (generated.isEmpty())
                Files.deleteIfExists(destination.resolve(MANIFEST));
            else
                ContentFileCache.writeJson(destination.resolve(MANIFEST), GSON.toJsonTree(new Manifest(VERSION, generated)));
        }
        catch (IOException | RuntimeException e)
        {
            FlansMod.log.error("Could not generate textures from '{}' to '{}': {}", source, destination, e.toString());
        }
    }

    static boolean needsUpdate(Path source, Path destination, Map<String, DynamicReference> aliases, boolean armor)
    {
        try
        {
            Manifest previous = read(destination);
            List<Path> inputs = sources(source);
            if (previous.version() != VERSION)
                return !inputs.isEmpty() || !previous.files().isEmpty();
            Map<String, Copy> bySource = new LinkedHashMap<>();
            previous.files().values().forEach(copy -> bySource.put(copy.source(), copy));
            if (bySource.size() != inputs.size())
                return true;
            for (Path input : inputs)
            {
                Copy copy = bySource.get(input.getFileName().toString());
                if (copy == null || !ContentFileCache.stamp(input).equals(copy.input()))
                    return true;
            }
            for (Map.Entry<String, Copy> entry : previous.files().entrySet())
            {
                Path output = destination.resolve(entry.getKey());
                if (!Files.isRegularFile(output) || !ContentFileCache.stamp(output).equals(entry.getValue().output()))
                    return true;
                String desired = targetName(source.resolve(entry.getValue().source()), aliases, armor);
                String actual = entry.getKey();
                if (!actual.equals(desired) && !actual.startsWith(FilenameUtils.getBaseName(desired) + "-"))
                    return true;
            }
            return false;
        }
        catch (IOException | RuntimeException e)
        {
            return true;
        }
    }

    private static boolean stillOwned(Path output, Copy copy) throws IOException
    {
        return copy != null && copy.owned() && Files.isRegularFile(output)
            && copy.digest() != null && ContentFileCache.digest(output).equals(copy.digest());
    }

    private static List<Path> sources(Path directory) throws IOException
    {
        if (!Files.isDirectory(directory))
            return List.of();
        try (Stream<Path> files = Files.list(directory))
        {
            return files.filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                .filter(file -> !FileUtils.isMacOsMetadataPath(file.getFileName().toString())).sorted().toList();
        }
    }

    private static String targetName(Path file, Map<String, DynamicReference> aliases, boolean armor)
    {
        String base = FilenameUtils.getBaseName(file.getFileName().toString());
        String name = armor ? LegacyTextureAliases.getArmorTextureBaseName(base) : base;
        String layer = armor ? base.substring(name.length()) : "";
        String sanitized = ResourceUtils.sanitize(name);
        DynamicReference alias = aliases.get(sanitized);
        return (alias == null ? sanitized : alias.get()) + layer + FileUtils.PNG_EXTENSION;
    }

    private static Manifest read(Path directory)
    {
        Path file = directory.resolve(MANIFEST);
        if (!Files.isRegularFile(file))
            return new Manifest(VERSION, Map.of());
        try
        {
            JsonElement json = JsonParser.parseString(Files.readString(file));
            // Old lists did not reliably distinguish authored files from generated files. Preserve them.
            if (json.isJsonArray())
                return new Manifest(1, Map.of());
            Manifest manifest = GSON.fromJson(json, Manifest.class);
            if (manifest != null && manifest.version() == VERSION && manifest.files() != null)
            {
                for (String name : manifest.files().keySet())
                    if (Path.of(name).isAbsolute() || !Path.of(name).getFileName().toString().equals(name)
                        || name.equals(".") || name.equals(".."))
                        return new Manifest(1, Map.of());
                return manifest;
            }
        }
        catch (IOException | RuntimeException e)
        {
            FlansMod.log.warn("Ignoring invalid generated texture manifest '{}': {}", file, e.toString());
        }
        return new Manifest(1, Map.of());
    }
}
