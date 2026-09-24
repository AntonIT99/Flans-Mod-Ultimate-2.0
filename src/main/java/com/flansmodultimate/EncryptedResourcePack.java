package com.flansmodultimate;

import com.flansmodultimate.config.ModClientConfig;
import org.jetbrains.annotations.NotNull;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client resource pack backed by one authenticated encrypted bundle.
 * The bundle is decrypted at most once for each resource-pack instance and its
 * entries are kept in memory until the pack is closed on the next reload.
 */
public final class EncryptedResourcePack implements PackResources
{
    public static final String BUNDLE_RESOURCE_PATH = "flans_content/content.fmu";

    private static final byte[] ENCRYPTED_MAGIC = "FMUENC01".getBytes(StandardCharsets.US_ASCII);
    private static final int ENCRYPTED_VERSION = 1;
    private static final int ARCHIVE_MAGIC = 0x464D5552; // FMUR
    private static final int ARCHIVE_VERSION = 1;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final int MAX_ENTRY_COUNT = 65_536;
    private static final int MAX_ENTRY_BYTES = 64 * 1024 * 1024;
    private static final int MAX_ARCHIVE_BYTES = 512 * 1024 * 1024;
    private static final String KEY_SALT = "Flans Mod Ultimate uncensored resources v1";

    private final PackLocationInfo location;
    private final String keyId;
    private final Path bundlePath;
    private volatile Map<ResourceLocation, byte[]> entries;
    private volatile Set<String> namespaces = Set.of();

    public EncryptedResourcePack(PackLocationInfo location, String keyId, Path bundlePath)
    {
        this.location = location;
        this.keyId = keyId;
        this.bundlePath = bundlePath;
    }

    public static PackMetadataSection metadata()
    {
        int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
        return new PackMetadataSection(Component.literal("Optional uncensored Flan content"), format);
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String @NotNull ... path)
    {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location)
    {
        if (type != PackType.CLIENT_RESOURCES || !isEnabled())
            return null;

        byte[] data = getEntries().get(location);
        return data == null ? null : () -> new ByteArrayInputStream(data);
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path,
                              @NotNull ResourceOutput output)
    {
        if (type != PackType.CLIENT_RESOURCES || !isEnabled())
            return;

        String prefix = path.isEmpty() ? "" : path.endsWith("/") ? path : path + "/";
        getEntries().forEach((location, data) -> {
            if (location.getNamespace().equals(namespace)
                && (location.getPath().equals(path) || location.getPath().startsWith(prefix)))
            {
                output.accept(location, () -> new ByteArrayInputStream(data));
            }
        });
    }

    @Override
    @NotNull
    public Set<String> getNamespaces(@NotNull PackType type)
    {
        if (type != PackType.CLIENT_RESOURCES || !isEnabled())
            return Set.of();
        getEntries();
        return namespaces;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer)
    {
        return serializer == PackMetadataSection.TYPE ? (T) metadata() : null;
    }

