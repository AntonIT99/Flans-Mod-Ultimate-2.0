package com.flansmodultimate.platform.network;

import com.flansmodultimate.common.sync.ContentFingerprint;
import com.flansmodultimate.common.sync.EnumContentMismatch;
import com.flansmodultimate.util.FlansLog;
import io.netty.handler.codec.DecoderException;
import io.netty.util.AttributeKey;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Per-connection pack diagnostics, available before registry sync without depending on client classes. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentPackDiagnostics
{
    private static final int MAX_PACKS = 1024;
    private static final int MAX_VISIBLE_PACKS = 3;
    private static final int MAX_VISIBLE_NAME = 32;
    private static final AttributeKey<Map<String, EnumContentMismatch>> MISMATCHES = AttributeKey.valueOf("flansmodultimate:pack_mismatches");
    private static final String FORGE_REGISTRY_FAILURE = "Failed to synchronize registry data from server, closing connection";

    public static void encode(FriendlyByteBuf buffer, Map<String, String> fingerprints)
    {
        buffer.writeVarInt(fingerprints.size());
        fingerprints.forEach((name, hash) ->
        {
            buffer.writeUtf(name, 256);
            buffer.writeUtf(hash, 64);
        });
    }

    public static Map<String, String> decode(FriendlyByteBuf buffer)
    {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_PACKS)
            throw new DecoderException("Invalid content pack count: " + size);
        Map<String, String> fingerprints = new TreeMap<>();
        for (int i = 0; i < size; i++)
            fingerprints.put(buffer.readUtf(256), buffer.readUtf(64));
        return fingerprints;
    }

    public static void receive(Connection connection, Map<String, String> fingerprints)
    {
        connection.channel().attr(MISMATCHES).set(ContentFingerprint.compareWith(fingerprints));
    }

    /** Only registry failures are rewritten. Authentication, bans, timeouts and other disconnect reasons stay intact. */
    public static Component disconnectReason(Connection connection, Component reason)
    {
        boolean registryFailure = reason.getContents() instanceof TranslatableContents translation && translation.getKey().startsWith("neoforge.network.registries.sync.");
        if (!registryFailure && !FORGE_REGISTRY_FAILURE.equals(reason.getString()))
            return reason;

        FlansLog.log.warn("Registry synchronization failed: {}", reason.getString());
        Map<String, EnumContentMismatch> mismatches = connection.channel().attr(MISMATCHES).get();
        if (mismatches != null)
            mismatches.forEach((name, mismatch) -> FlansLog.log.warn("Content pack '{}': {}", name, mismatch));
        return message(mismatches == null ? Map.of() : mismatches);
    }

    /** No raw loader reason or item ids are included; even very large manifests produce a bounded message. */
    public static Component message(Map<String, EnumContentMismatch> mismatches)
    {
        var message = Component.translatable("disconnect.flansmodultimate.content_mismatch");
        if (!mismatches.isEmpty())
        {
            String names = new TreeMap<>(mismatches).keySet().stream().limit(MAX_VISIBLE_PACKS).map(ContentPackDiagnostics::displayName).collect(Collectors.joining(", "));
            if (mismatches.size() > MAX_VISIBLE_PACKS)
                names += ", …";
            message.append("\n\n").append(Component.translatable("disconnect.flansmodultimate.pack_summary", mismatches.size(), names));
        }
        return message;
    }

    private static String displayName(String name)
    {
        String clean = name.replaceAll("[\\p{Cntrl}§]", " ").strip();
        int length = clean.codePointCount(0, clean.length());
        return length <= MAX_VISIBLE_NAME ? clean : clean.substring(0, clean.offsetByCodePoints(0, MAX_VISIBLE_NAME - 1)) + "…";
    }
}
