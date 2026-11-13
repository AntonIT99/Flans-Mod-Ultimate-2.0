package com.flansmodultimate.common.eras;

import com.flansmodultimate.api.eras.FlansEras;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EraApiSupportTest
{
    @Test
    void addonRestrictionsAreCanonicalAndMayNotDespawn()
    {
        assertEquals("", FlansEras.validateRestriction(" "));
        assertEquals("between ww2 1946", FlansEras.validateRestriction("between WW2 1946 match=service"));
        assertThrows(IllegalArgumentException.class, () -> FlansEras.validateRestriction("equal WW2 despawn=on"));
        assertThrows(IllegalArgumentException.class, () -> FlansEras.validateRestriction("equal Atlantis"));
        assertThrows(IllegalArgumentException.class, () -> FlansEras.validateRestriction("sometimes"));
    }

    @Test
    void parsedAddonRestrictionsAreReusedAndUnreadableOnesIgnored()
    {
        assertSame(EraApiSupport.parse("equal WW2"), EraApiSupport.parse("equal WW2"));
        assertNull(EraApiSupport.parse("sometimes"));
    }
}
