package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunTypeMuzzleFlashTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));

    @Test
    void anUnauthoredGunDoesNotRequestAPassengerMuzzleFlash()
    {
        assertFalse(read("ShortName testGun").hasMuzzleFlashModel());
    }

    @Test
    void aGunFlashModelRequestsAPassengerMuzzleFlash()
    {
        assertTrue(read("ShortName testGun", "FlashModel DefaultFlash").hasMuzzleFlashModel());
    }

    @Test
    void aCustomMuzzleFlashModelRequestsAPassengerMuzzleFlash()
    {
        assertTrue(read("ShortName testGun", "MuzzleFlashModel CustomFlash").hasMuzzleFlashModel());
    }

    private static GunType read(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testGun", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }
}
