package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SkirmishTest
{
    private static final int GERMANY = 4;
    private static final int BRITAIN = 7;

    @BeforeAll
    static void bootstrapRegistries()
    {
        // A skirmish keeps the key of its dimension, which needs the vanilla registries.
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

    private static Skirmish elAlamein()
    {
        Skirmish skirmish = new Skirmish(UUID.randomUUID(), "El Alamein", Level.OVERWORLD);
        skirmish.getSides().add(new Skirmish.Side(GERMANY, new Vec3(0D, 64D, 0D), 24D, new SkirmishComposer.Request(16, Map.of("panzergrenadiere", 2), Map.of(BotRole.ARMOR, 3), Map.of("flak88", 1)),
            List.of("afrikakorps", "luftwaffe")));
        skirmish.getSides().add(new Skirmish.Side(BRITAIN, new Vec3(200D, 64D, 0D), 32D, SkirmishComposer.Request.AS_LISTED, List.of("eighth_army")));
        skirmish.setIntent(Skirmish.Intent.PUSH);
        skirmish.setEnd(Skirmish.End.TIME);
        skirmish.setTimeLimitMinutes(20);
        skirmish.setRespawn(true);
        skirmish.setTickets(150);
        skirmish.setDifficulty(BotSettings.Difficulty.VETERAN);
        skirmish.setProtection(false);
        skirmish.setFront(new Vec3(100D, 66D, 0D));
        return skirmish;
    }

    @Test
    void sidesAndTheirFactionsAndTeamsAreFound()
    {
        Skirmish skirmish = elAlamein();
        assertEquals(1, skirmish.sideOf(BRITAIN));
        assertEquals(-1, skirmish.sideOf(99));
        assertEquals(0, skirmish.sideOfTeam("luftwaffe"));
        assertEquals(-1, skirmish.sideOfTeam("wehrmacht"));
    }

    @Test
    void aStoredSkirmishKeepsItsSidesRequestsAndSettings()
    {
        Skirmish before = elAlamein();
        Skirmish after = Skirmish.load(before.save()).orElseThrow();
        assertEquals(before.getDimension(), after.getDimension());
        assertEquals(before.getId(), after.getId());
        assertEquals("El Alamein", after.getName());
        assertEquals(before.getSides(), after.getSides());
        assertEquals(Skirmish.Intent.PUSH, after.getIntent());
        assertEquals(Skirmish.End.TIME, after.getEnd());
        assertEquals(20, after.getTimeLimitMinutes());
        assertTrue(after.isRespawn());
        assertEquals(150, after.getTickets());
        assertEquals(BotSettings.Difficulty.VETERAN, after.getDifficulty());
        assertFalse(after.isProtection());
        assertEquals(Skirmish.State.DRAFT, after.getState());
        assertEquals(new Vec3(100D, 66D, 0D), after.getFront());
    }

    @Test
    void aPushMovesTheFrontTowardTheWeakerSide()
    {
        assertEquals(0.5D, Skirmishes.pushShare(10, 10));
        assertEquals(0.5D, Skirmishes.pushShare(0, 0));
        assertTrue(Skirmishes.pushShare(30, 10) > 0.5D, "the first side, stronger, pushes the front toward the second");
        assertTrue(Skirmishes.pushShare(0, 12) < 0.5D);
        assertTrue(Skirmishes.pushShare(100, 0) < 1D, "the front never reaches the weaker side's zone");
    }
}
