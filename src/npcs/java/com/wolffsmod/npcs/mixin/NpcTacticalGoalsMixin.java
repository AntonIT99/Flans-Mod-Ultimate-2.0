package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcEquipment;
import noppes.npcs.ai.EntityAIAvoidTarget;
import noppes.npcs.ai.EntityAIDodgeShoot;
import noppes.npcs.ai.EntityAIOrbitTarget;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.data.DataInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Custom NPCs' tactics that ask whether the NPC fights at range: dodge, surround and hit and run. An equipped ranged
 * weapon counts as a projectile for them, as it does when the NPC's goals are set up.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = {EntityAIDodgeShoot.class, EntityAIOrbitTarget.class, EntityAIAvoidTarget.class}, remap = false)
public abstract class NpcTacticalGoalsMixin
{
    @Redirect(method = {"canUse", "m_8036_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataInventory;getProjectile()Lnoppes/npcs/api/item/IItemStack;"))
    private IItemStack wolffsmodnpcsWeaponCountsAsProjectile(DataInventory inventory)
    {
        return NpcEquipment.combatProjectile(((DataInventoryAccessor) inventory).wolffsmodnpcsNpc());
    }
}
