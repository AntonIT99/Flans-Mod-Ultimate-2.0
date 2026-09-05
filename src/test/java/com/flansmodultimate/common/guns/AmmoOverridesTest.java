package com.flansmodultimate.common.guns;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The reader has to survive anything a content pack can contain: a malformed optional
 * line must warn and be dropped, never abort loading and never become an arbitrary
 * ballistic number.
 */
class AmmoOverridesTest
{
    @Test
    void aDefinitionWithNoOverrideKeysProducesTheEmptyTable()
    {
        AmmoOverrides.Result result = read("Ammo 44_75APShell", "ShootDelay 5");
        assertTrue(result.overrides().isEmpty());
        assertFalse(result.overrides().isEmpty() && !result.warnings().isEmpty());
        assertTrue(result.warnings().isEmpty());
        assertNull(result.overrides().get("44_75APShell"));
    }

    @Test
    void readsEveryScalarKeyAndConvertsVelocityToBlocksPerTick()
    {
        AmmoOverrides overrides = read(
            "AmmoMass 44_75APShell 6800",
            "AmmoMuzzleVelocity 44_75APShell 770",
            "AmmoExplosiveMass 44_75APShell 0.029",
            "AmmoPenetrationAt100m 44_75APShell 143").overrides();

        AmmoOverride override = overrides.get("44_75APShell");
        assertNotNull(override);
        assertEquals(6800F, override.massGrams());
        assertEquals(38.5F, override.bulletSpeedBlocksPerTick(), 1.0E-4F);
        assertEquals(0.029F, override.explosiveMassKg(), 1.0E-6F);
        assertEquals(143F, override.penetrationAt100mMm());
        assertFalse(override.hasRounds());
    }

    @Test
    void ammoShortNamesMatchCaseInsensitively()
    {
        AmmoOverrides overrides = read("AmmoMass 44_75APShell 6800").overrides();
        assertNotNull(overrides.get("44_75apshell"));
        assertNotNull(overrides.get("44_75APSHELL"));
        assertNull(overrides.get("44_88APShell"));
    }

    @Test
    void eachAmmunitionIsOverriddenIndividually()
    {
        // The whole point of the feature: one weapon restating one shared round must
        // say nothing about the other rounds it accepts.
        AmmoOverrides overrides = read(
            "AmmoMass 44_75APShell 6800",
            "AmmoPenetrationAt100m 44_88APShell 162").overrides();

        AmmoOverride seventyFive = overrides.get("44_75APShell");
        AmmoOverride eightyEight = overrides.get("44_88APShell");
        assertNotNull(seventyFive);
        assertNotNull(eightyEight);
        assertEquals(6800F, seventyFive.massGrams());
        assertNull(seventyFive.penetrationAt100mMm());
        assertNull(eightyEight.massGrams());
        assertEquals(162F, eightyEight.penetrationAt100mMm());
    }

    @Test
    void lastDeclarationOfTheSameKeyWins()
    {
        AmmoOverride override = read("AmmoMass shell 100", "AmmoMass shell 250")
            .overrides().get("shell");
        assertNotNull(override);
        assertEquals(250F, override.massGrams());
    }

    @Test
    void readsAReplacementBeltAndWalksItPeriodically()
    {
        AmmoOverride override = read(
            "AddRoundForAmmo belt AP 1 162 0 800 45",
            "AddRoundForAmmo belt HE 2 135 0.016 835 0").overrides().get("belt");

        assertNotNull(override);
        assertTrue(override.hasRounds());
        assertEquals(3, override.periodLength());
        assertEquals(162F, override.statsForShot(0).mass());
        assertEquals(135F, override.statsForShot(1).mass());
        assertEquals(135F, override.statsForShot(2).mass());
        assertEquals(162F, override.statsForShot(3).mass(), "the belt must repeat");
        assertEquals(40F, override.statsForShot(0).bulletSpeed(), 1.0E-4F);
        assertEquals(0.016F, override.statsForShot(1).explosiveMass(), 1.0E-6F);
        assertEquals(45F, override.statsForShot(0).penetrationAt100m());
    }

    @Test
    void aBeltRoundMayOmitItsTrailingOptionalFields()
    {
        AmmoOverride override = read("AddRoundForAmmo belt Ball 1 9").overrides().get("belt");
        assertNotNull(override);
        assertEquals(9F, override.statsForShot(0).mass());
        assertEquals(0F, override.statsForShot(0).explosiveMass());
        assertEquals(0F, override.statsForShot(0).bulletSpeed());
        assertEquals(0F, override.statsForShot(0).penetrationAt100m());
    }

    @Test
    void malformedLinesWarnAndAreDropped()
    {
        AmmoOverrides.Result result = read(
            "AmmoMass 44_75APShell",
            "AmmoMass 44_88APShell notanumber",
            "AmmoPenetrationAt100m 44_90APShell -5",
            "AddRoundForAmmo belt AP",
            "AddRoundForAmmo belt AP zero 162",
            "AddRoundForAmmo belt AP 0 162");

        assertTrue(result.overrides().isEmpty(), "nothing valid was declared");
        assertEquals(6, result.warnings().size());
    }

    @Test
    void aNullFileIsToleratedRatherThanThrowing()
    {
        AmmoOverrides.Result result = AmmoOverrides.read(null);
        assertNotNull(result);
        assertTrue(result.overrides().isEmpty());
    }

    private static AmmoOverrides.Result read(String... lines)
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "ammo"));
        return AmmoOverrides.read(new TypeFile("syntheticWeapon", EnumType.GUN, pack, List.of(lines)));
    }
}
