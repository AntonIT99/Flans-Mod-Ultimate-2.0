package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ContentLoadingConfig;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.SoundLengthIndex;
import com.google.gson.Gson;
import net.minecraftforge.fml.ModList;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.flag.FeatureFlagSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Sound priority is independent of definition loading, texture aliases, and recipe pack order. */
public final class SoundPriority
{
    // Required TOP packs are inserted in reverse key order. Sort before packaged asset packs.
    static final String PACK_ID = "!flansmodultimate_sound_priority";
    private static final AtomicReference<State> state = new AtomicReference<>();

    record Source(String id, Path root, Path archive)
    {
        PackResources open()
        {
            return archive == null ? new PathPackResources(id, root, true) : new FilePackResources(id, archive.toFile(), true);
        }
    }

    private record State(List<Source> sources, SoundPriorityPlan plan, byte[] json)
    {
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (!(obj instanceof State other))
                return false;
            return Objects.equals(sources, other.sources) && Objects.equals(plan, other.plan) && Arrays.equals(json, other.json);
        }

        @Override
        public int hashCode()
        {
            return 31 * Objects.hash(sources, plan) + Arrays.hashCode(json);
        }

        @Override
        @NotNull
        public String toString()
        {
            return "State[sources=" + sources + ", plan=" + plan + ", json=" + Arrays.toString(json) + "]";
        }
    }

    private SoundPriority()
    {}

    /** Measured duration independent of whether type-defined sound timers are overridden. */
    public static OptionalInt measuredLength(String path)
    {
        State current = state.get();
        Integer length = current == null ? null : current.plan().lengths().get(path);
        return length != null && length > 0 ? OptionalInt.of(length) : OptionalInt.empty();
    }

    static void initialize(List<IContentProvider> providers)
    {
        initialize(providers, ContentLoadingWorkers.sequential());
    }

    /** Indexes the sound sources on the workers; the plan is built from them in priority order. */
    static void initialize(List<IContentProvider> providers, ContentLoadingWorkers workers)
    {
        Map<String, Source> discovered = new LinkedHashMap<>();
        for (var module : PackagedContentLoader.getRegisteredModules().stream().sorted(Comparator.comparing(PackagedContentLoader.RegisteredModule::modId)).toList())
        {
            Path archive = ModList.get().getModFileById(module.modId()).getFile().getFilePath();
            discovered.put("mod:" + module.modId(), new Source("mod:" + module.modId(), module.resourceRoot(), Files.isRegularFile(archive) ? archive : null));
        }
        var builtin = ModList.get().getModFileById(FlansMod.MOD_ID).getFile();
        Path builtinRoot = builtin.findResource("assets", FlansMod.FLANSMOD_ID).getParent().getParent();
        discovered.put("mod:" + FlansMod.MOD_ID, new Source("mod:" + FlansMod.MOD_ID, builtinRoot, Files.isRegularFile(builtin.getFilePath()) ? builtin.getFilePath() : null));
        for (IContentProvider provider : providers)
            if (!provider.isPreprocessed())
            {
                String id = standaloneId(provider);
                discovered.put(id, new Source(id, provider.getPath(), provider.isArchive() ? provider.getPath() : null));
            }
        List<String> configured = ContentLoadingConfig.synchronizeSoundPackPriority(List.copyOf(discovered.keySet()));
        List<Source> sources = orderedSources(discovered, configured);
        List<IndexedSource> indexed = workers.map(sources, SoundPriority::index);
        List<SoundPriorityPlan.Assets> assets = new ArrayList<>();
        for (IndexedSource source : indexed)
        {
            if (source.assets() != null)
                assets.add(source.assets());
            else
            {
                FlansLog.log.error("Could not index sound source '{}': {}", source.id(), source.error());
                assets.add(new SoundPriorityPlan.Assets(new com.google.gson.JsonObject(), Map.of()));
            }
        }
        SoundPriorityPlan plan = SoundPriorityPlan.create(assets);
        state.set(new State(sources, plan, new Gson().toJson(plan.events()).getBytes(StandardCharsets.UTF_8)));
        SoundLengthIndex.replace(plan.lengths());
        FlansLog.log.info("Sound source priority (highest first): {}", sources.stream().map(Source::id).toList());
    }

    private record IndexedSource(String id, SoundPriorityPlan.Assets assets, String error)
    {}

    private static IndexedSource index(Source source)
    {
        try
        {
            return new IndexedSource(source.id(), SoundAssetIndex.read(source), null);
        }
        catch (IOException | RuntimeException exception)
        {
            return new IndexedSource(source.id(), null, exception.toString());
        }
    }

    static String standaloneId(IContentProvider provider)
    {
        String name = provider.getPath().getFileName().toString();
        return "pack:" + (provider.isArchive() ? FilenameUtils.getBaseName(name) : name).toLowerCase(Locale.ROOT);
    }

    static List<Source> orderedSources(Map<String, Source> discovered, List<String> configured)
    {
        Map<String, Source> remaining = new LinkedHashMap<>(discovered);
        List<Source> sources = new ArrayList<>();
        for (String id : configured)
        {
            Source source = remaining.remove(id);
            if (source != null)
                sources.add(source);
        }
        sources.addAll(remaining.values());
        return List.copyOf(sources);
    }

    public static RepositorySource repositorySource()
    {
        return acceptor ->
        {
            State current = state.get();
            if (current == null)
                return;
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            Pack.Info info = new Pack.Info(Component.literal("Flan sound priority"), format, format, FeatureFlagSet.of(), false);
            Pack.ResourcesSupplier resources = id -> new SoundPriorityPackResources(id, current.sources(), current.plan(), current.json());
            acceptor.accept(Pack.create(PACK_ID, Component.literal("Flan sound priority"), true, resources, info, PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, PackSource.BUILT_IN));
        };
    }
}
