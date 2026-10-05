package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ContentLoadingConfig;
import com.flansmodultimate.util.FlansLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/** A read-only, pack-local view of modern assets. Legacy texture sources are never indexed here. */
final class ModernAssetAliases
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicReference<Map<Path, View>> views = new AtomicReference<>(Map.of());

    record Input(Path assets, boolean immutable, Set<String> entryModels, Map<String, String> renamedModels)
    {
        Input(Path assets, boolean immutable)
        {
            this(assets, immutable, Set.of(), Map.of());
        }

        Input(Path assets, boolean immutable, Set<String> entryModels)
        {
            this(assets, immutable, entryModels, Map.of());
        }
    }

    record View(Map<String, String> sources, Map<String, byte[]> json, Set<String> hidden)
    {
        static final View EMPTY = new View(Map.of(), Map.of(), Set.of());
    }

    record Assets(Map<String, String> textures, Map<String, JsonObject> models,
                          Map<String, JsonObject> blockstates, Set<String> metadata) {}

    private ModernAssetAliases() {}

    static View forPack(Path path)
    {
        return views.get().getOrDefault(path.toAbsolutePath().normalize(), View.EMPTY);
    }

    /** Called during resource discovery, after generated models and modern texture copies are available. */
    static void rebuild(List<IContentProvider> providers)
    {
        List<Input> inputs = new ArrayList<>();
        List<Assets> cachedAssets = new ArrayList<>();
        List<IContentProvider> indexed = new ArrayList<>();
        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(ContentLoadingConfig.getContentLoadingThreads()))
        {
            // The main mod's built-in assets are immutable too; reserve their names before user packs.
            var modFile = ModList.get().getModFileById(FlansMod.MOD_ID).getFile();
            Path builtinAssets = modFile.findResource("assets", FlansMod.FLANSMOD_ID);
            inputs.add(new Input(builtinAssets, true));
            cachedAssets.add(PackAssetIndex.modern(builtinAssets,
                Files.isRegularFile(modFile.getFilePath()) ? modFile.getFilePath() : null,
                modFile.getFilePath()));
            for (IContentProvider provider : providers)
            {
                if (!provider.shouldIndexAssetsForConflicts())
                    continue;
                indexed.add(provider);
                Set<String> entryModels = new HashSet<>();
                Map<String, String> renamedModels = new HashMap<>();
                ContentManager.listItems(provider).forEach(config -> {
                    entryModels.add("item/" + config.getShortName());
                    if (!config.getShortName().equals(config.getOriginalShortName()))
                        renamedModels.put("item/" + config.getOriginalShortName(), "item/" + config.getShortName());
                });
                ContentManager.listBlocks(provider).forEach(config -> {
                    entryModels.add("block/" + config.getShortName());
                    if (!config.getShortName().equals(config.getOriginalShortName()))
                        renamedModels.put("block/" + config.getOriginalShortName(), "block/" + config.getShortName());
                });
                // The cached overload does not use Input.assets; an archive path avoids opening it.
                inputs.add(new Input(provider.getPath(), provider.isPreprocessed(), entryModels, renamedModels));
            }
            // Each pack's index is independent; the plan below takes them in pack order.
            cachedAssets.addAll(workers.map(indexed, provider -> {
                try
                {
                    return PackAssetIndex.modern(provider);
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            }));
            List<View> plans = plan(inputs, cachedAssets);
            Map<Path, View> rebuilt = new HashMap<>();
            for (int i = 0; i < indexed.size(); i++)
            {
                IContentProvider provider = indexed.get(i);
                if (!provider.isPreprocessed())
                {
                    View view = plans.get(i + 1);
                    rebuilt.put(provider.getPath().toAbsolutePath().normalize(), view);
                    if (!view.sources().isEmpty())
                        FlansLog.log.info("Aliased {} modern asset resources in [{}]", view.sources().size(), provider.getName());
                }
            }
            views.set(Map.copyOf(rebuilt));
        }
        catch (IOException | RuntimeException e)
        {
            views.set(Map.of());
            FlansLog.log.error("Could not resolve modern content-pack asset conflicts", e);
        }
        finally
        {
            // Later discoveries walk the packs again: their files may have been edited since.
            ContentFileCache.releaseRunSnapshots();
        }
    }

    static List<View> plan(List<Input> inputs) throws IOException
    {
        List<Assets> assets = new ArrayList<>();
        for (Input input : inputs)
            assets.add(read(input.assets()));
        return plan(inputs, assets);
    }

    static List<View> plan(List<Input> inputs, List<Assets> assets)
    {
        Set<String> reservedTextures = new HashSet<>();
        Set<String> reservedModels = new HashSet<>();
        for (Assets pack : assets)
        {
            reservedTextures.addAll(pack.textures().keySet());
            reservedModels.addAll(pack.models().keySet());
        }

        Map<String, String> registeredTextures = new HashMap<>();
        Map<String, Map<String, String>> textureVariants = new HashMap<>();
        Map<String, Integer> registeredModels = new HashMap<>();
        // Registry entry models cannot move: Minecraft requests them by item/block ID. Give them
        // priority over helper/paintjob models, even when the latter belong to an earlier pack.
        for (int i = 0; i < inputs.size(); i++)
            if (inputs.get(i).immutable())
                for (String model : assets.get(i).models().keySet())
                    registeredModels.putIfAbsent(model, i);
        for (int i = 0; i < inputs.size(); i++)
            for (String model : inputs.get(i).entryModels())
                registeredModels.putIfAbsent(model, i);
        List<View> result = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++)
        {
            Assets pack = assets.get(i);
            boolean immutable = inputs.get(i).immutable();
            Map<String, String> textureAliases = new HashMap<>();
            Map<String, String> modelAliases = new HashMap<>();
            Map<String, String> sources = new LinkedHashMap<>();
            Map<String, byte[]> json = new LinkedHashMap<>();
            Set<String> hidden = new HashSet<>();
            Set<String> shadowedModels = new HashSet<>();
            inputs.get(i).renamedModels().forEach((original, renamed) -> {
                if (!original.equals(renamed) && pack.models().containsKey(original))
                    shadowedModels.add(renamed);
            });

            for (Map.Entry<String, String> texture : pack.textures().entrySet())
            {
                String original = texture.getKey();
                String existing = registeredTextures.get(original);
                Map<String, String> variants = textureVariants.computeIfAbsent(original, ignored -> new HashMap<>());
                String alias = original;
                if (!immutable && existing != null && !existing.equals(texture.getValue()))
                    alias = variants.computeIfAbsent(texture.getValue(), ignored -> allocate(original, reservedTextures));
                else if (existing == null || existing.equals(texture.getValue()))
                    variants.putIfAbsent(texture.getValue(), original);
                registeredTextures.putIfAbsent(alias, texture.getValue());
                textureAliases.put(original, alias);
                expose("textures/" + original + ".png", "textures/" + alias + ".png", sources, hidden);
                if (pack.metadata().contains(original))
                    expose("textures/" + original + ".png.mcmeta", "textures/" + alias + ".png.mcmeta", sources, hidden);
            }

            // Allocate the whole model graph before rewriting its edges. Even equal JSON can resolve to
            // different textures/parents in two packs; isolating colliding models also handles cycles.
            for (String original : pack.models().keySet())
            {
                if (shadowedModels.contains(original))
                    continue;
                String desired = inputs.get(i).renamedModels().getOrDefault(original, original);
                Integer owner = registeredModels.get(desired);
                String alias = !immutable && owner != null && owner != i
                    ? allocate(desired, reservedModels) : desired;
                registeredModels.putIfAbsent(alias, i);
                modelAliases.put(original, alias);
                expose("models/" + original + ".json", "models/" + alias + ".json", sources, hidden);
            }
            inputs.get(i).renamedModels().forEach((original, renamed) -> {
                if (!pack.models().containsKey(original) && pack.models().containsKey(renamed))
                    modelAliases.put(original, modelAliases.get(renamed));
            });

            if (!immutable)
            {
                for (Map.Entry<String, JsonObject> model : pack.models().entrySet())
                {
                    if (shadowedModels.contains(model.getKey()))
                        continue;
                    JsonObject rewritten = model.getValue().deepCopy();
                    rewriteModel(rewritten, textureAliases, modelAliases);
                    if (!rewritten.equals(model.getValue()))
                        json.put("models/" + modelAliases.get(model.getKey()) + ".json", bytes(rewritten));
                }
                for (Map.Entry<String, JsonObject> state : pack.blockstates().entrySet())
                {
                    if (shadowedModels.contains("block/" + state.getKey()))
                        continue;
                    JsonObject rewritten = state.getValue().deepCopy();
                    rewriteBlockstate(rewritten, modelAliases);
                    String name = inputs.get(i).renamedModels().getOrDefault("block/" + state.getKey(), "block/" + state.getKey()).substring(6);
                    expose("blockstates/" + state.getKey() + ".json", "blockstates/" + name + ".json", sources, hidden);
                    if (!rewritten.equals(state.getValue()))
                        json.put("blockstates/" + name + ".json", bytes(rewritten));
                }
            }
            result.add(new View(Map.copyOf(sources), Map.copyOf(json), Set.copyOf(hidden)));
        }
        return result;
    }

    static Assets read(Path root) throws IOException
    {
        Map<String, String> textures = new LinkedHashMap<>();
        Set<String> metadata = new HashSet<>();
        for (String folder : List.of("item", "block"))
        {
            Path textureRoot = root.resolve("textures");
            for (Path file : files(textureRoot.resolve(folder), ".png"))
            {
                String name = relative(textureRoot, file);
                name = name.substring(0, name.length() - 4);
                Path mcmeta = file.resolveSibling(file.getFileName() + ".mcmeta");
                String signature = ContentFileCache.digest(file);
                if (Files.isRegularFile(mcmeta))
                {
                    signature += ":" + ContentFileCache.digest(mcmeta);
                    metadata.add(name);
                }
                textures.put(name, signature);
            }
        }
        return new Assets(textures, readJson(root.resolve("models")), readJson(root.resolve("blockstates")), metadata);
    }

    private static Map<String, JsonObject> readJson(Path root) throws IOException
    {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (Path file : files(root, ".json"))
        {
            try
            {
                JsonElement value = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (value.isJsonObject())
                {
                    String path = relative(root, file);
                    result.put(path.substring(0, path.length() - 5), value.getAsJsonObject());
                }
            }
            catch (RuntimeException e)
            {
                FlansLog.log.warn("Cannot rewrite malformed asset JSON '{}': {}", file, e.toString());
            }
        }
        return result;
    }

    private static List<Path> files(Path root, String extension) throws IOException
    {
        if (!Files.isDirectory(root))
            return List.of();
        try (Stream<Path> walk = Files.walk(root))
        {
            return walk.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(extension))
                .sorted().toList();
        }
    }

    private static String relative(Path root, Path file)
    {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static String allocate(String original, Set<String> reserved)
    {
        for (int suffix = 2; ; suffix++)
        {
            String candidate = original + "_" + suffix;
            if (reserved.add(candidate))
                return candidate;
        }
    }

    private static void expose(String original, String alias, Map<String, String> sources, Set<String> hidden)
    {
        if (!original.equals(alias))
        {
            sources.put(alias, original);
            hidden.add(original);
            // Hide the original sidecar too, so this pack cannot replace the first pack's metadata.
            if (original.endsWith(".png"))
                hidden.add(original + ".mcmeta");
        }
    }

    private static byte[] bytes(JsonObject value)
    {
        return GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
    }

    private static void rewriteModel(JsonObject model, Map<String, String> textures, Map<String, String> models)
    {
        if (model.has("textures") && model.get("textures").isJsonObject())
        {
            JsonObject slots = model.getAsJsonObject("textures");
            for (String key : List.copyOf(slots.keySet()))
                rewriteReference(slots, key, textures);
        }
        rewriteReference(model, "parent", models);
        if (model.has("overrides") && model.get("overrides").isJsonArray())
            for (JsonElement override : model.getAsJsonArray("overrides"))
                if (override.isJsonObject())
                    rewriteReference(override.getAsJsonObject(), "model", models);
    }

    private static void rewriteBlockstate(JsonObject state, Map<String, String> models)
    {
        if (state.has("variants") && state.get("variants").isJsonObject())
            for (Map.Entry<String, JsonElement> variant : state.getAsJsonObject("variants").entrySet())
                rewriteModelChoices(variant.getValue(), models);
        if (state.has("multipart") && state.get("multipart").isJsonArray())
            for (JsonElement part : state.getAsJsonArray("multipart"))
                if (part.isJsonObject())
                    rewriteModelChoices(part.getAsJsonObject().get("apply"), models);
    }

    private static void rewriteModelChoices(JsonElement choices, Map<String, String> models)
    {
        if (choices == null)
            return;
        if (choices.isJsonArray())
            for (JsonElement choice : choices.getAsJsonArray())
                rewriteModelChoices(choice, models);
        else if (choices.isJsonObject())
            rewriteReference(choices.getAsJsonObject(), "model", models);
    }

    private static void rewriteReference(JsonObject object, String key, Map<String, String> aliases)
    {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            return;
        String reference = value.getAsString();
        String prefix = FlansMod.FLANSMOD_ID + ":";
        String normalized = reference.toLowerCase(java.util.Locale.ROOT).replace(prefix + "items/", prefix + "item/");
        if (!normalized.startsWith(prefix))
            return; // Unqualified resource names belong to minecraft; #variables are resolved by the model baker.
        String original = normalized.substring(prefix.length());
        String alias = aliases.get(original);
        String rewritten = alias == null ? normalized : prefix + alias;
        if (!rewritten.equals(reference))
            object.addProperty(key, rewritten);
    }
}
