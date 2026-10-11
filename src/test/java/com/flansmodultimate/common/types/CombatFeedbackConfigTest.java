package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CombatFeedbackConfigTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "feedback"));

    @Test
    void oldDefinitionsKeepGenericFlybyAndEnableExplosionShake()
    {
        BulletType type = bullet();
        assertNull(type.getFlyBySound());
        assertEquals(5F, type.getFlyBySoundRange());
        assertEquals(10F, type.getPowerShake());
        assertEquals(32F, type.getRangeShake());
    }

    @Test
    void explicitNoneAndBlankLegacyAliasMuteTheSound()
    {
        assertEquals("", bullet("FlyBySound None").getFlyBySound());
        assertEquals("", bullet("missNoise").getFlyBySound());
        assertEquals(0F, bullet("Suppression 0").getSuppression());
        assertEquals(0F, bullet("powerShake 0").getPowerShake());
        assertEquals(0F, bullet("rangeShake 0").getRangeShake());
        assertEquals(0F, bullet("FlyBySoundRange 0").getFlyBySoundRange());
    }

    @Test
    void modernSoundAliasCanOverrideAnExplicitlyMutedLegacyAlias()
    {
        assertEquals("", bullet("missNoise None", "FlyBySound").getFlyBySound());
        assertEquals(7F, bullet("flybysoundrange 7").getFlyBySoundRange());
    }

    @Test
    void sharedShakeKeysAlsoApplyToGrenadesAndInvalidValuesAreInert()
    {
        GrenadeType type = new GrenadeType();
        type.load(new TypeFile("grenade", EnumType.GRENADE, PACK, List.of("PowerShake 30", "rangeShake 60")));
        assertEquals(30F, type.getPowerShake());
        assertEquals(60F, type.getRangeShake());
        BulletType invalid = bullet("powerShake NaN", "rangeShake Infinity", "Suppression NaN", "SuppressionRange -1");
        assertEquals(0F, invalid.getPowerShake());
        assertEquals(0F, invalid.getRangeShake());
        assertEquals(0F, invalid.getSuppression());
        assertEquals(0F, invalid.getSuppressionRange());
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("bullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
