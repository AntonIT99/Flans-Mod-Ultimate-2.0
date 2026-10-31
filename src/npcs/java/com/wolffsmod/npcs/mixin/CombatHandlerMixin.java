package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.tactics.NpcThreats;
import com.wolffsmod.npcs.tactics.NpcWeaponRange;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.ai.CombatHandler;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hooks into the combat handler, which lets an NPC's target go once it is beyond the NPC's aggro range:
 * <ul>
 * <li>Weapon ranges: an NPC whose firing range comes from its weapon keeps targets as far as that weapon reaches.</li>
 * <li>Threats: an NPC that was hit records its attacker as its threat, and keeps a threat it remembers as its target
 * wherever it shoots from.</li>
 * </ul>
 */
@Mixin(value = CombatHandler.class, remap = false)
public abstract class CombatHandlerMixin
{
    @Shadow
    private EntityNPCInterface npc;

    // Weapon ranges

    @Redirect(method = "isValidTarget", at = @At(value = "FIELD", target = "Lnoppes/npcs/entity/data/DataStats;aggroRange:I", opcode = Opcodes.GETFIELD))
    private int wolffsmodnpcsWeaponAggroRange(DataStats stats)
    {
        return NpcWeaponRange.aggro(npc);
    }

    // Threats

    @Inject(method = "isValidTarget", at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsKeepThreat(LivingEntity target, CallbackInfoReturnable<Boolean> callback)
    {
        if (!callback.getReturnValueZ() && target != null && NpcThreats.keeps(npc, target))
            callback.setReturnValue(true);
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void wolffsmodnpcsRecordThreat(DamageSource source, float amount, CallbackInfo callback)
    {
        if (NoppesUtilServer.GetDamageSourcee(source) instanceof LivingEntity attacker)
            NpcThreats.hit(npc, attacker);
    }
}
