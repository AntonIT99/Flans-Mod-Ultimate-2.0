package com.wolffsmod.npcs.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Where a player's orders send their squads, and the rally point of the squad they lead, for their client to mark in the
 * world. Sent while the player has any, and once empty when the last one is gone.
 *
 * @param waypoints
 *            the places, each with the squad and what it does there
 */
public record BotWaypointsPacket(List<Waypoint> waypoints)
{
    private static final int MAX_KIND = 16;
    private static final int MAX_WAYPOINTS = 64;

    /**
     * A marked place.
     *
     * @param squad
     *            the squad that goes there
     * @param kind
     *            what it does there: {@code hold}, {@code attack}, {@code defend} or {@code rally}
     */
    public record Waypoint(double x, double y, double z, int squad, String kind)
    {}

    public BotWaypointsPacket
    {
        waypoints = List.copyOf(waypoints.subList(0, Math.min(waypoints.size(), MAX_WAYPOINTS)));
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeCollection(waypoints, (out, waypoint) ->
        {
            out.writeDouble(waypoint.x());
            out.writeDouble(waypoint.y());
            out.writeDouble(waypoint.z());
            out.writeVarInt(waypoint.squad());
            out.writeUtf(waypoint.kind(), MAX_KIND);
        });
    }

    public static BotWaypointsPacket read(FriendlyByteBuf buffer)
    {
        List<Waypoint> waypoints = buffer.readList(in -> new Waypoint(in.readDouble(), in.readDouble(), in.readDouble(), in.readVarInt(), in.readUtf(MAX_KIND)));
        return new BotWaypointsPacket(waypoints);
    }
}
