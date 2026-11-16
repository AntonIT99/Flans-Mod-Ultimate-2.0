package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.diag.NpcStatus;
import com.wolffsmod.npcs.network.NpcStatusPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;

import java.util.Optional;

/** The client's copy of what the server last said the NPC the General tab shows is doing. Client thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcStatusView
{
    /** How long an answer is shown after the server stopped sending them, as when the NPC left the player's level. */
    private static final long STALE_MILLIS = 3000L;

    private static int entityId = -1;
    @Nullable
    private static NpcStatus status;
    private static long receivedAt;

    public static void receive(NpcStatusPacket packet)
    {
        if (packet.status() == null)
            return;
        entityId = packet.entityId();
        status = packet.status();
        receivedAt = Util.getMillis();
    }

    /** @return what the NPC is doing, while the server answers about it */
    public static Optional<NpcStatus> get(int npc)
    {
        if (entityId != npc || Util.getMillis() - receivedAt > STALE_MILLIS)
            return Optional.empty();
        return Optional.ofNullable(status);
    }
}
