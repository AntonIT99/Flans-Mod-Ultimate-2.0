package com.flansmodultimate.common.entity;

import net.minecraft.server.level.ServerPlayer;
import java.util.List;

/** Internal bridge across the private vanilla tracker class. */
public interface EntityTrackingRefresh
{
    void flansmodultimate$refreshTracking(List<ServerPlayer> players);
}
