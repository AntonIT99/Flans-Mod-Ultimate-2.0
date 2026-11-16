package com.wolffsmod.npcs.properties;

/**
 * The share of its normal speed an NPC moves at, as the server measures it, synchronized for the client's engine pitch.
 * <p>
 * Clients cannot measure it themselves: an NPC's position reaches them only every few ticks and is interpolated
 * unevenly in between, so its movement from one client tick to the next swings between far too fast and standing
 * still, and an engine pitch following it stutters.
 */
public interface NpcEngineSpeed
{
    /** @return the synchronized speed share, from 0 standing to 1 at normal speed */
    float wolffsmodnpcsEngineSpeed();

    /** Sets the speed share the clients follow. Server thread. */
    void wolffsmodnpcsEngineSpeed(float share);
}
