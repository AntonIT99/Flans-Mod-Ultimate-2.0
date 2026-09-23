package com.flansmodultimate.common;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Short particle names are a convenience for commands and content packs, so they must never
 * change the meaning of a name that already exists on its own.
 */
class FlanParticlesResolveTest
{
    @Test
    void fullNamesResolveToThemselves()
    {
        assertEquals(Optional.of(FlanParticles.FM_FLARE), FlanParticles.resolve("flansmod.flare"));
        assertEquals(Optional.of(FlanParticles.SMOKE), FlanParticles.resolve("smoke"));
    }

    @Test
    void shortNamesResolveToThisModsParticle()
    {
        assertEquals(Optional.of(FlanParticles.FM_FLARE), FlanParticles.resolve("flare"));
        assertEquals(Optional.of(FlanParticles.FM_MUZZLE_FLASH), FlanParticles.resolve("muzzleflash"));
    }

    @Test
    void existingNamesWinOverShortForms()
    {
        // "flame" is the vanilla particle; the mod's own one is "flansmod.fmflame"
        assertEquals(Optional.of(FlanParticles.FLAME), FlanParticles.resolve("flame"));
        assertEquals(Optional.of(FlanParticles.FM_FLAME), FlanParticles.resolve("fmflame"));
    }

    @Test
    void namesAreCaseAndSpaceInsensitive()
    {
        assertEquals(Optional.of(FlanParticles.FM_FLARE), FlanParticles.resolve("  FlansMod.Flare "));
    }

    @Test
    void blockAndItemVariantsKeepTheirResourceId()
    {
        assertEquals(Optional.of("blockcrack_minecraft:stone"), FlanParticles.resolve("blockcrack_minecraft:stone"));
        assertEquals(Optional.of(FlanParticles.BLOCK_CRACK), FlanParticles.resolve("blockcrack"));
    }

    @Test
    void unknownNamesResolveToNothing()
    {
        assertTrue(FlanParticles.resolve("not_a_particle").isEmpty());
        assertTrue(FlanParticles.resolve("flansmod.not_a_particle").isEmpty());
        assertTrue(FlanParticles.resolve("blockcrack_").isEmpty());
        assertTrue(FlanParticles.resolve("").isEmpty());
        assertTrue(FlanParticles.resolve(null).isEmpty());
    }
}
