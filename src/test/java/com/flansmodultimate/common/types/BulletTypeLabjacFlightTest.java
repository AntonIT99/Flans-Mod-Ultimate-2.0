package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Parsing of the long-range, missile flight and chunk-loading keys of the Labjac fork. */
class BulletTypeLabjacFlightTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "labjac-flight"));

    @Test
    void defaultsLeaveOrdinaryAmmunitionUnchanged()
    {
        BulletType type = bullet();
        assertFalse(type.hasLongRangeDamage());
        assertEquals(1F, type.getDynamicDamage());
        assertFalse(type.isProjectileChunkLoading());
        assertTrue(type.isMissileTwirl());
        assertEquals(28F, type.getMissileTwirlSpeed());
        assertEquals(4F, type.getMissileTwirlWobble());
        assertEquals(0.12F, type.getMissileGroundSkimTolerance());
    }

    @Test
    void longRangeDamageReadsBothSpellings()
    {
        BulletType type = bullet("dynamicBulletDelay 12", "DynamicDamage 0.4");
        assertTrue(type.hasLongRangeDamage());
        assertEquals(12, type.getDynamicBulletDelay());
        assertEquals(0.4F, type.getDynamicDamage());
    }

    @Test
    void forkPlaceholderDelayMeansUnused()
    {
        assertFalse(bullet("DynamicBulletDelay 9000", "DynamicDamage 0.5").hasLongRangeDamage());
        assertTrue(bullet("DynamicBulletDelay 8998").hasLongRangeDamage());
        assertEquals(0F, bullet("DynamicDamage -3").getDynamicDamage());
        assertEquals(1F, bullet("DynamicDamage NaN").getDynamicDamage());
    }

    @Test
    void missileKeysAcceptTheAtgmAliasesAndTheMissileSpellingWins()
    {
        BulletType atgm = bullet("ATGMTwirl false", "ATGMTwirlSpeed 10", "ATGMTwirlWobble 1", "ATGMGroundSkimTolerance 0.3");
        assertFalse(atgm.isMissileTwirl());
        assertEquals(10F, atgm.getMissileTwirlSpeed());
        assertEquals(1F, atgm.getMissileTwirlWobble());
        assertEquals(0.3F, atgm.getMissileGroundSkimTolerance());

        BulletType both = bullet("ATGMTwirlSpeed 10", "MissileTwirlSpeed 40", "ATGMTwirl false", "MissileTwirl true");
        assertEquals(40F, both.getMissileTwirlSpeed());
        assertTrue(both.isMissileTwirl());
    }

    @Test
    void groundSkimToleranceIsBounded()
    {
        assertEquals(0F, bullet("MissileGroundSkimTolerance -1").getMissileGroundSkimTolerance());
        assertEquals(4F, bullet("MissileGroundSkimTolerance 100").getMissileGroundSkimTolerance());
        assertEquals(0.12F, bullet("MissileGroundSkimTolerance NaN").getMissileGroundSkimTolerance());
    }

    @Test
    void chunkLoadingIsOptIn()
    {
        assertTrue(bullet("ProjectileChunkLoading true").isProjectileChunkLoading());
        assertFalse(bullet("ProjectileChunkLoading false").isProjectileChunkLoading());
    }

    static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("labjac", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
