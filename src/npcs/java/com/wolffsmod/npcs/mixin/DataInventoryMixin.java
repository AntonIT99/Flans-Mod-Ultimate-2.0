package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.diag.NpcServerSettings;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Custom NPCs' inventory. A killed NPC drops none of its items while the server's item drops setting is off; its
 * experience drops regardless.
 */
@Mixin(value = DataInventory.class, remap = false)
public abstract class DataInventoryMixin
{
    @Shadow
    private EntityNPCInterface npc;

    @Inject(method = "dropStuff", at = @At("HEAD"))
    private void wolffsmodnpcsSkipDisabledItemDrops(NpcEvent.DiedEvent event, Entity entity, DamageSource damagesource, CallbackInfo callback)
    {
        NpcServerSettings settings = NpcServerSettings.of(npc);
        if (settings != null && !settings.itemDropsEnabled())
            event.droppedItems = new IItemStack[0];
    }
}
