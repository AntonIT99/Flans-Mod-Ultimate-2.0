package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcItemAttacks;
import com.wolffsmod.npcs.combat.NpcRangedAttack;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;

/** Replaces only recognized, enabled Flan ammunition; every other projectile retains the original path. */
@Mixin(value = EntityNPCInterface.class, remap = false)
public abstract class NpcRangedAttackMixin
{
    @Unique
    private int wolffsmodnpcsRound;

    // Custom NPCs uses SRG names in production and Mojang names in the development dependency.
    @Inject(method = {"performRangedAttack", "m_6504_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsFireFlanRound(LivingEntity target, float distanceFactor, CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (NpcEquipment.weaponAuthority(npc))
        {
            // Scripted ranged triggers use the same item action, never the editor projectile template.
            if (NpcEquipment.ranged(npc) && NpcItemAttacks.ranged(npc, target))
                NpcItemAttacks.launched(npc, target);
            callback.cancel();
            return;
        }
        if (NpcRangedAttack.fire((EntityNPCInterface) (Object) this, target, distanceFactor == 1F, wolffsmodnpcsRound))
        {
            wolffsmodnpcsRound = wolffsmodnpcsRound >= Integer.MAX_VALUE / 10 - 1 ? 0 : wolffsmodnpcsRound + 1;
            callback.cancel();
        }
    }
}
