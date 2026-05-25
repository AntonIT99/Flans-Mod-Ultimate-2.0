package com.flansmodultimate.util;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaModelCompiler
{
    private static final String COM_FOLDER = "com";
    private static final String JAVA_EXTENSION = ".java";
    private static final String MANIFEST_FILE = ".flansmod_compiled_java_models.json";
    private static final int SOURCE_TRANSFORM_VERSION = 1;
    private static final int MAX_DIAGNOSTICS_TO_LOG = 20;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SOURCE_HASH_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final List<Charset> SOURCE_CHARSETS = List.of(
        StandardCharsets.UTF_8,
        Charset.forName("GB18030"),
        StandardCharsets.ISO_8859_1
    );

    public static boolean isCompilerAvailable()
    {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    public static boolean hasOutdatedJavaModels(IContentProvider provider)
    {
        try
        {
            Path packRoot = provider.isArchive() ? provider.getExtractedPath() : provider.getPath();
            return hasOutdatedJavaModelsInDirectory(packRoot);
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not check Java model sources in content pack '{}': {}", provider.getName(), e.toString());
            return false;
        }
    }

    public static void compileJavaModels(IContentProvider provider)
    {
        Path packRoot = provider.isArchive() ? provider.getExtractedPath() : provider.getPath();
        CompilePlan plan;
        try
        {
            plan = createDirectoryCompilePlan(packRoot);
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not scan Java model sources in content pack '{}': {}", provider.getName(), e.toString());
            return;
        }

        if (!plan.needsCompile())
            return;

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
        {
            FlansMod.log.warn("Found Java model sources in content pack '{}', but no Java compiler is available. Run Minecraft with a JDK to compile pack model sources automatically.", provider.getName());
            return;
        }

        Path tempRoot = null;
        try
        {
            tempRoot = Files.createTempDirectory(packRoot.getParent(), ".flansmod-javac-");
            Path tempSourceRoot = tempRoot.resolve("src");
            Path tempClassRoot = tempRoot.resolve("classes");
            Files.createDirectories(tempSourceRoot);
            Files.createDirectories(tempClassRoot);

            List<Path> transformedSources = writeTransformedSources(packRoot, plan.sources(), tempSourceRoot);
            if (transformedSources.isEmpty())
                return;

            FlansMod.log.info("Compiling {} Java model source(s) in content pack '{}'", transformedSources.size(), provider.getName());
            if (!compileSources(compiler, packRoot, tempClassRoot, transformedSources))
                return;

            int classCount = copyTransformedClasses(tempClassRoot, packRoot);
            writeManifest(packRoot, plan.sourceHashes());
            FlansMod.log.info("Compiled {} Java model source(s) into {} class file(s) for content pack '{}'", transformedSources.size(), classCount, provider.getName());
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to compile Java model sources in content pack '{}'", provider.getName(), e);
        }
        finally
        {
            FileUtils.deleteRecursively(tempRoot);
        }
    }

    private static boolean hasOutdatedJavaModelsInDirectory(Path packRoot) throws IOException
    {
        return createDirectoryCompilePlan(packRoot).needsCompile();
    }

    private static CompilePlan createDirectoryCompilePlan(Path packRoot) throws IOException
    {
        List<Path> sources = listJavaSources(packRoot);
        if (sources.isEmpty())
            return new CompilePlan(sources, Collections.emptyMap(), false);

        Map<String, String> sourceHashes = getDirectorySourceHashes(packRoot, sources);
        boolean needsCompile = hasMissingDirectoryClass(packRoot, sourceHashes.keySet())
            || readDirectoryManifest(packRoot).map(manifest -> !manifest.matches(sourceHashes)).orElse(true);
        return new CompilePlan(sources, sourceHashes, needsCompile);
    }

    private static List<Path> listJavaSources(Path packRoot) throws IOException
    {
        Path javaRoot = packRoot.resolve(COM_FOLDER);
        if (!Files.isDirectory(javaRoot))
            return List.of();

        try (Stream<Path> walk = Files.walk(javaRoot))
        {
            return walk
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(JAVA_EXTENSION))
                .filter(source -> !hasPrimaryDirectoryClass(packRoot, source))
                .sorted()
                .toList();
        }
    }

    private static Map<String, String> getDirectorySourceHashes(Path packRoot, List<Path> sources) throws IOException
    {
        Map<String, String> hashes = new TreeMap<>();
        for (Path source : sources)
            hashes.put(toPackRelativePath(packRoot, source), sha256(source));
        return hashes;
    }

    private static boolean hasMissingDirectoryClass(Path packRoot, Set<String> sourcePaths)
    {
        return sourcePaths.stream().map(JavaModelCompiler::toPrimaryClassPath).anyMatch(classPath -> !Files.exists(packRoot.resolve(classPath)));
    }

    private static Optional<SourceManifest> readDirectoryManifest(Path packRoot)
    {
        Path manifestFile = packRoot.resolve(MANIFEST_FILE);
        if (!Files.isRegularFile(manifestFile))
            return Optional.empty();

        try
        {
            return readManifest(Files.readString(manifestFile, StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not read Java model compiler manifest '{}': {}", manifestFile.toAbsolutePath(), e.toString());
            return Optional.empty();
        }
    }

    private static Optional<SourceManifest> readManifest(String json)
    {
        try
        {
            JsonObject object = GSON.fromJson(json, JsonObject.class);
            if (object == null || !object.has("version") || !object.has("sources"))
                return Optional.empty();

            int version = object.get("version").getAsInt();
            Map<String, String> sources = GSON.fromJson(object.get("sources"), SOURCE_HASH_MAP_TYPE);
            return Optional.of(new SourceManifest(version, sources == null ? Collections.emptyMap() : sources));
        }
        catch (RuntimeException e)
        {
            return Optional.empty();
        }
    }

    private static void writeManifest(Path packRoot, Map<String, String> sourceHashes) throws IOException
    {
        JsonObject object = new JsonObject();
        object.addProperty("version", SOURCE_TRANSFORM_VERSION);
        object.add("sources", GSON.toJsonTree(sourceHashes, SOURCE_HASH_MAP_TYPE));
        Files.writeString(packRoot.resolve(MANIFEST_FILE), GSON.toJson(object), StandardCharsets.UTF_8);
    }

    private static List<Path> writeTransformedSources(Path packRoot, List<Path> sources, Path tempSourceRoot) throws IOException
    {
        List<Path> transformedSources = new ArrayList<>();
        for (Path source : sources)
        {
            String relativePath = toPackRelativePath(packRoot, source);
            Path transformedSource = tempSourceRoot.resolve(relativePath);
            Files.createDirectories(transformedSource.getParent());
            Files.writeString(transformedSource, transformSource(readSource(source)), StandardCharsets.UTF_8);
            transformedSources.add(transformedSource);
        }
        return transformedSources;
    }

    private static boolean compileSources(JavaCompiler compiler, Path packRoot, Path tempClassRoot, List<Path> sources) throws IOException
    {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8))
        {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(sources);
            List<String> options = List.of(
                "-encoding", StandardCharsets.UTF_8.name(),
                "-proc:none",
                "-source", "17",
                "-target", "17",
                "-classpath", buildClasspath(packRoot, tempClassRoot),
                "-d", tempClassRoot.toString()
            );

            Boolean ok = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();
            if (!Boolean.TRUE.equals(ok))
            {
                logDiagnostics(diagnostics);
                return false;
            }
            return true;
        }
    }

    private static int copyTransformedClasses(Path tempClassRoot, Path packRoot) throws IOException
    {
        if (!Files.isDirectory(tempClassRoot))
            return 0;

        List<Path> classFiles;
        try (Stream<Path> walk = Files.walk(tempClassRoot))
        {
            classFiles = walk
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.CLASS_EXTENSION))
                .sorted()
                .toList();
        }

        for (Path classFile : classFiles)
        {
            Path relativePath = tempClassRoot.relativize(classFile);
            Path outputFile = packRoot.resolve(relativePath);
            Files.createDirectories(outputFile.getParent());
            byte[] classData = Files.readAllBytes(classFile);
            Files.write(outputFile, ClassLoaderUtils.getModifiedClassData(classData, null));
        }

        return classFiles.size();
    }

    private static String buildClasspath(Path packRoot, Path tempClassRoot)
    {
        Set<String> entries = new LinkedHashSet<>();
        addClasspathEntry(entries, packRoot);
        addClasspathEntry(entries, tempClassRoot);

        addClasspathProperty(entries, "java.class.path");
        addClasspathProperty(entries, "legacyClassPath");
        addClasspathProperty(entries, "minecraft.class.path");
        addClasspathProperty(entries, "minecraft_classpath");

        addCodeSource(entries, FlansMod.class);
        addCodeSource(entries, IModelBase.class);
        addCodeSource(entries, ModelRendererTurbo.class);
        addCodeSource(entries, "net.minecraft.world.entity.Entity");
        addCodeSource(entries, "net.minecraft.client.Minecraft");
        addCodeSource(entries, "net.minecraftforge.fml.ModList");
        addCodeSource(entries, "com.mojang.blaze3d.vertex.PoseStack");
        addCodeSource(entries, "org.lwjgl.opengl.GL11");
        addCodeSource(entries, "org.joml.Quaternionf");

        return String.join(System.getProperty("path.separator"), entries);
    }

    private static void addClasspathProperty(Set<String> entries, String propertyName)
    {
        String classpath = System.getProperty(propertyName);
        if (StringUtils.isBlank(classpath))
            return;

        for (String entry : classpath.split(Pattern.quote(System.getProperty("path.separator"))))
            addClasspathEntry(entries, entry);
    }

    private static void addCodeSource(Set<String> entries, String className)
    {
        try
        {
            addCodeSource(entries, Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
        }
        catch (ClassNotFoundException | LinkageError ignored)
        {
            // Optional dependency for source compilation.
        }
    }

    private static void addCodeSource(Set<String> entries, Class<?> clazz)
    {
        try
        {
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource == null)
                return;

            URL location = codeSource.getLocation();
            if (location != null)
                addClasspathEntry(entries, Path.of(location.toURI()));
        }
        catch (SecurityException | IllegalArgumentException | URISyntaxException ignored)
        {
            // Best-effort classpath enrichment only.
        }
    }

    private static void addClasspathEntry(Set<String> entries, Path entry)
    {
        if (entry != null)
            addClasspathEntry(entries, entry.toAbsolutePath().normalize().toString());
    }

    private static void addClasspathEntry(Set<String> entries, String entry)
    {
        if (StringUtils.isNotBlank(entry))
            entries.add(entry);
    }

    private static void logDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics)
    {
        List<Diagnostic<? extends JavaFileObject>> diagnosticList = diagnostics.getDiagnostics();
        diagnosticList.stream()
            .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
            .limit(MAX_DIAGNOSTICS_TO_LOG)
            .forEach(diagnostic -> FlansMod.log.error("Java model compile error in {} at line {}: {}",
                diagnostic.getSource() == null ? "unknown source" : diagnostic.getSource().getName(),
                diagnostic.getLineNumber(),
                diagnostic.getMessage(Locale.ROOT)));

        long omitted = diagnosticList.stream().filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR).count() - MAX_DIAGNOSTICS_TO_LOG;
        if (omitted > 0)
            FlansMod.log.error("Omitted {} additional Java model compile error(s).", omitted);
    }

    private static String transformSource(String source)
    {
        String transformed = source;
        for (Map.Entry<String, String> mapping : ClassLoaderUtils.getSourceClassMappings()
            .entrySet()
            .stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, String> mapping) -> mapping.getKey().length()).reversed())
            .toList())
        {
            transformed = transformed.replace(mapping.getKey(), mapping.getValue());
        }

        transformed = removeLegacyClientOnlyAnnotations(transformed);
        transformed = transformGlStateManagerCalls(transformed);

        for (Map.Entry<String, String> mapping : ClassLoaderUtils.getMinecraftMethodMappings().entrySet())
            transformed = replaceIdentifier(transformed, mapping.getKey(), mapping.getValue());

        for (Map.Entry<String, String> mapping : ClassLoaderUtils.getMinecraftFieldMappings().entrySet())
            transformed = replaceIdentifier(transformed, mapping.getKey(), sanitizeSourceFieldName(mapping.getValue()));

        return transformed;
    }

    private static String removeLegacyClientOnlyAnnotations(String source)
    {
        String transformed = source.replaceAll("(?m)^\\s*import\\s+(?:cpw\\.mods\\.fml|net\\.minecraftforge\\.fml)\\.relauncher\\.(?:Side|SideOnly)\\s*;\\s*\\R?", "");
        return transformed.replaceAll("(?m)^\\s*@SideOnly\\s*\\([^)]*\\)\\s*\\R?", "");
    }

    private static String transformGlStateManagerCalls(String source)
    {
        String transformed = source
            .replace("import net.minecraft.client.renderer.GlStateManager;", "import org.lwjgl.opengl.GL11;")
            .replace("net.minecraft.client.renderer.GlStateManager", "org.lwjgl.opengl.GL11");

        transformed = transformed.replaceAll("\\bGlStateManager\\s*\\.\\s*translate\\s*\\(", "GL11.glTranslatef(");
        transformed = transformed.replaceAll("\\bGlStateManager\\s*\\.\\s*scale\\s*\\(", "GL11.glScalef(");
        transformed = transformed.replaceAll("\\bGlStateManager\\s*\\.\\s*rotate\\s*\\(", "GL11.glRotatef(");
        return transformed;
    }

    private static String replaceIdentifier(String source, String from, String to)
    {
        return source.replaceAll("\\b" + Pattern.quote(from) + "\\b", Matcher.quoteReplacement(to));
    }

    private static String sanitizeSourceFieldName(String mappedFieldName)
    {
        int comma = mappedFieldName.indexOf(',');
        return comma >= 0 ? mappedFieldName.substring(0, comma) : mappedFieldName;
    }

    private static String readSource(Path file) throws IOException
    {
        CharacterCodingException firstDecodeFailure = null;
        for (Charset charset : SOURCE_CHARSETS)
        {
            try
            {
                return Files.readString(file, charset);
            }
            catch (CharacterCodingException e)
            {
                if (firstDecodeFailure == null)
                    firstDecodeFailure = e;
                else
                    firstDecodeFailure.addSuppressed(e);
            }
        }

        throw firstDecodeFailure != null ? firstDecodeFailure : new IOException("No charset configured for " + file);
    }

    private static String sha256(Path file) throws IOException
    {
        return sha256(Files.readAllBytes(file));
    }

    private static String sha256(byte[] data)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String toPackRelativePath(Path packRoot, Path path)
    {
        return packRoot.relativize(path).toString().replace('\\', '/');
    }

    private static String toPrimaryClassPath(String sourcePath)
    {
        return sourcePath.substring(0, sourcePath.length() - JAVA_EXTENSION.length()) + FileUtils.CLASS_EXTENSION;
    }

    private static boolean hasPrimaryDirectoryClass(Path packRoot, Path source)
    {
        return Files.exists(packRoot.resolve(toPrimaryClassPath(toPackRelativePath(packRoot, source))));
    }

    private record CompilePlan(List<Path> sources, Map<String, String> sourceHashes, boolean needsCompile) {}

    private record SourceManifest(int version, Map<String, String> sourceHashes)
    {
        boolean matches(Map<String, String> currentSourceHashes)
        {
            return version == SOURCE_TRANSFORM_VERSION && sourceHashes.equals(currentSourceHashes);
        }
    }
}
