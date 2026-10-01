package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GunTypeSideViewLimitTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));

    @Test
    void deployableSideViewLimitNeverExceedsFortyDegrees()
    {
        assertEquals(40F, read().getSideViewLimit());
        assertEquals(30F, read("SideViewLimit 30").getSideViewLimit());
        assertEquals(40F, read("SideViewLimit 45").getSideViewLimit());
    }

    private static GunType read(String... settings)
    {
        GunType type = new GunType();
        List<String> lines = new java.util.ArrayList<>();
        lines.add("ShortName testGun");
        lines.addAll(List.of(settings));
        type.read(new TypeFile("testGun", EnumType.GUN, PACK, lines));
        return type;
    }
}
