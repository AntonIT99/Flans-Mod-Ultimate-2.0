package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.platform.event.FlanCancellableEvent;
import lombok.Getter;

import net.minecraft.world.entity.player.Player;

/**
 * Posted on the game event bus, server side, when a player chooses to enter a
 * driveable seat: by clicking the seat or the hull, or by cycling seats.
 * Cancelling it keeps the player out. It is not posted when a player leaves.
 */
@Getter
public class PlayerEnterSeatEvent extends FlanCancellableEvent
{
    private final Seat seat;
    private final Player player;

    public PlayerEnterSeatEvent(Seat seat, Player player)
    {
        this.seat = seat;
        this.player = player;
    }
}
