package com.flansmodultimate.common.types.keyframe;

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeyframeAnimationReaderTest
{
    private static final String RELOAD = """
        {
            "format_version": "1.8.0",
            "animations": {
                "rifle_reload": {
                    "animation_length": 2,
                    "bones": {
                        "gun": {
                            "rotation": {"0.0": [0, 0, 0], "1.0": [10, -20, 30]},
                            "position": {"0.5": {"pre": [1, 1, 1], "post": [2, 4, 6]}}
                        },
                        "left_arm": {"position": [3, 2, 1]},
                        "ammo": {"rotation": {"0.0": ["query.anim_time", 0, 0]}}
                    }
                },
                "rifle_shoot": {"animation_length": 0.5, "bones": {}}
            }
        }
        """;

    private static Map<String, KeyframeAnimation> read(String json)
    {
        return KeyframeAnimationReader.read(new StringReader(json));
    }

    private static float[] sample(KeyframeChannel channel, float time, float length)
    {
        float[] out = new float[3];
        channel.sample(time, length, out);
        return out;
    }

    @Test
    void readsEveryAnimationWithItsLengthAndBones()
    {
        Map<String, KeyframeAnimation> animations = read(RELOAD);

        assertEquals(2, animations.size());
        KeyframeAnimation reload = animations.get("rifle_reload");
        assertEquals(2F, reload.length());
        assertNotNull(reload.bone(EnumKeyframeBone.GUN));
        assertNull(reload.bone(EnumKeyframeBone.RIGHT_ARM));
        assertTrue(animations.get("rifle_shoot").bones().isEmpty());
    }

    @Test
    void interpolatesBetweenKeyframesAndEasesToZeroAtTheEnd()
    {
        KeyframeChannel rotation = read(RELOAD).get("rifle_reload").bone(EnumKeyframeBone.GUN).rotation();

        assertArrayEquals(new float[] {5F, -10F, 15F}, sample(rotation, 0.5F, 2F), 1.0E-5F);
        assertArrayEquals(new float[] {10F, -20F, 30F}, sample(rotation, 1F, 2F), 1.0E-5F);
        // After the last keyframe the value returns to zero at the animation's end, as in Tyrants and Plebeians
        assertArrayEquals(new float[] {5F, -10F, 15F}, sample(rotation, 1.5F, 2F), 1.0E-5F);
        assertArrayEquals(new float[] {0F, 0F, 0F}, sample(rotation, 2F, 2F), 1.0E-5F);
    }

    @Test
    void startsFromZeroBeforeTheFirstKeyframeAndUsesPostValues()
    {
        KeyframeChannel position = read(RELOAD).get("rifle_reload").bone(EnumKeyframeBone.GUN).position();

        assertEquals(1, position.size());
        assertArrayEquals(new float[] {1F, 2F, 3F}, sample(position, 0.25F, 2F), 1.0E-5F);
        assertArrayEquals(new float[] {2F, 4F, 6F}, sample(position, 0.5F, 2F), 1.0E-5F);
    }

    @Test
    void readsStaticVectorsAndSkipsMolangExpressions()
    {
        KeyframeAnimation reload = read(RELOAD).get("rifle_reload");

        assertArrayEquals(new float[] {3F, 2F, 1F}, sample(reload.bone(EnumKeyframeBone.LEFT_ARM).position(), 0F, 2F), 1.0E-5F);
        assertTrue(reload.bone(EnumKeyframeBone.LEFT_ARM).rotation().isEmpty());
        assertTrue(reload.bone(EnumKeyframeBone.AMMO).rotation().isEmpty());
    }

    @Test
    void rejectsFilesWithoutAnimations()
    {
        assertThrows(JsonParseException.class, () -> read("{\"format_version\": \"1.8.0\"}"));
    }
}
