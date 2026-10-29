package com.flansmodultimate.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the main mod's mixins, the client ones included, against the installed Minecraft without starting a game. */
class MixinTargetsTest
{
    @Test
    void everyMixinFindsItsTargetsInTheInstalledMinecraft() throws IOException
    {
        MixinTargetCheck.Result result = MixinTargetCheck.check("flansmodultimate.mixins.json");
        assertEquals(List.of(), result.problems());
        // Guards against a check that silently looks at nothing.
        assertTrue(result.checked() > 50, "checked " + result.checked());
    }
}
