package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/** Exposes only selected Flan audio and the selected event definitions. No texture or data resources. */
final class SoundPriorityPackResources implements PackResources
{
    private final PackLocationInfo location;
    private final List<PackResources> sources;
    private final SoundPriorityPlan plan;
    private final byte[] json;

    SoundPriorityPackResources(PackLocationInfo location, List<SoundPriority.Source> sources, SoundPriorityPlan plan, byte[] json)
    {
        this.location = location;
        this.sources = sources.stream().map(SoundPriority.Source::open).toList();
        this.plan = plan;
        this.json = json;
    }

    @Override
    public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location)
    {
        if (type != PackType.CLIENT_RESOURCES || !location.getNamespace().equals(FlansMod.FLANSMOD_ID))
            return null;
        if (location.getPath().equals("sounds.json"))
            return () -> new ByteArrayInputStream(json);
        String path = location.getPath();
        if (!path.startsWith("sounds/") || !path.endsWith(".ogg"))
            return null;
        Integer owner = plan.fileOwners().get(path.substring(7, path.length() - 4));
        return owner == null ? null : sources.get(owner).getResource(type, location);
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput output)
    {
        if (type != PackType.CLIENT_RESOURCES || !namespace.equals("flansmod"))
            return;
        for (String file : plan.fileOwners().keySet())
        {
            String resource = "sounds/" + file + ".ogg";
            if (!path.isEmpty() && !resource.startsWith(path + "/"))
                continue;
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, resource);
            IoSupplier<InputStream> stream = getResource(type, location);
            if (stream != null)
                output.accept(location, stream);
        }
    }

    @Override
    @NotNull
    public Set<String> getNamespaces(@NotNull PackType type)
    {
        return type == PackType.CLIENT_RESOURCES ? Set.of("flansmod") : Set.of();
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String @NotNull ... path)
    {
        return null;
    }

    @Override
    public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer)
    {
        return null;
    }

    @Override
    public @NotNull PackLocationInfo location()
    {
        return location;
    }

    @Override
    public void close()
    {
        sources.forEach(PackResources::close);
    }
}
