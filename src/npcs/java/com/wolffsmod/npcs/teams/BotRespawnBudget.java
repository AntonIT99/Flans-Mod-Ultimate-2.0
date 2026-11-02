package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Optional bot-only respawn limits; a Teams gamemode always owns its victory conditions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotRespawnBudget
{
    /** Objective modes already account for players and bots through their match score or reinforcements. */
    public static boolean available(String gameType)
    {
        return switch (gameType)
        {
            case "con", "dom", "aslt" -> false;
            default -> true;
        };
    }

    /** Standalone battles retain their own tickets; saved round values are ignored in objective modes. */
    static int limit(BotBattle battle)
    {
        return battle.getGround().round().filter(live -> !available(live.getRound().getGameTypeId())).isPresent() ? 0 : battle.getSettings().getTickets();
    }
}
