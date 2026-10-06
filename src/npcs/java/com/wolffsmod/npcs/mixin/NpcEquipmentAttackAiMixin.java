package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.EquipmentAttributes;
import com.wolffsmod.npcs.combat.NpcEquipment;
import net.minecraftforge.common.ForgeMod;
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

/** Uses weapon reach/cadence in the existing melee AI, keeping editor values as saved fallbacks. */
@Mixin(value = EntityAIAttackTarget.class, remap = false)
public abstract class NpcEquipmentAttackAiMixin
{
    @Shadow
    private EntityNPCInterface npc;

    @Redirect(method = {"canUse", "m_8036_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataInventory;getProjectile()Lnoppes/npcs/api/item/IItemStack;"))
    private IItemStack wolffsmodnpcsWeaponChoosesAttack(DataInventory inventory)
    {
        return NpcEquipment.weaponAuthority(npc) ? null : inventory.getProjectile();
    }

    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataMelee;getDelay()I"))
    private int wolffsmodnpcsMeleeCadence(DataMelee melee)
    {
        return NpcEquipment.weaponAuthority(npc) ? NpcEquipment.meleeDelay(npc.getMainHandItem()) : melee.getDelay();
    }

    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataMelee;getRange()F"))
    private float wolffsmodnpcsMeleeReach(DataMelee melee)
    {
        return NpcEquipment.weaponAuthority(npc)
            ? (float) EquipmentAttributes.value(npc.getMainHandItem(), EquipmentSlot.MAINHAND, ForgeMod.ENTITY_REACH.get(), 3D)
            : melee.getRange();
    }
}
