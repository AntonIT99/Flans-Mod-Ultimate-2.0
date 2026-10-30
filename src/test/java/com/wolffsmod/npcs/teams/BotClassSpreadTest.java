package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class BotClassSpreadTest
{
    private static Map<String, Integer> field(List<String> classes, Map<String, Integer> weights, int soldiers, long seed)
    {
        RandomGenerator random = new Random(seed);
        Map<String, Integer> fielded = new HashMap<>();
        for (int i = 0; i < soldiers; i++)
            fielded.merge(BotPopulation.pickClass(classes, weights, fielded, random), 1, Integer::sum);
        return fielded;
    }

    @Test
    void everyClassIsFieldedOnceThereIsRoomForIt()
    {
        List<String> classes = List.of("rifleman", "medic", "engineer", "antitank", "support");
        for (long seed = 0; seed < 50; seed++)
        {
            Map<String, Integer> fielded = field(classes, Map.of(), 5, seed);
            classes.forEach(playerClass -> assertEquals(1, fielded.getOrDefault(playerClass, 0), playerClass));
        }
    }

    @Test
    void classesKeepToTheirWeights()
    {
        Map<String, Integer> fielded = field(List.of("rifleman", "medic", "sniper"), Map.of("rifleman", 3, "medic", 1, "sniper", 0), 8, 1L);
        assertEquals(6, fielded.get("rifleman"));
        assertEquals(2, fielded.get("medic"));
        assertFalse(fielded.containsKey("sniper"));
    }

    @Test
    void classesWithoutAnyWeightAllCount()
    {
        Map<String, Integer> fielded = field(List.of("rifleman", "medic"), Map.of("rifleman", 0), 4, 2L);
        assertEquals(2, fielded.get("rifleman"));
        assertEquals(2, fielded.get("medic"));
    }

    @Test
    void aTeamWithoutClassesGetsNone()
    {
        assertEquals("", BotPopulation.pickClass(List.of(), Map.of(), Map.of(), new Random(0L)));
    }
}