    @Override
    @NotNull
    public String packId()
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
        entries = null;
        namespaces = Set.of();
    }

    private static boolean isEnabled()
    {
        return ModClientConfig.isUncensoredContentEnabled();
    }

    private Map<ResourceLocation, byte[]> getEntries()
    {
        Map<ResourceLocation, byte[]> current = entries;
        if (current != null)
            return current;

        synchronized (this)
        {
            if (entries == null)
            {
                try
                {
                    entries = Collections.unmodifiableMap(decryptBundle(bundlePath, keyId));
                    Set<String> loadedNamespaces = new LinkedHashSet<>();
                    entries.keySet().forEach(location -> loadedNamespaces.add(location.getNamespace()));
                    namespaces = Set.copyOf(loadedNamespaces);
                    FlansMod.log.info("Loaded {} encrypted uncensored resource(s) for {}.", entries.size(), keyId);
                }
                catch (Exception e)
                {
                    entries = Map.of();
                    namespaces = Set.of();
                    FlansMod.log.error("Could not load encrypted uncensored resources from {}.", bundlePath, e);
                }
            }
            return entries;
        }
    }

    static Map<ResourceLocation, byte[]> decryptBundle(Path bundlePath, String keyId) throws Exception
    {
        byte[] encryptedFile = Files.readAllBytes(bundlePath);
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(encryptedFile)))
        {
            byte[] magic = input.readNBytes(ENCRYPTED_MAGIC.length);
            if (!MessageDigest.isEqual(magic, ENCRYPTED_MAGIC))
                throw new IOException("Invalid encrypted resource bundle header");
            if (input.readInt() != ENCRYPTED_VERSION)
                throw new IOException("Unsupported encrypted resource bundle version");

            int ivLength = input.readUnsignedByte();
            if (ivLength != GCM_IV_BYTES)
                throw new IOException("Invalid encrypted resource bundle IV length: " + ivLength);
            byte[] iv = input.readNBytes(ivLength);
            int encryptedLength = input.readInt();
            if (encryptedLength < GCM_TAG_BITS / 8 || encryptedLength > MAX_ARCHIVE_BYTES)
                throw new IOException("Invalid encrypted resource bundle length: " + encryptedLength);
            byte[] encrypted = input.readNBytes(encryptedLength);
            if (encrypted.length != encryptedLength || input.read() != -1)
                throw new IOException("Truncated or trailing encrypted resource data");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(deriveKey(keyId), "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(aad(keyId));
            return readArchive(cipher.doFinal(encrypted));
        }
    }

    private static Map<ResourceLocation, byte[]> readArchive(byte[] archive) throws IOException
    {
        if (archive.length > MAX_ARCHIVE_BYTES)
            throw new IOException("Decrypted resource archive is too large");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(archive)))
        {
            if (input.readInt() != ARCHIVE_MAGIC || input.readInt() != ARCHIVE_VERSION)
                throw new IOException("Invalid decrypted resource archive header");
            int count = input.readInt();
            if (count < 0 || count > MAX_ENTRY_COUNT)
                throw new IOException("Invalid decrypted resource entry count: " + count);

            Map<ResourceLocation, byte[]> resources = new LinkedHashMap<>();
            int totalBytes = 0;
            for (int i = 0; i < count; i++)
            {
                String sourcePath = input.readUTF();
                int length = input.readInt();
                if (length < 0 || length > MAX_ENTRY_BYTES || totalBytes > MAX_ARCHIVE_BYTES - length)
                    throw new IOException("Invalid encrypted resource entry length for " + sourcePath);
                byte[] data = input.readNBytes(length);
                if (data.length != length)
                    throw new IOException("Truncated encrypted resource entry: " + sourcePath);
                totalBytes += length;

                ResourceLocation location = parseAssetPath(sourcePath);
                if (resources.putIfAbsent(location, data) != null)
                    throw new IOException("Duplicate encrypted resource: " + location);
            }
            if (input.read() != -1)
                throw new IOException("Trailing data in decrypted resource archive");
            return resources;
        }
    }

    private static ResourceLocation parseAssetPath(String sourcePath) throws IOException
    {
        if (!sourcePath.startsWith("assets/") || sourcePath.contains("\\") || sourcePath.contains(".."))
            throw new IOException("Invalid encrypted asset path: " + sourcePath);
        int namespaceEnd = sourcePath.indexOf('/', "assets/".length());
        if (namespaceEnd < 0 || namespaceEnd == sourcePath.length() - 1)
            throw new IOException("Invalid encrypted asset path: " + sourcePath);

        String namespace = sourcePath.substring("assets/".length(), namespaceEnd);
        String path = sourcePath.substring(namespaceEnd + 1);
        if (!(path.startsWith("lang/") || path.startsWith("textures/")))
            throw new IOException("Encrypted resources may only contain lang or textures assets: " + sourcePath);
        try
        {
            return ResourceLocation.fromNamespaceAndPath(namespace, path);
        }
        catch (RuntimeException e)
        {
            throw new IOException("Invalid encrypted resource location: " + sourcePath, e);
        }
    }

    private static byte[] deriveKey(String keyId) throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(KEY_SALT.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        return digest.digest(keyId.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] aad(String keyId) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(ENCRYPTED_MAGIC);
        output.write(keyId.getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }
}
