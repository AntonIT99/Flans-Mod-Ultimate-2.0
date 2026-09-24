package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Seat;
import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import net.minecraft.world.entity.player.Player;

/**
 * Posted on the game event bus, server side, when a player chooses to enter a
 * driveable seat: by clicking the seat or the hull, or by cycling seats.
 * Cancelling it keeps the player out. It is not posted when a player leaves.
 */
@Getter
public class PlayerEnterSeatEvent extends Event implements ICancellableEvent
{
    private final Seat seat;
    private final Player player;

    public PlayerEnterSeatEvent(Seat seat, Player player)
    {
        this.seat = seat;
        this.player = player;
    }
}
