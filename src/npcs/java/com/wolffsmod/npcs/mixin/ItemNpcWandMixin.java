package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.NpcCreationFactions;
import noppes.npcs.items.ItemNpcWand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Applies the creating player's faction default before the wand adds a new NPC to the world. */
@Mixin(value = ItemNpcWand.class, remap = false)
public abstract class ItemNpcWandMixin
{
    @Redirect(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    @SuppressWarnings("DefaultAnnotationParam") // The vanilla invocation must remap despite the containing mixin's remap = false.
    private boolean wolffsmodnpcsAssignCreatedFaction(Level level, Entity entity, UseOnContext context)
    {
        NpcCreationFactions.apply(context.getPlayer(), entity);
        return level.addFreshEntity(entity);
    }
}
