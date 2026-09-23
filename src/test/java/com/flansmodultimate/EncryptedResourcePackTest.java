package com.flansmodultimate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.resources.ResourceLocation;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptedResourcePackTest
{
    private static final String KEY_ID = "test_content_module";
    private static final String KEY_SALT = "Flans Mod Ultimate uncensored resources v1";
    private static final byte[] MAGIC = "FMUENC01".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path temporaryDirectory;

    @Test
    void decryptsAuthenticatedLanguageAndTextureResources() throws Exception
    {
        Map<String, byte[]> expected = new LinkedHashMap<>();
        expected.put("assets/flansmod/lang/en_us.json", "{\"item.flansmod.test\":\"Test\"}".getBytes(StandardCharsets.UTF_8));
        expected.put("assets/flansmod/textures/item/test.png", new byte[] { 1, 2, 3, 4 });
        Path bundle = writeBundle(expected);

        Map<ResourceLocation, byte[]> actual = EncryptedResourcePack.decryptBundle(bundle, KEY_ID);

        assertArrayEquals(expected.get("assets/flansmod/lang/en_us.json"),
            actual.get(ResourceLocation.fromNamespaceAndPath("flansmod", "lang/en_us.json")));
        assertArrayEquals(expected.get("assets/flansmod/textures/item/test.png"),
            actual.get(ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/test.png")));
    }

    @Test
    void rejectsModifiedCiphertext() throws Exception
    {
        Path bundle = writeBundle(Map.of(
            "assets/flansmod/lang/en_us.json", "{}".getBytes(StandardCharsets.UTF_8)));
        byte[] modified = Files.readAllBytes(bundle);
        modified[modified.length - 1] ^= 1;
        Files.write(bundle, modified);

        assertThrows(Exception.class, () -> EncryptedResourcePack.decryptBundle(bundle, KEY_ID));
    }

    private Path writeBundle(Map<String, byte[]> resources) throws Exception
    {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (DataOutputStream archive = new DataOutputStream(archiveBytes))
        {
            archive.writeInt(0x464D5552);
            archive.writeInt(1);
            archive.writeInt(resources.size());
            for (Map.Entry<String, byte[]> entry : resources.entrySet())
            {
                archive.writeUTF(entry.getKey());
                archive.writeInt(entry.getValue().length);
                archive.write(entry.getValue());
            }
        }
        byte[] plaintext = archiveBytes.toByteArray();

        MessageDigest keyDigest = MessageDigest.getInstance("SHA-256");
        keyDigest.update(KEY_SALT.getBytes(StandardCharsets.UTF_8));
        keyDigest.update((byte) 0);
        byte[] key = keyDigest.digest(KEY_ID.getBytes(StandardCharsets.UTF_8));

        MessageDigest ivDigest = MessageDigest.getInstance("SHA-256");
        ivDigest.update(plaintext);
        ivDigest.update((byte) 0);
        byte[] iv = Arrays.copyOf(ivDigest.digest(KEY_ID.getBytes(StandardCharsets.UTF_8)), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        ByteArrayOutputStream aad = new ByteArrayOutputStream();
        aad.write(MAGIC);
        aad.write(KEY_ID.getBytes(StandardCharsets.UTF_8));
        cipher.updateAAD(aad.toByteArray());
        byte[] encrypted = cipher.doFinal(plaintext);

        Path bundle = temporaryDirectory.resolve("test.fmu");
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(bundle)))
        {
            output.write(MAGIC);
            output.writeInt(1);
            output.writeByte(iv.length);
            output.write(iv);
            output.writeInt(encrypted.length);
            output.write(encrypted);
        }
        return bundle;
    }
}
