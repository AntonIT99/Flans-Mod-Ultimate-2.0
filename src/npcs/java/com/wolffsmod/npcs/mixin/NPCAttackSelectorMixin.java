package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.tactics.NpcWeaponRange;
import com.wolffsmod.npcs.teams.FactionMobSettings;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Hooks into the selector that decides which entities an NPC may pick as its target:
 * <ul>
 * <li>Hostile mobs: allows faction aggression toward hostile mobs after the selector's normal range and home-distance
 * checks. Every hostile mob counts, not only those extending {@code Monster}: slimes, magma cubes, ghasts, phantoms,
 * shulkers and hoglins carry the {@link Enemy} marker without being {@code Monster}s, as do hostile mobs added by other
 * mods.</li>
 * <li>Weapon ranges: an NPC whose firing range comes from its weapon picks targets as far as that weapon reaches.</li>
 * </ul>
 */
@Mixin(value = NPCAttackSelector.class, remap = false)
public abstract class NPCAttackSelectorMixin
{
    @Shadow
    private EntityNPCInterface npc;

    // Hostile mobs

    @Inject(method = "isEntityApplicable", at = @At(value = "INVOKE", target = "Lnoppes/npcs/roles/JobInterface;getType()I"), cancellable = true)
    private void wolffsmodnpcsAttackHostileMobs(LivingEntity entity, CallbackInfoReturnable<Boolean> callback)
    {
        if (entity instanceof Enemy && ((FactionMobSettings) npc.faction).wolffsmodnpcsAttackMobs())
            callback.setReturnValue(!npc.ais.directLOS || npc.getSensing().hasLineOfSight(entity));
    }

    // Weapon ranges

    @Redirect(method = "isEntityApplicable", at = @At(value = "FIELD", target = "Lnoppes/npcs/entity/data/DataStats;aggroRange:I", opcode = Opcodes.GETFIELD))
    private int wolffsmodnpcsWeaponAggroRange(DataStats stats)
    {
        return NpcWeaponRange.aggro(npc);
    }
}
