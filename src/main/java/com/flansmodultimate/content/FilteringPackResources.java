package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public final class FilteringPackResources implements PackResources
{
    private final PackResources delegate;
    private final PackType packType;
    private final ModernAssetAliases.View aliases;

    FilteringPackResources(PackResources delegate, PackType packType, ModernAssetAliases.View aliases)
    {
        this.delegate = delegate;
        this.packType = packType;
        this.aliases = aliases;
    }

    private static boolean isExcluded(PackType type, ResourceLocation location)
    {
        if (type != PackType.CLIENT_RESOURCES)
            return false;

        return location.getNamespace().equals(FlansMod.FLANSMOD_ID)
            && (location.getPath().startsWith(ContentPackPaths.FOLDER_TEXTURES_ARMOR + "/") || location.getPath().startsWith(ContentPackPaths.FOLDER_TEXTURES_GUI + "/")
                || location.getPath().startsWith(ContentPackPaths.FOLDER_TEXTURES_SKINS + "/") || location.getPath().startsWith(ContentPackPaths.FOLDER_SOUND + "/")
                || location.getPath().startsWith(ContentPackPaths.FOLDER_TEXTURES + "/" + ContentPackPaths.FOLDER_TEXTURES_ITEMS + "/")
                || location.getPath().startsWith(ContentPackPaths.FOLDER_TEXTURES + "/" + ContentPackPaths.FOLDER_TEXTURES_BLOCKS + "/"));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String @NotNull... path)
    {
        return delegate.getRootResource(path);
    }

    @Override
    public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location)
    {
        if (isExcluded(type, location))
            return null;

        if (type == PackType.CLIENT_RESOURCES && location.getNamespace().equals(FlansMod.FLANSMOD_ID))
        {
            String path = location.getPath();
            if (aliases.hidden().contains(path))
                return null;
            byte[] json = aliases.json().get(path);
            if (json != null)
                return () -> new ByteArrayInputStream(json);
            String source = aliases.sources().get(path);
            if (source != null)
                return delegate.getResource(type, ResourceLocation.fromNamespaceAndPath(location.getNamespace(), source));
        }

        return delegate.getResource(type, location);
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput output)
    {
        delegate.listResources(type, namespace, path, (location, supplier) ->
        {
            IoSupplier<InputStream> resource = getResource(type, location);
            if (resource != null)
                output.accept(location, resource);
        });
        if (type == PackType.CLIENT_RESOURCES && namespace.equals(FlansMod.FLANSMOD_ID))
        {
            for (Map.Entry<String, String> alias : aliases.sources().entrySet())
            {
                if (!path.isEmpty() && !alias.getKey().startsWith(path + "/"))
                    continue;
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, alias.getKey());
                IoSupplier<InputStream> resource = getResource(type, location);
                if (resource != null)
                    output.accept(location, resource);
            }
        }
    }

    @Override
    @NotNull
    public Set<String> getNamespaces(@NotNull PackType type)
    {
        return delegate.getNamespaces(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer) throws IOException
    {
        if (serializer == PackMetadataSection.TYPE)
        {
            PackMetadataSection metadata = delegate.getMetadataSection(PackMetadataSection.TYPE);
            if (metadata == null)
                return null;

            // Flan packs are transformed by ContentManager before they reach
            // Minecraft. Their original pack.mcmeta often targets a much older
            // game, so report the transformed pack as compatible without
            // modifying the user's archive on every version switch.
            int currentFormat = SharedConstants.getCurrentVersion().getPackVersion(packType);
            return (T) new PackMetadataSection(metadata.getDescription(), currentFormat);
        }

        return delegate.getMetadataSection(serializer);
    }

    @Override
    @NotNull
    public String packId()
    {
        return delegate.packId();
    }

    @Override
    public boolean isBuiltin()
    {
        return delegate.isBuiltin();
    }

    @Override
    public void close()
    {
        delegate.close();
    }
}
