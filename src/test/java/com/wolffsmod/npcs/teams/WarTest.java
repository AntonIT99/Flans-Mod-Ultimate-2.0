package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WarTest
{
    private static final int GERMANY = 1;
    private static final int BRITAIN = 2;

    @BeforeAll
    static void bootstrapRegistries()
    {
        // A war keeps the keys of the dimensions it is fought in, which need the vanilla registries.
        SharedConstants.tryDetectVersion();
        try
        {
            Bootstrap.bootStrap();
        }
        catch (ExceptionInInitializerError error)
        {
            // Plain JUnit lacks the NetworkEvent transformer of Forge; vanilla registries are initialized first.
            Throwable cause = error;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (!(cause instanceof NoSuchMethodException) || !cause.getMessage().equals("net.minecraftforge.network.NetworkEvent.<init>()"))
                throw error;
        }
    }

    private static War northAfrica()
    {
        War war = new War(UUID.randomUUID(), "North Africa");
        war.getFactions().addAll(List.of(GERMANY, BRITAIN));
        war.getDimensions().add(Level.OVERWORLD);
        Wars.claim(war, Level.OVERWORLD, new ChunkPos(0, 0), 1, GERMANY);
        Wars.claim(war, Level.OVERWORLD, new ChunkPos(3, 0), 1, BRITAIN);
        war.getEngagements().add(new War.Engagement(UUID.randomUUID(), new WarFront.Border(ChunkPos.asLong(1, 0), GERMANY, ChunkPos.asLong(2, 0), BRITAIN)));
        war.setRunning(true);
        war.setFlipSeconds(45);
        war.setInfantry(20);
        return war;
    }

    @Test
    void claimsHandChunksOverAndCountWhatChanged()
    {
        War war = northAfrica();
        assertEquals(Map.of(GERMANY, 9, BRITAIN, 9), war.holdings());
        assertEquals(3, Wars.claim(war, Level.OVERWORLD, new ChunkPos(2, 0), 0, GERMANY) + Wars.claim(war, Level.OVERWORLD, new ChunkPos(2, 1), 0, GERMANY)
            + Wars.claim(war, Level.OVERWORLD, new ChunkPos(2, -1), 0, GERMANY), "each chunk Britain held changes hands");
        assertEquals(0, Wars.claim(war, Level.OVERWORLD, new ChunkPos(0, 0), 0, GERMANY), "a chunk already held changes nothing");
        assertEquals(9, Wars.claim(war, Level.OVERWORLD, new ChunkPos(0, 0), 1, null));
        assertEquals(Map.of(GERMANY, 3, BRITAIN, 6), war.holdings());
        assertTrue(war.covers(Level.OVERWORLD));
        assertFalse(war.covers(Level.NETHER));
    }

    @Test
    void aSideTakesAChunkAfterPressingOnLongEnough()
    {
        War.Engagement engagement = new War.Engagement(UUID.randomUUID(), new WarFront.Border(1L, GERMANY, 2L, BRITAIN));
        assertFalse(engagement.advance(0, 5, 15));
        assertFalse(engagement.advance(0, 5, 15));
        assertFalse(engagement.advance(-1, 5, 15), "a break starts over");
        assertFalse(engagement.advance(0, 5, 15));
        assertFalse(engagement.advance(1, 5, 15), "the other side pressing starts over");
        assertFalse(engagement.advance(1, 5, 15));
        assertTrue(engagement.advance(1, 5, 15));
        assertEquals(1, engagement.getPressing());
    }

    @Test
    void aStoredWarKeepsItsTerritoryEngagementsAndSettings()
    {
        War before = northAfrica();
        War after = War.load(before.save()).orElseThrow();
        assertEquals(before.getId(), after.getId());
        assertEquals(List.of(GERMANY, BRITAIN), after.getFactions());
        assertTrue(after.covers(Level.OVERWORLD));
        assertFalse(after.covers(Level.END));
        assertEquals(before.territory(Level.OVERWORLD), after.territory(Level.OVERWORLD));
        assertEquals(before.getEngagements().get(0).getBorder(), after.getEngagements().get(0).getBorder());
        assertEquals(before.getEngagements().get(0).getSkirmish(), after.getEngagements().get(0).getSkirmish());
        assertTrue(after.isRunning());
        assertEquals(45, after.getFlipSeconds());
        assertEquals(20, after.getInfantry());
    }
}
