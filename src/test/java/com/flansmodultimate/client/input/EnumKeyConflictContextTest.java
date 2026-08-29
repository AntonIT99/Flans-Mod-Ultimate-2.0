package com.flansmodultimate.client.input;

import net.minecraftforge.client.settings.KeyConflictContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumKeyConflictContextTest
{
    /**
     * Forge reads "does not conflict with IN_GAME" as "this is a modifier
     * context", and KeyModifier.NONE then reports itself inactive while Shift,
     * Ctrl or Alt is held. That silently switches off every binding in the
     * context, so this has to stay true.
     */
    @Test
    void everyContextDeclaresItselfInGame()
    {
        for (EnumKeyConflictContext context : EnumKeyConflictContext.values())
        {
            assertTrue(context.conflicts(KeyConflictContext.IN_GAME), context.name());
            assertTrue(context.conflicts(context), context.name());
        }
    }

    @Test
    void aPlaneBindAndAGroundBindCanShareAKey()
    {
        assertFalse(EnumKeyConflictContext.PLANE.conflicts(EnumKeyConflictContext.GROUND_DRIVEABLE));
        assertFalse(EnumKeyConflictContext.GROUND_DRIVEABLE.conflicts(EnumKeyConflictContext.PLANE));
    }

    @Test
    void bindsSharedByEveryDriveableClashWithBothSubsets()
    {
        assertTrue(EnumKeyConflictContext.DRIVEABLE.conflicts(EnumKeyConflictContext.PLANE));
        assertTrue(EnumKeyConflictContext.DRIVEABLE.conflicts(EnumKeyConflictContext.GROUND_DRIVEABLE));
        assertTrue(EnumKeyConflictContext.PLANE.conflicts(EnumKeyConflictContext.DRIVEABLE));
        assertTrue(EnumKeyConflictContext.GROUND_DRIVEABLE.conflicts(EnumKeyConflictContext.DRIVEABLE));
    }
}
