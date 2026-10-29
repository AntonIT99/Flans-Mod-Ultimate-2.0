package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.mixin.MixinTargetCheck;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Merges from the Forge branch bring mixins written against its newer Custom NPCs build; this checks each of them, the
 * client ones included, against the installed NeoForge build without starting a game.
 */
class NpcMixinTargetsTest
{
    @Test
    void everyMixinFindsItsTargetsInTheInstalledCustomNpcsAndMinecraft() throws IOException
    {
        MixinTargetCheck.Result result = MixinTargetCheck.check("wolffsmodnpcs.mixins.json");
        assertEquals(List.of(), result.problems());
        // Guards against a check that silently looks at nothing.
        assertTrue(result.checked() > 200, "checked " + result.checked());
    }
}
