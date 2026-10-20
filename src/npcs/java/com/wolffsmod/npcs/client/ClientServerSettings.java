package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.network.ServerControlPacket;
import lombok.*;

/** The client's copy of the world-wide NPC settings, as the server last sent them. Client thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientServerSettings
{
    /** Whether the server has answered since the page was opened; until then the controls stay read-only. */
    @Getter
    private static boolean known;
    @Getter
    private static int nearInterval = NpcServerSettings.DEFAULT_NEAR_INTERVAL;
    @Getter
    private static int farInterval = NpcServerSettings.DEFAULT_FAR_INTERVAL;
    @Getter
    private static boolean profiling;
    @Getter
    private static int aggroRange = NpcServerSettings.DEFAULT_AGGRO_RANGE;
    @Getter
    private static int firingRange = NpcServerSettings.DEFAULT_FIRING_RANGE;

    /** Forgets the copy, so that a page opened later waits for the server again. */
    public static void forget()
    {
        known = false;
    }

    public static void receive(ServerControlPacket packet)
    {
        known = true;
        nearInterval = packet.nearInterval();
        farInterval = packet.farInterval();
        profiling = packet.profiling();
        aggroRange = packet.aggroRange();
        firingRange = packet.firingRange();
    }
}
