package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Seat;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.world.entity.player.Player;

/**
 * Posted on the Forge event bus, server side, when a player chooses to enter a
 * driveable seat: by clicking the seat or the hull, or by cycling seats.
 * Cancelling it keeps the player out. It is not posted when a player leaves.
 */
@Cancelable
@Getter
public class PlayerEnterSeatEvent extends Event
{
    private final Seat seat;
    private final Player player;

    public PlayerEnterSeatEvent(Seat seat, Player player)
    {
        this.seat = seat;
        this.player = player;
    }
}
