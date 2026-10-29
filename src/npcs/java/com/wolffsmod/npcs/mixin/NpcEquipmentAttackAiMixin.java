package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcModels;
import noppes.npcs.ai.EntityAIAttackTarget;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataRanged;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionHand;

/**
 * Uses the selected melee hand's reach, cadence and swing in the existing AI, retaining one shared cooldown and editor fallbacks. An equipped ranged weapon
 * leaves melee to within melee range, as a projectile would; any other equipped weapon fights in melee at any distance. An NPC wearing an AA gun, vehicle
 * or plane model never closes in to strike; a mecha requires a sword addon or a melee damage of its own.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = EntityAIAttackTarget.class, remap = false)
public abstract class NpcEquipmentAttackAiMixin
{
    @Shadow
    private EntityNPCInterface npc;

    @Inject(method = {"canUse", "canContinueToUse"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsNoMachineMelee(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcModels.meleeless(npc))
            callback.setReturnValue(false);
    }

    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataInventory;getProjectile()Lnoppes/npcs/api/item/IItemStack;"))
    private IItemStack wolffsmodnpcsWeaponChoosesAttack(DataInventory inventory)
    {
        return NpcEquipmentRangedGoal.rushes(npc) ? null : NpcEquipment.combatProjectile(npc);
    }

    @Redirect(method = {"canUse", "canContinueToUse"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataRanged;getMeleeRange()I"))
    private int wolffsmodnpcsPursueBetweenThrows(DataRanged ranged)
    {
        return NpcEquipmentRangedGoal.rushes(npc) ? 0 : ranged.getMeleeRange();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataMelee;getDelay()I"))
    private int wolffsmodnpcsMeleeCadence(DataMelee melee)
    {
        return NpcEquipment.itemMelee(npc, EquipmentProperty.MELEE_DELAY) ? NpcEquipment.meleeDelay(NpcEquipment.meleeWeapon(npc)) : melee.getDelay();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataMelee;getRange()I"))
    private int wolffsmodnpcsMeleeReach(DataMelee melee)
    {
        return NpcEquipment.itemMelee(npc, EquipmentProperty.MELEE_REACH) ? (int) Math.round(NpcEquipmentStats.meleeReach(NpcEquipment.meleeWeapon(npc))) : melee.getRange();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/EntityNPCInterface;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private void wolffsmodnpcsSwingMeleeHand(EntityNPCInterface attacker, InteractionHand original)
    {
        attacker.swing(NpcEquipment.enabled(attacker, Feature.MELEE_DUAL_WIELD) ? NpcEquipment.meleeHand(attacker) : original);
    }
}
