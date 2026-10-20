package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.tactics.NpcTactics;
import noppes.npcs.ai.EntityAIDodgeShoot;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Custom NPCs' dodge tactic.
 *
 * <ul>
 * <li>An equipped ranged weapon counts as a projectile for it, as it does when the NPC's goals are set up.</li>
 * <li>It waits while a tactic of the mod's own moves the NPC.</li>
 * </ul>
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = EntityAIDodgeShoot.class, remap = false)
public abstract class EntityAIDodgeShootMixin
{
    @Shadow
    private EntityNPCInterface entity;

    @Inject(method = {"canUse", "m_8036_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsYieldToTactic(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcTactics.of(entity) != Tactic.CUSTOM_NPCS)
            callback.setReturnValue(false);
    }

    @Redirect(method = {"canUse", "m_8036_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataInventory;getProjectile()Lnoppes/npcs/api/item/IItemStack;"))
    private IItemStack wolffsmodnpcsWeaponCountsAsProjectile(DataInventory inventory)
    {
        return NpcEquipment.combatProjectile(entity);
    }
}
