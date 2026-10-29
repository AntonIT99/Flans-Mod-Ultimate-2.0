package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.NpcRemotePacket;

/** Receives wand scan results only while the remote browser is open. */
public interface NpcRemoteReceiver
{
    void wolffsmodnpcsReceive(NpcRemotePacket packet);
}
