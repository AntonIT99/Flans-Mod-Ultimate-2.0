package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.NpcInspectPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.chat.Component;

import java.util.List;

/** The client's copy of what the server last found of the NPC the Diagnosis page shows. Client thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcInspectionView
{
    private static int entityId = -1;
    private static List<Component> effective = List.of();
    private static List<Component> live = List.of();

    public static void receive(NpcInspectPacket packet)
    {
        entityId = packet.entityId();
        effective = packet.effective();
        live = packet.live();
    }

    public static boolean known(int npc)
    {
        return entityId == npc;
    }

    /** @return the values the NPC acts on, with where they come from; none until the server answered about it */
    public static List<Component> effective(int npc)
    {
        return known(npc) ? effective : List.of();
    }

    /** @return what the NPC did since the page last asked; none until the server answered about it */
    public static List<Component> live(int npc)
    {
        return known(npc) ? live : List.of();
    }
}
