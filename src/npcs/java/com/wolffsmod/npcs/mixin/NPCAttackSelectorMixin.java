package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.FactionMobSettings;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Allows faction aggression toward hostile mobs after the selector's normal range and home-distance checks.
 *
 * <p>
 * Every hostile mob counts, not only those extending {@code Monster}: slimes, magma cubes, ghasts, phantoms, shulkers
 * and hoglins carry the {@link Enemy} marker without being {@code Monster}s, as do hostile mobs added by other mods.
 * </p>
 */
@Mixin(value = NPCAttackSelector.class, remap = false)
public abstract class NPCAttackSelectorMixin
{
    @Shadow
    private EntityNPCInterface npc;

    @Inject(method = "isEntityApplicable", at = @At(value = "INVOKE", target = "Lnoppes/npcs/roles/JobInterface;getType()I"), cancellable = true)
    private void wolffsmodnpcsAttackHostileMobs(LivingEntity entity, CallbackInfoReturnable<Boolean> callback)
    {
        if (entity instanceof Enemy && ((FactionMobSettings) npc.faction).wolffsmodnpcsAttackMobs())
            callback.setReturnValue(!npc.ais.directLOS || npc.getSensing().hasLineOfSight(entity));
    }
}
