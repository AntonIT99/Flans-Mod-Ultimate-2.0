package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BulletTypeSpecialWarheadsTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "warheads"));

    @Test
    void legacyRoundsKeepTheirBallisticsUntilTheSpecialTraitIsSelected()
    {
        BulletType type = bullet();
        assertFalse(type.isDepthCharge());
        assertFalse(type.isHesh());
        assertFalse(type.isHeat());
        assertEquals(20F, type.getActivationDepth());
        assertEquals(2400, type.getDepthChargeLifetime());
        assertEquals(Integer.MIN_VALUE, type.getDepthChargeSeaLevel());
    }

    @Test
    void depthChargeAcceptsLegacyKeysAndAnOptionalSeaLevelOverride()
    {
        BulletType type = bullet("depthCharge true", "activationDepth 12.5", "DepthChargeSeaLevel -32");
        assertTrue(type.isDepthCharge());
        assertEquals(12.5F, type.getActivationDepth());
        assertEquals(-32, type.getDepthChargeSeaLevel());
        assertTrue(bullet("DEPTHCHARGE True").isDepthCharge());
        assertFalse(bullet("depthCharge false").isDepthCharge());
    }

    @Test
    void invalidDepthCannotCreateAnUnboundedOrNonfiniteActivationRule()
    {
        assertEquals(20F, bullet("activationDepth NaN").getActivationDepth());
        assertEquals(20F, bullet("activationDepth Infinity").getActivationDepth());
        assertEquals(0F, bullet("activationDepth -1").getActivationDepth());
        assertEquals(4096F, bullet("activationDepth 100000").getActivationDepth());
    }

    @Test
    void depthChargeLifetimeIsConfigurableAndBounded()
    {
        assertEquals(1800, bullet("DepthChargeLifetime 1800").getDepthChargeLifetime());
        assertEquals(1, bullet("DepthChargeLifetime -1").getDepthChargeLifetime());
        assertEquals(72000, bullet("DepthChargeLifetime 100000").getDepthChargeLifetime());
    }

    @Test
    void heshIsRecognisedWithoutSelectingTheHeatJetOrProtectionChannel()
    {
        BulletType type = bullet("HESH true", "PenetrationAt100m 100");
        assertTrue(type.isHesh());
        assertFalse(type.isHeat());
        assertEquals(100F, type.getPenetrationAt100m(0));
        assertTrue(bullet("HESH True").isHesh());
        assertFalse(bullet("HESH false").isHesh());
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("warhead", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
