package com.flansmodultimate;

import com.flansmodultimate.common.item.GloveItem;
import com.flansmodultimate.common.types.EnumType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Supplies item tags for content-pack items registered before datapack reload. */
public final class EnchantmentItemRepositorySource
{
    private static final String DESCRIPTION = "Flan enchantable items";
    private static final ResourceLocation GLOVES = id("tags/item/enchantable/gloves.json");
    private static final ResourceLocation OFFHAND = id("tags/item/enchantable/offhand.json");
    private static final ResourceLocation ARMOR = id("tags/item/enchantable/armor.json");

    private EnchantmentItemRepositorySource() {}

    public static RepositorySource create()
    {
        return acceptor -> {
            PackLocationInfo location = new PackLocationInfo(FlansMod.MOD_ID + ":enchantable_items",
                Component.literal(DESCRIPTION), PackSource.BUILT_IN, Optional.empty());
            Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier()
            {
                @Override
                public PackResources openPrimary(PackLocationInfo info)
                {
                    return new Resources(info);
                }

                @Override
                public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata)
                {
                    return new Resources(info);
                }
            };
            Pack.Metadata metadata = new Pack.Metadata(Component.literal(DESCRIPTION),
                PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
            acceptor.accept(new Pack(location, resources, metadata,
                new PackSelectionConfig(true, Pack.Position.TOP, false)));
        };
    }

    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, path);
    }

    private static byte[] tag(List<String> values)
    {
        JsonObject object = new JsonObject();
        JsonArray entries = new JsonArray();
        values.forEach(entries::add);
        object.add("values", entries);
        return object.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static List<String> itemIds(EnumType type)
    {
        List<String> result = new ArrayList<>();
        for (DeferredHolder<Item, ? extends Item> holder : FlansMod.getItems(type))
            result.add(holder.getId().toString());
        return result;
    }

    private static List<String> idsMatching(Predicate<Item> predicate)
    {
        TreeSet<String> ids = new TreeSet<>();
        for (Item item : BuiltInRegistries.ITEM)
            if (predicate.test(item))
                ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
        return List.copyOf(ids);
    }

    private static Map<ResourceLocation, byte[]> resources()
    {
        TreeSet<String> gloves = new TreeSet<>(itemIds(EnumType.GLOVE));
        gloves.addAll(idsMatching(item -> item instanceof GloveItem));
        TreeSet<String> offhand = new TreeSet<>(gloves);
        offhand.addAll(idsMatching(item -> item instanceof ShieldItem));
        TreeSet<String> armor = new TreeSet<>(itemIds(EnumType.ARMOR));
        armor.add("#minecraft:enchantable/armor");
        armor.addAll(idsMatching(item -> item instanceof ArmorItem));
        return Map.of(GLOVES, tag(List.copyOf(gloves)), OFFHAND, tag(List.copyOf(offhand)),
            ARMOR, tag(List.copyOf(armor)));
    }

    private static final class Resources implements PackResources
    {
        private final PackLocationInfo location;
        private final Map<ResourceLocation, byte[]> entries;

        private Resources(PackLocationInfo location)
        {
            this.location = location;
            this.entries = resources();
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String @NotNull ... path)
        {
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location)
        {
            byte[] data = type == PackType.SERVER_DATA ? entries.get(location) : null;
            return data == null ? null : () -> new ByteArrayInputStream(data);
        }

        @Override
        public void listResources(@NotNull PackType type, @NotNull String namespace,
                                  @NotNull String path, @NotNull ResourceOutput output)
        {
            if (type != PackType.SERVER_DATA || !namespace.equals(FlansMod.MOD_ID))
                return;
            entries.forEach((location, data) -> {
                if (location.getPath().startsWith(path))
                    output.accept(location, () -> new ByteArrayInputStream(data));
            });
        }

        @Override
        public @NotNull Set<String> getNamespaces(@NotNull PackType type)
        {
            return type == PackType.SERVER_DATA ? Set.of(FlansMod.MOD_ID) : Set.of();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer)
        {
            if (serializer != PackMetadataSection.TYPE)
                return null;
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
            return (T) new PackMetadataSection(Component.literal(DESCRIPTION), format);
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
        public void close() {}
    }
}
