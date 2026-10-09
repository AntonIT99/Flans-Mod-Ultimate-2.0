package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.EquipmentAttributes;
import com.wolffsmod.npcs.combat.NpcEquipment;
import noppes.npcs.ai.EntityAIAttackTarget;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataMelee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Uses weapon reach/cadence in the existing melee AI, keeping editor values as saved fallbacks. */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = EntityAIAttackTarget.class, remap = false)
public abstract class NpcEquipmentAttackAiMixin
{
    @Shadow
    private EntityNPCInterface npc;

    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataInventory;getProjectile()Lnoppes/npcs/api/item/IItemStack;"))
    private IItemStack wolffsmodnpcsWeaponChoosesAttack(DataInventory inventory)
    {
        return NpcEquipment.weaponAuthority(npc) ? null : inventory.getProjectile();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataMelee;getDelay()I"))
    private int wolffsmodnpcsMeleeCadence(DataMelee melee)
    {
        return NpcEquipment.weaponAuthority(npc) ? NpcEquipment.meleeDelay(npc.getMainHandItem()) : melee.getDelay();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataMelee;getRange()I"))
    private int wolffsmodnpcsMeleeReach(DataMelee melee)
    {
        return NpcEquipment.weaponAuthority(npc)
            ? (int) Math.round(EquipmentAttributes.value(npc.getMainHandItem(), EquipmentSlot.MAINHAND, Attributes.ENTITY_INTERACTION_RANGE, 3D))
            : melee.getRange();
    }
}
