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
import net.minecraft.world.entity.monster.Monster;

/** Allows faction monster aggression after the selector's normal range and home-distance checks. */
@Mixin(value = NPCAttackSelector.class, remap = false)
public abstract class NPCAttackSelectorMixin
{
    @Shadow
    private EntityNPCInterface npc;

    @Inject(method = "isEntityApplicable", at = @At(value = "INVOKE", target = "Lnoppes/npcs/roles/JobInterface;getType()I"), cancellable = true)
    private void wolffsmodnpcsAttackMonsters(LivingEntity entity, CallbackInfoReturnable<Boolean> callback)
    {
        if (entity instanceof Monster && ((FactionMobSettings) npc.faction).wolffsmodnpcsAttackMobs())
            callback.setReturnValue(!npc.ais.directLOS || npc.getSensing().hasLineOfSight(entity));
    }
}
