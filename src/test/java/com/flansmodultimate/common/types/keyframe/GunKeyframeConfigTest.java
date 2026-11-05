package com.flansmodultimate.common.types.keyframe;

import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GunKeyframeConfigTest
{
    private static final String ANIMATION_FILE = """
        {"format_version": "1.8.0", "animations": {
            "kar_reload": {"animation_length": 4, "bones": {"gun": {"rotation": {"1.0": [5, 0, 0]}}}},
            "kar_shoot": {"animation_length": 1, "bones": {}},
            "kar_draw": {"animation_length": 1, "bones": {}}
        }}
        """;

    private static GunKeyframeConfig read(IContentProvider pack, String... lines)
    {
        TypeFile file = new TypeFile("testGun", EnumType.GUN, pack, List.of(lines));
        GunKeyframeConfig config = new GunKeyframeConfig();
        config.read(file);
        config.loadAnimations(pack, file);
        return config;
    }

    @Test
    void readsTheTyrantsAndPlebeiansKeysAndLoadsNamedAnimations(@TempDir Path packRoot) throws IOException
    {
        Path animations = Files.createDirectories(packRoot.resolve("assets").resolve("flansmod").resolve("animations"));
        // Pack files keep their own casing; the lookup tolerates a mismatch
        Files.writeString(animations.resolve("Kar98k.animation.json"), ANIMATION_FILE);
        IContentProvider pack = new ContentPack("test", packRoot);

        GunKeyframeConfig config = read(pack, "UseGLTFAnimation true", "UseRightArmGLTFAnimation true", "UseLoopReloadAnimation true", "GLTFAnimation kar_draw kar98k.animation",
            "GLTFAnimation kar_shoot kar98k.animation", "GLTFAnimation kar_reload kar98k.animation", "GLTFAnimation kar_missing kar98k.animation", "SwitchAnimation kar_draw",
            "ShootAnimation kar_shoot", "TacticalReloadGLTFAnimation none");

        assertTrue(config.isEnabled());
        assertTrue(config.isRightArmAnimated());
        assertFalse(config.isLeftArmAnimated());
        assertTrue(config.isLoopReloadAnimation());
        // The last GLTFAnimation line is the default reload animation
        assertEquals("kar_missing", config.getReloadAnimation());
        assertEquals("kar_draw", config.getSwitchAnimation());
        assertNull(config.getTacticalReloadAnimation());
        assertEquals(4F, config.getAnimation("kar_reload").length());
        assertTrue(config.hasAnimation("kar_shoot"));
        assertFalse(config.hasAnimation("kar_missing"));
        assertFalse(config.hasAnimation(null));
    }

    @Test
    void staysDisabledAndEmptyWithoutKeyframeSettings(@TempDir Path packRoot)
    {
        GunKeyframeConfig config = read(new ContentPack("test", packRoot), "ShortName testGun");

        assertFalse(config.isEnabled());
        assertNull(config.getReloadAnimation());
        assertTrue(config.getAnimations().isEmpty());
    }

    @Test
    void skipsMissingAnimationFiles(@TempDir Path packRoot)
    {
        GunKeyframeConfig config = read(new ContentPack("test", packRoot), "UseGLTFAnimation true", "GLTFAnimation kar_reload absent.animation");

        assertEquals("kar_reload", config.getReloadAnimation());
        assertFalse(config.hasAnimation("kar_reload"));
    }
}
