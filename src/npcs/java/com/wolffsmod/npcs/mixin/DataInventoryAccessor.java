package com.wolffsmod.npcs.mixin;

import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the NPC an inventory belongs to, for hooks that only see the inventory. */
@Mixin(value = DataInventory.class, remap = false)
public interface DataInventoryAccessor
{
    @Accessor("npc")
    EntityNPCInterface wolffsmodnpcsNpc();
}
