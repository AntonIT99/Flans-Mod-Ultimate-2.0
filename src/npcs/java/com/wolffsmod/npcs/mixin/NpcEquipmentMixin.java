package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.EquippedArmorProperties;
import com.flansmodultimate.api.IEquipmentPolicy;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.combat.NpcItemAttacks;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.Resistances;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Equipment owns native combat; Custom NPCs' stored stats remain fallback/editor data. */
@Mixin(value = EntityNPCInterface.class, remap = false)
// Mixin inheritance follows the target entity; Minecraft owns entity identity/equality.
@SuppressWarnings({"java:S110", "java:S2160"})
public abstract class NpcEquipmentMixin extends PathfinderMob implements IEquipmentPolicy
{
    @Unique
    private NpcEquipmentRangedGoal wolffsmodnpcsItemGoal;
    @Unique
    private int wolffsmodnpcsShieldCooldown;

    protected NpcEquipmentMixin(EntityType<? extends PathfinderMob> type, Level level)
    {
        super(type, level);
    }

    @Override
    public boolean flansArmorEffects()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.ITEM_ARMOR);
    }

    @Override
    public boolean flansWeaponEffects()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.ITEM_WEAPONS);
    }

    @Override
    public boolean flansArmorAnimations()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.ARMOR_ANIMATIONS);
    }

    @Override
    public boolean flansWeaponAnimations()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.WEAPON_ANIMATIONS);
    }

    @Inject(method = "updateTasks", at = @At("TAIL"))
    private void wolffsmodnpcsItemAI(CallbackInfo callback)
    {
        if (wolffsmodnpcsItemGoal != null)
            goalSelector.removeGoal(wolffsmodnpcsItemGoal);
        wolffsmodnpcsItemGoal = new NpcEquipmentRangedGoal((EntityNPCInterface) (Object) this);
        goalSelector.addGoal(1, wolffsmodnpcsItemGoal);
    }

    @Inject(method = {"tick", "m_8119_"}, at = @At("TAIL"))
    private void wolffsmodnpcsEquipmentTick(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (level().isClientSide)
            return;
        updateArmorAttributes();
        if (wolffsmodnpcsShieldCooldown > 0)
            wolffsmodnpcsShieldCooldown--;
        if (!flansWeaponEffects() || npc.getTarget() == null || !npc.getTarget().isAlive() || wolffsmodnpcsShieldCooldown > 0)
        {
            if (isUsingItem() && NpcEquipment.shield(getUseItem()))
                stopUsingItem();
        }
        else if (!isUsingItem())
        {
            if (NpcEquipment.shield(getOffhandItem()))
                startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
            else if (NpcEquipment.shield(getMainHandItem()))
                startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    @Unique
    private void updateArmorAttributes()
    {
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() != EquipmentSlot.Type.ARMOR)
                continue;
            ItemStack armor = getItemBySlot(slot);
            armor.getAttributeModifiers(slot).forEach((attribute, modifier) ->
            {
                var instance = getAttribute(attribute);
                if (instance == null)
                    return;
                if (!flansArmorEffects())
                    instance.removeModifier(modifier.getId());
                else if (!instance.hasModifier(modifier))
                    instance.addTransientModifier(modifier);
            });
        }
    }

    @Inject(method = {"doHurtTarget", "m_7327_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsItemMelee(Entity target, CallbackInfoReturnable<Boolean> callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (NpcEquipment.weaponAuthority(npc))
            callback.setReturnValue(NpcItemAttacks.melee(npc, target));
    }

    @Inject(method = {"getDamageAfterArmorAbsorb", "m_21161_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsNativeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> callback)
    {
        if (flansArmorEffects())
            callback.setReturnValue(super.getDamageAfterArmorAbsorb(source, amount));
    }

    @Inject(method = {"fireImmune", "m_5825_"}, at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsArmorFireProtection(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquippedArmorProperties::fireResistance))
            callback.setReturnValue(true);
    }

    @Inject(method = {"decreaseAirSupply", "m_7302_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsArmorAir(int air, CallbackInfoReturnable<Integer> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquippedArmorProperties::waterBreathing))
            callback.setReturnValue(air);
    }

    @Inject(method = {"causeFallDamage", "m_142535_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsArmorFall(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquippedArmorProperties::negatesFallDamage))
            callback.setReturnValue(false);
    }

    @Redirect(method = {"hurt", "m_6469_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/Resistances;applyResistance(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float wolffsmodnpcsArmorNotEditor(Resistances resistances, DamageSource source, float amount)
    {
        return flansArmorEffects() ? amount : resistances.applyResistance(source, amount);
    }

    @Redirect(method = {"knockback", "m_147240_"}, at = @At(value = "FIELD", target = "Lnoppes/npcs/Resistances;knockback:F"))
    private float wolffsmodnpcsNativeKnockback(Resistances resistances)
    {
        return flansArmorEffects() ? 1F : resistances.knockback;
    }

    // LivingEntity's default equipment wear hooks are empty for ordinary mobs.
    @Override
    protected void hurtArmor(DamageSource source, float amount)
    {
        if (!flansArmorEffects() || amount <= 0F)
            return;
        int wear = Math.max(1, (int) (amount / 4F));
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() == EquipmentSlot.Type.ARMOR)
            {
                ItemStack stack = getItemBySlot(slot);
                if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem && (!source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || !stack.getItem().isFireResistant()))
                    stack.hurtAndBreak(wear, this, holder -> holder.broadcastBreakEvent(slot));
            }
        }
    }

    @Override
    protected void hurtCurrentlyUsedShield(float amount)
    {
        if (amount >= 3F && flansWeaponEffects() && NpcEquipment.shield(getUseItem()))
        {
            getUseItem().hurtAndBreak(1 + (int) amount, this, holder -> holder.broadcastBreakEvent(getUsedItemHand()));
            if (getUseItem().isEmpty())
                stopUsingItem();
        }
    }

    @Override
    protected void blockUsingShield(LivingEntity attacker)
    {
        super.blockUsingShield(attacker);
        if (attacker.getMainHandItem().canDisableShield(getUseItem(), this, attacker))
        {
            wolffsmodnpcsShieldCooldown = 100;
            stopUsingItem();
            level().broadcastEntityEvent(this, (byte) 30);
        }
    }
}
