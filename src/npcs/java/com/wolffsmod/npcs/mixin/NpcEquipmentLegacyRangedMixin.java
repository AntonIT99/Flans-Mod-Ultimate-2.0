package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcEquipment;
import noppes.npcs.ai.EntityAIRangedAttack;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents the fallback ranged AI from firing synthetic shots alongside an authoritative equipped weapon. */
@Mixin(value = EntityAIRangedAttack.class, remap = false)
public abstract class NpcEquipmentLegacyRangedMixin
{
    @Shadow @Final
    private EntityNPCInterface npc;

    @Inject(method = {"canUse", "m_8036_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsNativeWeaponAI(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.weaponAuthority(npc))
            callback.setReturnValue(false);
    }
}
