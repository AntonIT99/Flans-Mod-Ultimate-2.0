package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GunTypeFirstPersonFrameTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));

    @ParameterizedTest
    @ValueSource(strings = {"Tap", "tap", "FLANS_TAP", "labjac", "1.7.10"})
    void legacyAuthoredFramesNormalizeWithoutChangingAimCorrection(String value)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testGun", EnumType.GUN, PACK, List.of("ShortName testGun", "FirstPersonFrame " + value)));

        assertEquals(EnumGunFirstPersonFrame.FLANS_1710, type.getFirstPersonFrame());
        for (EnumGunFirstPersonFrame view : EnumGunFirstPersonFrame.values())
            assertEquals(view.aimZCorrectionFrom(EnumGunFirstPersonFrame.FLANS_TAP), view.aimZCorrectionFrom(type.getFirstPersonFrame()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.12.2", "FLANS_1122", "1122"})
    void newerAuthoredFrameRetainsItsOwnAlignment(String value)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testGun", EnumType.GUN, PACK, List.of("ShortName testGun", "FirstPersonFrame " + value)));

        assertEquals(EnumGunFirstPersonFrame.FLANS_1122, type.getFirstPersonFrame());
    }
}
