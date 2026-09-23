package com.flansmodultimate.common.sync;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.TypeFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.apache.commons.io.FilenameUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A fingerprint per content pack of the definitions it loaded.
 *
 * <p>This exists to tell a player that their packs are not the server's, which is the usual cause
 * of a client that displays the wrong numbers or plays the wrong animation for a weapon the server
 * resolves differently. It is a diagnostic and nothing more: the mod is server-authoritative, so a
 * mismatch costs accuracy of information rather than fairness, and a fingerprint a client sends can
 * always be whatever that client wants it to be. Nothing here decides who may play.
 *
 * <p>Fingerprints cover definition text, not pack assets, and they are taken per pack so the
 * warning can name which pack to go and fix. Blank lines, comments and indentation are normalised
 * away, because a pack that differs only in its whitespace loads identically on both sides and
 * reporting it would teach players to ignore the warning.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentFingerprint
{
    private static final String ALGORITHM = "SHA-256";

    /** Digests still being fed, keyed by pack name. Sorted so the order is the same on both sides. */
    private static final Map<String, MessageDigest> digests = new TreeMap<>();
    /** Finished fingerprints, computed once the last definition has been recorded. */
    private static Map<String, String> fingerprints;

    /**
     * Folds one definition file into its pack's fingerprint.
     *
     * <p>Call this as the file is registered, while the content pipeline still holds the file's
     * text: loading order is deterministic, so feeding the digest in registration order gives both
     * sides the same result without having to sort anything again here.
     */
    public static synchronized void record(TypeFile typeFile)
    {
        MessageDigest digest = digests.computeIfAbsent(packName(typeFile), ContentFingerprint::createDigest);
        if (digest == null)
            return;

        fingerprints = null;
        digest.update(normalize(typeFile).getBytes(StandardCharsets.UTF_8));
    }

    /** The fingerprint of every pack that loaded at least one definition, keyed by pack name. */
    public static synchronized Map<String, String> get()
    {
        if (fingerprints == null)
        {
            Map<String, String> computed = new TreeMap<>();
            digests.forEach((pack, digest) -> computed.put(pack, finalizeDigest(digest)));
            fingerprints = Collections.unmodifiableMap(computed);
        }
        return fingerprints;
    }

    /**
     * Which packs differ from the fingerprints given, and how.
     *
     * @param theirs the other side's fingerprints
     * @return pack name to the way it differs, empty when the two sides loaded the same definitions
     */
    public static Map<String, EnumContentMismatch> compareWith(Map<String, String> theirs)
    {
        return compare(get(), theirs);
    }

    /** Pure form of {@link #compareWith(Map)}, for both sides being supplied rather than loaded. */
    public static Map<String, EnumContentMismatch> compare(Map<String, String> ours, Map<String, String> theirs)
    {
        Map<String, EnumContentMismatch> mismatches = new TreeMap<>();

        theirs.forEach((pack, theirFingerprint) -> {
            String ourFingerprint = ours.get(pack);
            if (ourFingerprint == null)
                mismatches.put(pack, EnumContentMismatch.MISSING);
            else if (!ourFingerprint.equals(theirFingerprint))
                mismatches.put(pack, EnumContentMismatch.DIFFERENT);
        });

        ours.keySet().stream()
            .filter(pack -> !theirs.containsKey(pack))
            .forEach(pack -> mismatches.put(pack, EnumContentMismatch.UNKNOWN_TO_THEM));

        return mismatches;
    }

    /**
     * The pack a definition came from, without the archive extension: a pack loaded as a folder on
     * one side and as a zip on the other is the same pack and should not be reported as two.
     */
    private static String packName(TypeFile typeFile)
    {
        String name = typeFile.getContentPack().getName();
        return FilenameUtils.getBaseName(name).isBlank() ? name : FilenameUtils.getBaseName(name);
    }

    private static String normalize(TypeFile typeFile)
    {
        StringBuilder out = new StringBuilder(typeFile.getName()).append('\n');

        for (String line : typeFile.getLines())
        {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//"))
                continue;
            out.append(trimmed).append('\n');
        }

        return out.toString();
    }

    private static MessageDigest createDigest(String pack)
    {
        try
        {
            return MessageDigest.getInstance(ALGORITHM);
        }
        catch (NoSuchAlgorithmException e)
        {
            // Every JVM is required to provide SHA-256, so this is unreachable in practice, and a
            // fingerprint is worth nothing to crash a load over.
            FlansMod.log.error("Cannot fingerprint content pack '{}': {} is unavailable", pack, ALGORITHM);
            return null;
        }
    }

    /**
     * Reads a digest without spending it, so that a pack still being recorded is not left
     * fingerprinting from an empty state. A digest that cannot be copied is read the only other
     * way there is, which is correct as long as nothing is recorded afterwards; packs are read once
     * at startup and the fingerprints are not asked for until a player joins.
     */
    private static String finalizeDigest(MessageDigest digest)
    {
        try
        {
            return toHex(((MessageDigest) digest.clone()).digest());
        }
        catch (CloneNotSupportedException e)
        {
            return toHex(digest.digest());
        }
    }

    private static String toHex(byte[] bytes)
    {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        return hex.toString();
    }
}
