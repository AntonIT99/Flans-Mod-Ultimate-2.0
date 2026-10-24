package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Editing the rotation keeps it going on from the round last started: moving, removing and the rounds of a removed map. */
class TeamsRotationEditTest
{
    /** The server-wide manager before the test, which creating one replaces. */
    private Object previous;

    @BeforeAll
    static void bootstrapRegistries()
    {
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

    @BeforeEach
    void keepInstance() throws ReflectiveOperationException
    {
        previous = instanceField().get(null);
    }

    @AfterEach
    void restoreInstance() throws ReflectiveOperationException
    {
        instanceField().set(null, previous);
    }

    private static Field instanceField() throws NoSuchFieldException
    {
        Field field = TeamsManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        return field;
    }

    /** A manager with rounds of 10 to 13 minutes on two alternating maps, and round {@code lastStarted} the one last started. */
    private static TeamsManager rotation(int lastStarted) throws ReflectiveOperationException
    {
        TeamsManager manager = new TeamsManager();
        TeamsSavedData data = new TeamsSavedData();
        for (String map : List.of("north", "south"))
            data.maps.put(map, new TeamsMap(map, map, Level.OVERWORLD));
        for (int i = 0; i < 4; i++)
            data.rounds.add(new TeamsRound(i % 2 == 0 ? "north" : "south", "tdm", List.of("red", "blue"), 10 + i, 50));
        set(manager, "savedData", data);
        set(manager, "rotationIndex", lastStarted);
        return manager;
    }

    private static void set(TeamsManager manager, String name, Object value) throws ReflectiveOperationException
    {
        Field field = TeamsManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(manager, value);
    }

    /** The round that starts next, by the length it was given, which tells the rounds apart. */
    private static int nextMinutes(TeamsManager manager)
    {
        return manager.getRounds().get(manager.nextRoundIndex()).getTimeLimitMinutes();
    }

    @Test
    void removingARoundBeforeTheOneLastStartedStillPlaysTheOneAfterIt() throws ReflectiveOperationException
    {
        TeamsManager manager = rotation(2);
        assertTrue(manager.removeRound(0));
        assertEquals(13, nextMinutes(manager));
    }

    @Test
    void removingTheRoundLastStartedPlaysTheOneThatFollowedIt() throws ReflectiveOperationException
    {
        TeamsManager manager = rotation(1);
        assertTrue(manager.removeRound(1));
        assertEquals(12, nextMinutes(manager));
    }

    @Test
    void removingTheLastRoundOfTheRotationWrapsToTheFirst() throws ReflectiveOperationException
    {
        TeamsManager manager = rotation(3);
        assertTrue(manager.removeRound(3));
        assertEquals(10, nextMinutes(manager));
    }

    @Test
    void movingRoundsKeepsTheRoundThatPlaysNext() throws ReflectiveOperationException
    {
        TeamsManager manager = rotation(0);
        assertTrue(manager.moveRound(0, 3));
        assertEquals(11, nextMinutes(manager));
        assertEquals(10, manager.getRounds().get(3).getTimeLimitMinutes());
        assertFalse(manager.moveRound(0, 4));
    }

    @Test
    void removingAMapTakesItsRoundsAndPlaysTheNextRoundLeft() throws ReflectiveOperationException
    {
        TeamsManager manager = rotation(1);
        assertTrue(manager.removeMap("north"));
        assertEquals(2, manager.getRounds().size());
        assertEquals(13, nextMinutes(manager));
    }

    @Test
    void aRoundTakesNewLimits() throws ReflectiveOperationException
    {
        TeamsManager manager = rotation(0);
        assertTrue(manager.setRoundLimits(1, 25, 400));
        assertEquals(25, manager.getRounds().get(1).getTimeLimitMinutes());
        assertEquals(400, manager.getRounds().get(1).getScoreLimit());
        assertFalse(manager.setRoundLimits(4, 25, 400));
        assertFalse(manager.setRoundLimits(1, 0, 400));
    }
}
