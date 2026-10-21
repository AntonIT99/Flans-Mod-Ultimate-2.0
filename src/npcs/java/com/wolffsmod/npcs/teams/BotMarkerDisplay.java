package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Shows operators the bot markers of a map for a while: a column of coloured dust over each marker near them, seen by
 * them alone. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotMarkerDisplay
{
    /** Ticks markers stay shown. */
    public static final int SHOW_TICKS = 2 * 60 * 20;
    private static final int INTERVAL = 10;
    /** Blocks from the operator within which markers are drawn. */
    private static final double RANGE = 128D;
    private static final int COLUMN = 6;
    private static final double COLUMN_STEP = 0.5D;
    private static final float DUST_SIZE = 1.5F;

    private record Showing(String mapId, long until)
    {}

    private static final Map<UUID, Showing> SHOWING = new HashMap<>();

    /** Shows the markers of a map to a player, from now until {@link #SHOW_TICKS} have passed. */
    public static void show(ServerPlayer player, String mapId)
    {
        SHOWING.put(player.getUUID(), new Showing(mapId, player.server.getTickCount() + (long) SHOW_TICKS));
    }

    /** Forgets every display, as the server stops. */
    static void clear()
    {
        SHOWING.clear();
    }

    static void tick(MinecraftServer server)
    {
        int now = server.getTickCount();
        if (SHOWING.isEmpty() || now % INTERVAL != 0)
            return;
        Iterator<Map.Entry<UUID, Showing>> entries = SHOWING.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<UUID, Showing> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || now > entry.getValue().until())
            {
                entries.remove();
                continue;
            }
            draw(player, BotMarkers.get(server).of(entry.getValue().mapId()));
        }
    }

    // The level belongs to the server, which closes it; nothing here opens or owns it.
    @SuppressWarnings("resource")
    private static void draw(ServerPlayer player, List<BotMarkers.Marker> markers)
    {
        ServerLevel level = player.serverLevel();
        for (BotMarkers.Marker marker : markers)
        {
            if (marker.position().distanceTo(player.position()) > RANGE)
                continue;
            DustParticleOptions dust = new DustParticleOptions(colour(marker.type()), DUST_SIZE);
            for (int i = 0; i < COLUMN; i++)
                level.sendParticles(player, dust, true, marker.position().x, marker.position().y + i * COLUMN_STEP, marker.position().z, 1, 0D, 0D, 0D, 0D);
        }
    }

    private static Vector3f colour(BotMarkers.Type type)
    {
        return switch (type)
        {
            case ROUTE -> new Vector3f(1F, 1F, 1F);
            case GARRISON -> new Vector3f(0.2F, 0.4F, 1F);
            case SNIPER -> new Vector3f(1F, 0.2F, 0.2F);
            case AA -> new Vector3f(1F, 0.8F, 0.1F);
            case NAVAL -> new Vector3f(0.1F, 0.9F, 0.9F);
        };
    }
}
