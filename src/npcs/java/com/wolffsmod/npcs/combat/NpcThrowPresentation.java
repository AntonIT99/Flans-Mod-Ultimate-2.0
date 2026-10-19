package com.wolffsmod.npcs.combat;

import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.item.ItemStack;

/** Transient, server-synchronized throw display; the NPC's actual inventory is never moved or replaced. */
public interface NpcThrowPresentation
{
    ItemStack wolffsmodnpcsThrowDisplay();

    void wolffsmodnpcsThrowDisplay(ItemStack stack);

    static void show(EntityNPCInterface npc, ItemStack stack)
    {
        if (npc instanceof NpcThrowPresentation presentation)
            presentation.wolffsmodnpcsThrowDisplay(stack);
    }

    static void clear(EntityNPCInterface npc)
    {
        show(npc, ItemStack.EMPTY);
    }
}
