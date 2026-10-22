package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TheatresTest
{
    @Test
    void biomesAndTagsAreKeptWithTheirNamespace()
    {
        assertEquals(Optional.of("minecraft:desert"), Theatres.normalize("desert"));
        assertEquals(Optional.of("#minecraft:is_badlands"), Theatres.normalize("#is_badlands"));
        assertEquals(Optional.of("#forge:is_desert"), Theatres.normalize("#forge:is_desert"));
    }

    @Test
    void textThatIsNoIdIsRefused()
    {
        assertEquals(Optional.empty(), Theatres.normalize("Sandy Desert"));
        assertEquals(Optional.empty(), Theatres.normalize("#is badlands"));
    }
}
