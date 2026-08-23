package com.flansmodultimate;

import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Supplies 26.1 item-definition entrypoints for legacy content-pack items.
 * Legacy packs continue to own their {@code models/item}, textures and model
 * classes; this compatibility pack only bridges Minecraft's new {@code items}
 * resource directory to those unchanged assets.
 */
public final class LegacyItemDefinitionRepositorySource
{
    private static final String PACK_ID = FlansMod.MOD_ID + ":legacy_item_definitions";

    private LegacyItemDefinitionRepositorySource()
    {
    }

    public static RepositorySource create()
    {
        return acceptor -> {
            PackLocationInfo location = new PackLocationInfo(PACK_ID,
                Component.literal("Flan legacy item definitions"), PackSource.BUILT_IN, Optional.empty());
            Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier()
            {
                @Override
                public PackResources openPrimary(PackLocationInfo packLocation)
                {
                    return new Resources(packLocation);
                }

                @Override
                public PackResources openFull(PackLocationInfo packLocation, Pack.Metadata metadata)
                {
                    return new Resources(packLocation);
                }
            };
            PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, false);
            Pack pack = Pack.readMetaAndCreate(location, supplier, PackType.CLIENT_RESOURCES, selection);
            if (pack != null)
                acceptor.accept(pack);
        };
    }

    private static final class Resources implements PackResources
    {
        private static final Gson GSON = new Gson();
        private final PackLocationInfo location;
        private final Map<Identifier, byte[]> definitions;

        private Resources(PackLocationInfo location)
        {
            this.location = location;
            definitions = createDefinitions();
        }

        private static Map<Identifier, byte[]> createDefinitions()
        {
            Map<Identifier, byte[]> result = new LinkedHashMap<>();
            BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(FlansMod.FLANSMOD_ID))
                .sorted()
                .forEach(id -> {
                    Object item = BuiltInRegistries.ITEM.getValue(id);
                    if (!(item instanceof IFlanItem<?> flanItem))
                        return;

                    JsonObject model = new JsonObject();
                    model.addProperty("type", FlansMod.MOD_ID + ":legacy_item");
                    model.addProperty("model", id.getNamespace() + ":item/" + id.getPath());

                    if (flanItem instanceof IPaintableItem<?> paintableItem)
                    {
                        JsonArray paintjobs = new JsonArray();
                        for (Paintjob paintjob : paintableItem.getPaintableType().getPaintjobs().values())
                        {
                            if (paintjob.isDefault())
                                continue;
                            JsonObject entry = new JsonObject();
                            entry.addProperty("id", paintjob.getId());
                            entry.addProperty("model", FlansMod.FLANSMOD_ID + ":item/" + paintjob.getIcon());
                            paintjobs.add(entry);
                        }
                        if (!paintjobs.isEmpty())
                            model.add("paintjobs", paintjobs);
                    }

                    JsonObject root = new JsonObject();
                    root.add("model", model);
                    Identifier resource = Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json");
                    result.put(resource, GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
                });
            return Map.copyOf(result);
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String @NotNull ... path)
        {
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull Identifier resource)
        {
            if (type != PackType.CLIENT_RESOURCES)
                return null;
            byte[] bytes = definitions.get(resource);
            return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
        }

        @Override
        public void listResources(@NotNull PackType type, @NotNull String namespace,
                                  @NotNull String path, @NotNull ResourceOutput output)
        {
            if (type != PackType.CLIENT_RESOURCES || !namespace.equals(FlansMod.FLANSMOD_ID))
                return;
            definitions.forEach((resource, bytes) -> {
                if (resource.getPath().startsWith(path))
                    output.accept(resource, () -> new ByteArrayInputStream(bytes));
            });
        }

        @Override
        public @NotNull Set<String> getNamespaces(@NotNull PackType type)
        {
            return type == PackType.CLIENT_RESOURCES ? Set.of(FlansMod.FLANSMOD_ID) : Set.of();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMetadataSection(@NotNull MetadataSectionType<T> serializer)
        {
            MetadataSectionType<PackMetadataSection> packMetadataType = PackMetadataSection.forPackType(PackType.CLIENT_RESOURCES);
            if (serializer != packMetadataType)
                return null;
            var currentFormat = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
            return (T)new PackMetadataSection(location.title(), new InclusiveRange<>(currentFormat));
        }

        @Override
        public @NotNull String packId()
        {
            return location.id();
        }

        @Override
        public PackLocationInfo location()
        {
            return location;
        }

        @Override
        public void close()
        {
        }
    }
}
