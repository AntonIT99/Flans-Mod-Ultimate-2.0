package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;

/** How many loose infantry bots each team should field. Pure arithmetic, any thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ForcePlanner
{
    /**
     * @param counts
     *            each team's count setting, in round order: a count, a share weight or a fill target
     * @param players
     *            players fighting for each team; teams left out have none
     * @return the bots each team should field, in the order of {@code counts}
     */
    public static Map<String, Integer> infantryTargets(BotSettings.Mode mode, int total, Map<String, Integer> counts, Map<String, Integer> players)
    {
        Map<String, Integer> targets = new LinkedHashMap<>();
        switch (mode)
        {
            case FIXED -> counts.forEach((team, count) -> targets.put(team, Math.max(0, count)));
            case FILL -> counts.forEach((team, count) -> targets.put(team, Math.max(0, count - players.getOrDefault(team, 0))));
            case SHARE -> targets.putAll(share(Math.max(0, total), counts));
        }
        return targets;
    }

    /**
     * Shares a total out in proportion to weights, by largest remainders, so the parts always add up to the total. When
     * no team has any weight, every team weighs the same.
     */
    static Map<String, Integer> share(int total, Map<String, Integer> weights)
    {
        Map<String, Integer> parts = new LinkedHashMap<>();
        if (weights.isEmpty())
            return parts;
        long weightSum = weights.values().stream().mapToLong(weight -> Math.max(0, weight)).sum();
        Map<String, Long> effective = new LinkedHashMap<>();
        weights.forEach((team, weight) -> effective.put(team, weightSum == 0 ? 1L : Math.max(0, weight)));
        long sum = weightSum == 0 ? weights.size() : weightSum;

        int assigned = 0;
        Map<String, Long> remainders = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : effective.entrySet())
        {
            long exact = entry.getValue() * total;
            int part = (int) (exact / sum);
            parts.put(entry.getKey(), part);
            remainders.put(entry.getKey(), exact % sum);
            assigned += part;
        }
        // The units left over go to the largest remainders; ties go to the team listed first.
        List<Map.Entry<String, Long>> order = new ArrayList<>(remainders.entrySet());
        order.sort(Map.Entry.<String, Long>comparingByValue().reversed());
        for (int i = 0; assigned < total && i < order.size(); i++, assigned++)
            parts.merge(order.get(i).getKey(), 1, Integer::sum);
        return parts;
    }
}
