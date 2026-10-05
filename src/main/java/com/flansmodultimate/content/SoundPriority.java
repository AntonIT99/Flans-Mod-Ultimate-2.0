package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ContentLoadingConfig;
import com.flansmodultimate.util.SoundLengthIndex;
import com.google.gson.Gson;
import net.minecraftforge.fml.ModList;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Sound priority is independent of definition loading, texture aliases, and recipe pack order. */
public final class SoundPriority
{
    // Required TOP packs are inserted in reverse key order. Sort before packaged asset packs.
    static final String PACK_ID = "!flansmodultimate_sound_priority";
    private static final Logger LOGGER = FlansMod.log;
    private static final AtomicReference<State> state = new AtomicReference<>();

    record Source(String id, Path root, Path archive)
    {
        PackResources open()
        {
            return archive == null ? new PathPackResources(id, root, true)
                : new FilePackResources(id, archive.toFile(), true);
        }
    }

    private record State(List<Source> sources, SoundPriorityPlan plan, byte[] json)
    {
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof State other))
                return false;
            return Objects.equals(sources, other.sources)
                && Objects.equals(plan, other.plan)
                && Arrays.equals(json, other.json);
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
            return "State[sources=" + sources
                + ", plan=" + plan
                + ", json=" + Arrays.toString(json)
                + "]";
        }
    }

    private SoundPriority() {}

    static void initialize(List<IContentProvider> providers)
    {
        Map<String, Source> discovered = new LinkedHashMap<>();
        for (var module : PackagedContentLoader.getRegisteredModules().stream()
            .sorted(Comparator.comparing(PackagedContentLoader.RegisteredModule::modId)).toList())
        {
            Path archive = ModList.get().getModFileById(module.modId()).getFile().getFilePath();
            discovered.put("mod:" + module.modId(), new Source("mod:" + module.modId(), module.resourceRoot(),
                Files.isRegularFile(archive) ? archive : null));
        }
        var builtin = ModList.get().getModFileById(FlansMod.MOD_ID).getFile();
        Path builtinRoot = builtin.findResource("assets", FlansMod.FLANSMOD_ID).getParent().getParent();
        discovered.put("mod:" + FlansMod.MOD_ID, new Source("mod:" + FlansMod.MOD_ID, builtinRoot,
            Files.isRegularFile(builtin.getFilePath()) ? builtin.getFilePath() : null));
        for (IContentProvider provider : providers)
            if (!provider.isPreprocessed())
            {
                String id = standaloneId(provider);
                discovered.put(id, new Source(id, provider.getPath(), provider.isArchive() ? provider.getPath() : null));
            }
        List<String> configured = ContentLoadingConfig.synchronizeSoundPackPriority(List.copyOf(discovered.keySet()));
        List<Source> sources = orderedSources(discovered, configured);
        List<SoundPriorityPlan.Assets> assets = new ArrayList<>();
        for (Source source : sources)
            try
            {
                assets.add(SoundAssetIndex.read(source));
            }
            catch (IOException | RuntimeException exception)
            {
                LOGGER.error("Could not index sound source '{}': {}", source.id(), exception.toString());
                assets.add(new SoundPriorityPlan.Assets(new com.google.gson.JsonObject(), Map.of()));
            }
        SoundPriorityPlan plan = SoundPriorityPlan.create(assets);
        state.set(new State(sources, plan, new Gson().toJson(plan.events()).getBytes(StandardCharsets.UTF_8)));
        SoundLengthIndex.replace(plan.lengths());
        LOGGER.info("Sound source priority (highest first): {}", sources.stream().map(Source::id).toList());
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
        return acceptor -> {
            State current = state.get();
            if (current == null)
                return;
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            Pack.Info info = new Pack.Info(Component.literal("Flan sound priority"), format, format, FeatureFlagSet.of(), false);
            Pack.ResourcesSupplier resources = id -> new SoundPriorityPackResources(id, current.sources(), current.plan(), current.json());
            acceptor.accept(Pack.create(PACK_ID, Component.literal("Flan sound priority"), true, resources,
                info, PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, PackSource.BUILT_IN));
        };
    }
}
