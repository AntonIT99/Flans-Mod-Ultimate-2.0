package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ForcePlannerTest
{
    private static Map<String, Integer> ordered(Object... entries)
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2)
            map.put((String) entries[i], (Integer) entries[i + 1]);
        return map;
    }

    @Test
    void fixedCountsAreTakenAsTheyAre()
    {
        assertEquals(ordered("us", 13, "germany", 40), ForcePlanner.infantryTargets(BotSettings.Mode.FIXED, 0, ordered("us", 13, "germany", 40), Map.of("us", 5)));
    }

    @Test
    void aTotalIsSharedByWeightAndAlwaysAddsUp()
    {
        // A third and two thirds, as for a one-to-two ratio.
        assertEquals(ordered("us", 30, "germany", 60), ForcePlanner.infantryTargets(BotSettings.Mode.SHARE, 90, ordered("us", 1, "germany", 2), Map.of()));
        Map<String, Integer> uneven = ForcePlanner.infantryTargets(BotSettings.Mode.SHARE, 10, ordered("a", 1, "b", 1, "c", 1), Map.of());
        assertEquals(10, uneven.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(ordered("a", 4, "b", 3, "c", 3), uneven, "the leftover goes to the team listed first");
    }

    @Test
    void teamsWithoutWeightShareEquallyWhenNobodyHasAny()
    {
        assertEquals(ordered("a", 5, "b", 5), ForcePlanner.share(10, ordered("a", 0, "b", 0)));
        assertEquals(ordered("a", 10, "b", 0), ForcePlanner.share(10, ordered("a", 3, "b", 0)));
    }

    @Test
    void aSideTrailingOnScoreGetsExtraInfantry()
    {
        Map<String, Integer> sides = Map.of("us", 0, "uk", 0, "germany", 1);
        // Germany trails by half the score limit, so it gets half of the 40 percent: 20 percent more.
        Map<String, Integer> adjusted = ForcePlanner.rubberBand(ordered("us", 10, "uk", 10, "germany", 20), sides, Map.of(0, 100, 1, 50), 100, 40);
        assertEquals(ordered("us", 10, "uk", 10, "germany", 24), adjusted);
        assertEquals(ordered("us", 10), ForcePlanner.rubberBand(ordered("us", 10), sides, Map.of(0, 0, 1, 90), 100, 0), "switched off, nothing changes");
    }

    @Test
    void fillTopsEachTeamUpToItsTargetWithPlayersCounted()
    {
        Map<String, Integer> targets = ForcePlanner.infantryTargets(BotSettings.Mode.FILL, 0, ordered("us", 20, "germany", 40), Map.of("us", 3, "germany", 45));
        assertEquals(ordered("us", 17, "germany", 0), targets);
    }
}
