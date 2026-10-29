package com.wolffsmod.npcs.diag;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The aggro and firing ranges an NPC inherits from a Flan's model, as the side asking knows them: the server reads its saved
 * settings, a client what its server last sent it, which the editor's readouts show.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcModelRanges
{
    private static final AtomicInteger clientAggro = new AtomicInteger(NpcServerSettings.DEFAULT_MODEL_RANGE);
    private static final AtomicInteger clientFiring = new AtomicInteger(NpcServerSettings.DEFAULT_MODEL_RANGE);

    /** Takes the ranges the server sent. Client thread. */
    public static void received(int aggro, int firing)
    {
        clientAggro.set(aggro);
        clientFiring.set(firing);
    }

    /** @return the aggro range, in blocks, an NPC inherits from its model */
    public static int aggro(Entity npc)
    {
        NpcServerSettings settings = NpcServerSettings.of(npc);
        return settings == null ? clientAggro.get() : settings.getModelAggroRange();
    }

    /** @return the firing range, in blocks, an NPC inherits from its model */
    public static int firing(Entity npc)
    {
        NpcServerSettings settings = NpcServerSettings.of(npc);
        return settings == null ? clientFiring.get() : settings.getModelFiringRange();
    }
}
