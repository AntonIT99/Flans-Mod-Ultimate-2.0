package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The {@code HEAT} key is shared with the Labjac edition, so its packs need no rewriting to be recognised. */
class BulletTypeHeatTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "heat"));

    @Test
    void roundsAreKineticUnlessMarkedHeat()
    {
        assertFalse(bullet("PenetrationAt100m 143").isHeat());
        assertTrue(bullet("HEAT true", "PenetrationAt100m 200").isHeat());
        assertFalse(bullet("HEAT false").isHeat());
    }

    @Test
    void theLabjacSpellingsAreAccepted()
    {
        assertTrue(bullet("HEAT True").isHeat());
        assertTrue(bullet("heat true").isHeat());
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("syntheticBullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
