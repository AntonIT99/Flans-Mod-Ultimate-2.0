package com.flansmodultimate;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Gives generated or legacy Flan packs current-version metadata without
 * rewriting their files. Pack contents and every non-pack metadata section are
 * delegated unchanged.
 */
public class CompatiblePackResources implements PackResources
{
    protected final PackResources delegate;
    private final PackType packType;

    public CompatiblePackResources(PackResources delegate, PackType packType)
    {
        this.delegate = delegate;
        this.packType = packType;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String @NotNull ... path)
    {
        return delegate.getRootResource(path);
    }

    @Override
    public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull Identifier location)
    {
        return delegate.getResource(type, location);
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace,
                              @NotNull String path, @NotNull ResourceOutput output)
    {
        delegate.listResources(type, namespace, path, output);
    }

    @Override
    @NotNull
    public Set<String> getNamespaces(@NotNull PackType type)
    {
        return delegate.getNamespaces(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(@NotNull MetadataSectionType<T> serializer) throws IOException
    {
        MetadataSectionType<PackMetadataSection> packMetadataType = PackMetadataSection.forPackType(packType);
        if (serializer == packMetadataType)
        {
            PackMetadataSection metadata = delegate.getMetadataSection(packMetadataType);
            Component description = metadata != null ? metadata.description() : location().title();
            var currentFormat = SharedConstants.getCurrentVersion().packVersion(packType);
            return (T) new PackMetadataSection(description, new InclusiveRange<>(currentFormat));
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
    public PackLocationInfo location()
    {
        return delegate.location();
    }

    @Override
    public void close()
    {
        delegate.close();
    }
}
