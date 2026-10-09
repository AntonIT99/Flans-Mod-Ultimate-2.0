package com.wolffsmod.npcs.combat;

import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Opt-in adapter for modded items whose native use needs integration beyond vanilla living-entity hooks.
 * Implement on the item. Actions run on the server; readouts must be safe on either side.
 */
public interface NpcWeaponAdapter
{
    /** Fires the item's real attack, including ammunition, durability and effects. */
    boolean fireNpcWeapon(EntityNPCInterface npc, LivingEntity target, ItemStack weapon);

    /** Tick interval between triggers; minimum one tick. */
    default double npcShotDelay(ItemStack weapon)
    {
        return 20D;
    }

    /** Field IDs and display values in the Custom NPCs ranged editor; unknown statistics should be omitted. */
    default Map<Integer, String> npcRangedReadouts(ItemStack weapon)
    {
        return Map.of();
    }
}
