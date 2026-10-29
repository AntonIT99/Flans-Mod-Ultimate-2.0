package com.wolffsmod.npcs.properties;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which stored sound names the module plays the Flan way, and which stay with Custom NPCs. */
class NpcSoundsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void contentPackSoundNameIsPlayedTheFlanWay()
    {
        assertTrue(NpcSounds.isFlanSound("mechaidle"), "a content pack sound is only ever stored unqualified");
        assertTrue(NpcSounds.isFlanSound("flansmod:mechaidle"));
    }

    @Test
    void vanillaSoundStaysWithCustomNpcs()
    {
        assertFalse(NpcSounds.isFlanSound("entity.zombie.ambient"), "Custom NPCs reads an unqualified name as a vanilla sound");
        assertFalse(NpcSounds.isFlanSound("minecraft:entity.zombie.ambient"));
    }

    @Test
    void otherNamespaceStaysWithCustomNpcs()
    {
        assertFalse(NpcSounds.isFlanSound("customnpcs:somesound"));
    }

    @Test
    void noSoundIsNoFlanSound()
    {
        assertFalse(NpcSounds.isFlanSound(null));
        assertFalse(NpcSounds.isFlanSound(" "));
        assertFalse(NpcSounds.isFlanSound("Not A Sound"));
    }
}
