package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.diag.NpcModelRanges;
import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import com.wolffsmod.npcs.network.ServerControlPacket;
import com.wolffsmod.npcs.network.ServerReportPacket;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/** The client's copy of the world-wide NPC settings, and of the census and benchmarks, as the server last sent them. Client thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientServerSettings
{
    /** Whether the server has answered since the page was opened; until then the controls stay read-only. */
    @Getter
    private static boolean known;
    private static final int[] VALUES = Setting.fallbacks();
    /** The census and benchmarks the server last sent, or null until it answers. */
    @Nullable @Getter
    private static ServerReportPacket report;

    /** Forgets the copy, so that a page opened later waits for the server again. */
    public static void forget()
    {
        known = false;
    }

    public static void forgetReport()
    {
        report = null;
    }

    public static int get(Setting setting)
    {
        return VALUES[setting.ordinal()];
    }

    /** @return every value, in the order of {@link Setting}, with one of them replaced */
    public static List<Integer> with(Setting setting, int value)
    {
        int[] copy = Arrays.copyOf(VALUES, VALUES.length);
        copy[setting.ordinal()] = value;
        return Arrays.stream(copy).boxed().toList();
    }

    public static int getNearInterval()
    {
        return get(Setting.NEAR_INTERVAL);
    }

    public static int getFarInterval()
    {
        return get(Setting.FAR_INTERVAL);
    }

    public static void receive(ServerControlPacket packet)
    {
        known = true;
        for (int index = 0; index < Math.min(VALUES.length, packet.values().size()); index++)
            VALUES[index] = packet.values().get(index);
        NpcModelRanges.received(get(Setting.MODEL_AGGRO_RANGE), get(Setting.MODEL_FIRING_RANGE));
    }

    public static void receive(ServerReportPacket packet)
    {
        report = packet;
    }
}
