package com.wolffsmod.npcs.teams;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * What a player told a squad of bots to do: follow them, hold a place, or take or hold an objective. It comes before the
 * squad's commander's task until it runs out or is released.
 *
 * @param player
 *            the player who gave it, whom a squad told to follow follows
 * @param target
 *            where the squad goes; for an order to follow, where the player was when it was given
 * @param expiresAt
 *            game time at which the squad returns to its commander's tasks
 */
public record PlayerOrder(UUID player, Kind kind, Vec3 target, long expiresAt)
{
    /** Ticks an order lasts: five minutes. */
    public static final long DURATION = 5L * 60L * 20L;

    /** What a player may tell a squad. */
    public enum Kind
    {
        FOLLOW, HOLD, ATTACK, DEFEND, RELEASE
    }

    /** The task the order gives the squad, with the player where they are now for an order to follow. */
    public SquadTask task(Vec3 playerPosition)
    {
        return switch (kind)
        {
            case FOLLOW -> new SquadTask(SquadTask.Kind.GUARD, playerPosition, 5D);
            case HOLD -> new SquadTask(SquadTask.Kind.DEFEND, target, 4D);
            case ATTACK -> new SquadTask(SquadTask.Kind.ATTACK, target, 6D);
            case DEFEND, RELEASE -> new SquadTask(SquadTask.Kind.DEFEND, target, 6D);
        };
    }
}
