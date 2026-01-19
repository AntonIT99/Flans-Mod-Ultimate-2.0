package com.flansmodultimate.common.item;

import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfigs;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CustomArmorItem extends ArmorItem implements IFlanItem<ArmorType>
{
    private static final int EFFECT_CHECK_PERIOD = 40; // every 2 seconds
    protected static final int EFFECT_DURATION = 600; // 30 seconds
    protected static final int EFFECT_REFRESH_THRESHOLD = 60; // refresh when < 3 seconds remaining
    protected static final Map<UUID, Set<MobEffect>> LAST_ARMOR_EFFECTS = new HashMap<>();

    protected static final UUID[] speed_uuid = new UUID[] { UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID() };
    protected static final UUID[] kb_uuid = new UUID[] { UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID() };

    @Getter
    protected final ArmorType configType;
    protected final String shortname;

    public CustomArmorItem(ArmorType configType)
    {
        super(new CustomArmorMaterial(configType), configType.getArmorItemType(), new Properties());
        this.configType = configType;
        shortname = configType.getShortName();
    }

    @Override
    public int getEnchantmentValue()
    {
        if (!configType.isReadEnchantability())
            return ModCommonConfigs.defaultArmorEnchantability.get();
        return material.getEnchantmentValue();
    }

    @Override
    public boolean isDamageable(ItemStack stack)
    {
        //0 = Non-breakable, 1 = All breakable, 2 = Refer to armor config
        int breakType = ModCommonConfigs.breakableArmor.get();
        return (breakType == 2 && configType.hasDurability()) || breakType == 1;
    }

    @Override
    public int getMaxDamage(ItemStack stack)
    {
        if (ModCommonConfigs.breakableArmor.get() == 1)
        {
            return ModCommonConfigs.defaultArmorDurability.get();
        }
        return super.getMaxDamage(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.empty());

        String defense = BooleanUtils.isTrue(ModCommonConfigs.enableOldArmorRatioSystem.get()) ?
                IFlanItem.formatDouble(configType.getDefence() * 100.0) + "%" : String.valueOf(Math.round(configType.getDefence() * ArmorType.ARMOR_POINT_FACTOR));
        String bulletDefense = BooleanUtils.isTrue(ModCommonConfigs.enableOldArmorRatioSystem.get()) ?
                IFlanItem.formatDouble(configType.getBulletDefence() * 100.0) + "%" : String.valueOf(Math.round(configType.getBulletDefence() * ArmorType.ARMOR_POINT_FACTOR));

        tooltipComponents.add(IFlanItem.statLine("Defense", defense));
        if (configType.getBulletDefence() != configType.getDefence())
            tooltipComponents.add(IFlanItem.statLine("Bullet Defense", bulletDefense));

        if (configType.getDurability() > 0F)
            tooltipComponents.add(IFlanItem.statLine("Durability", IFlanItem.formatDouble(configType.getDurability())));
        if (configType.getEnchantability() > 0F)
            tooltipComponents.add(IFlanItem.statLine("Enchantability", IFlanItem.formatDouble(getEnchantmentValue())));

        if (Math.abs(configType.getJumpModifier() - 1F) > 0F)
            tooltipComponents.add(IFlanItem.modifierLine("Jump Height", configType.getJumpModifier(), false));

        if (configType.isSmokeProtection())
            tooltipComponents.add(Component.literal("+Smoke Protection").withStyle(ChatFormatting.DARK_GREEN));
        if (configType.isNightVision())
            tooltipComponents.add(Component.literal("+Night Vision").withStyle(ChatFormatting.DARK_GREEN));
        if (configType.isInvisible())
            tooltipComponents.add(Component.literal("+Invisibility").withStyle(ChatFormatting.DARK_GREEN));
        if (configType.isNegateFallDamage())
            tooltipComponents.add(Component.literal("+Negates Fall Damage").withStyle(ChatFormatting.DARK_GREEN));
        if (configType.isFireResistance())
            tooltipComponents.add(Component.literal("+Fire Resistance").withStyle(ChatFormatting.DARK_GREEN));
        if (configType.isWaterBreathing())
            tooltipComponents.add(Component.literal("+Water Breathing").withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    @NotNull
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> vanilla = super.getDefaultAttributeModifiers(slot);

        if (slot == configType.getArmorItemType().getSlot())
        {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            if (BooleanUtils.isTrue(ModCommonConfigs.enableOldArmorRatioSystem.get()))
            {
                // Copy everything EXCEPT vanilla armor/toughness (disables vanilla mitigation for this piece)
                for (var entry : vanilla.entries())
                {
                    Attribute attr = entry.getKey();
                    if (attr == Attributes.ARMOR || attr == Attributes.ARMOR_TOUGHNESS || attr == null || entry.getValue() == null)
                        continue;
                    builder.put(attr, entry.getValue());
                }
            }
            else
            {
                builder.putAll(vanilla);
            }

            builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(speed_uuid[configType.getArmorItemType().getSlot().getIndex()], "Movement Speed", configType.getMoveSpeedModifier() - 1F, AttributeModifier.Operation.MULTIPLY_TOTAL));
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(kb_uuid[configType.getArmorItemType().getSlot().getIndex()], "Knockback Resistance", configType.getKnockbackModifier(), AttributeModifier.Operation.MULTIPLY_TOTAL));
            return builder.build();
        }

        return vanilla;
    }

    public static void handleMobEffects(LivingEntity entity)
    {
        int offset = (entity.getUUID().hashCode() & 0x7fffffff) % EFFECT_CHECK_PERIOD;
        if (((entity.tickCount + offset) % EFFECT_CHECK_PERIOD) != 0)
            return;

        // Early-out: no CustomArmorItem equipped => nothing to do, but we may need to remove previously applied armor effects (instant removal)
        boolean anyCustom = false;
        for (ItemStack armor : entity.getArmorSlots())
        {
            if (armor.getItem() instanceof CustomArmorItem)
            {
                anyCustom = true;
                break;
            }
        }

        // If no custom armor now, remove only those effects we previously applied.
        if (!anyCustom)
        {
            removeNoLongerDesiredArmorEffects(entity, Collections.emptySet());
            return;
        }

        int nv = 0;
        int invis = 0;
        int fire = 0;
        int water = 0;
        int hunger = 0;
        int regen = 0;
        Map<MobEffect, MobEffectInstance> desiredExtra = new HashMap<>();

        for (ItemStack armor : entity.getArmorSlots())
        {
            if (!(armor.getItem() instanceof CustomArmorItem armorItem))
                continue;

            ArmorType armorType = armorItem.configType;

            if (armorType.isNightVision())
                nv++;
            if (armorType.isInvisible())
                invis++;
            if (armorType.isFireResistance())
                fire++;
            if (armorType.isWaterBreathing())
                water++;
            if (armorType.isHunger())
                hunger++;
            if (armorType.isRegeneration())
                regen++;

            // Merge configured effects (best-of)
            Collection<MobEffectInstance> list = armorType.getEffects();
            if (list != null && !list.isEmpty())
            {
                for (MobEffectInstance mobEffectInstance : list)
                {
                    if (mobEffectInstance == null)
                        continue;
                    MobEffect eff = mobEffectInstance.getEffect();
                    MobEffectInstance normalized = new MobEffectInstance(eff, EFFECT_DURATION, mobEffectInstance.getAmplifier(), true, mobEffectInstance.isVisible(), mobEffectInstance.showIcon());
                    mergeBestOf(desiredExtra, normalized);
                }
            }
        }

        Set<MobEffect> desiredNow = new HashSet<>(desiredExtra.keySet());

        if (nv > 0)
            desiredNow.add(MobEffects.NIGHT_VISION);
        if (invis > 0)
            desiredNow.add(MobEffects.INVISIBILITY);
        if (fire > 0)
            desiredNow.add(MobEffects.FIRE_RESISTANCE);
        if (water > 0)
            desiredNow.add(MobEffects.WATER_BREATHING);
        if (hunger > 0)
            desiredNow.add(MobEffects.HUNGER);
        if (regen > 0)
            desiredNow.add(MobEffects.REGENERATION);

        ensureEffectLevel(entity, MobEffects.NIGHT_VISION, nv);
        ensureEffectLevel(entity, MobEffects.INVISIBILITY, invis);
        ensureEffectLevel(entity, MobEffects.FIRE_RESISTANCE, fire);
        ensureEffectLevel(entity, MobEffects.WATER_BREATHING, water);
        ensureEffectLevel(entity, MobEffects.HUNGER, hunger);
        ensureEffectLevel(entity, MobEffects.REGENERATION, regen);

        applyDesiredExtraEffects(entity, desiredExtra, desiredNow);
    }

    /**
     * Best-of merge: choose higher amplifier; if tie, prefer longer duration (mostly irrelevant since we normalize).
     */
    private static void mergeBestOf(Map<MobEffect, MobEffectInstance> out, MobEffectInstance incoming)
    {
        out.merge(incoming.getEffect(), incoming, (cur, inc) -> {
            if (inc.getAmplifier() > cur.getAmplifier())
                return inc;
            if (inc.getAmplifier() < cur.getAmplifier())
                return cur;
            return (inc.getDuration() > cur.getDuration()) ? inc : cur;
        });
    }

    private static void ensureEffectLevel(LivingEntity entity, MobEffect effect, int piecesGivingIt)
    {
        if (piecesGivingIt <= 0)
        {
            MobEffectInstance cur = entity.getEffect(effect);
            if (cur != null && cur.isAmbient())
                entity.removeEffect(effect);
            // Track that we are not applying this built-in effect anymore
            rememberAppliedEffect(entity, effect, false);
            return;
        }

        int amp = piecesGivingIt - 1;

        MobEffectInstance currentEffect = entity.getEffect(effect);
        if (currentEffect == null || currentEffect.getAmplifier() != amp || currentEffect.getDuration() < EFFECT_REFRESH_THRESHOLD)
            entity.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amp, true, false, false));

        rememberAppliedEffect(entity, effect, true);
    }

    /**
     * Applies desired extra effects and removes extra effects we previously applied but are no longer desired.
     */
    private static void applyDesiredExtraEffects(LivingEntity entity, Map<MobEffect, MobEffectInstance> desiredExtra, Set<MobEffect> desiredNow)
    {
        for (MobEffectInstance desiredInst : desiredExtra.values())
        {
            MobEffect eff = desiredInst.getEffect();
            MobEffectInstance cur = entity.getEffect(eff);

            if (cur == null
                || cur.getAmplifier() != desiredInst.getAmplifier()
                || cur.getDuration() < EFFECT_REFRESH_THRESHOLD
                || !cur.isAmbient())
                entity.addEffect(desiredInst);

            rememberAppliedEffect(entity, eff, true);
        }

        // Remove previously applied armor effects that are no longer desired
        removeNoLongerDesiredArmorEffects(entity, desiredNow);
    }

    /**
     * Removes effects that our armor system applied previously but are not desired anymore.
     * Uses "ambient" as a heuristic marker to avoid removing potion/beacon effects.
     */
    private static void removeNoLongerDesiredArmorEffects(LivingEntity entity, Set<MobEffect> desiredNow)
    {
        UUID uuid = entity.getUUID();
        Set<MobEffect> last = LAST_ARMOR_EFFECTS.get(uuid);
        if (last == null || last.isEmpty())
            return;

        // Copy to avoid concurrent modification
        Set<MobEffect> toRemove = new HashSet<>(last);
        toRemove.removeAll(desiredNow);

        for (MobEffect eff : toRemove)
        {
            MobEffectInstance cur = entity.getEffect(eff);
            if (cur != null && cur.isAmbient())
                entity.removeEffect(eff);

            last.remove(eff);
        }

        if (last.isEmpty())
            LAST_ARMOR_EFFECTS.remove(uuid);
    }

    /**
     * Tracks which effects were applied by armor system.
     * We add/remove from LAST_ARMOR_EFFECTS based on whether we're actively applying it.
     */
    private static void rememberAppliedEffect(LivingEntity entity, MobEffect effect, boolean applied)
    {
        UUID uuid = entity.getUUID();
        if (applied) {
            LAST_ARMOR_EFFECTS.computeIfAbsent(uuid, k -> new HashSet<>()).add(effect);
        }
        else
        {
            Set<MobEffect> set = LAST_ARMOR_EFFECTS.get(uuid);
            if (set != null)
            {
                set.remove(effect);
                if (set.isEmpty()) LAST_ARMOR_EFFECTS.remove(uuid);
            }
        }
    }

    public static void handleSpecialEffects(LivingEntity entity)
    {
        boolean waterWalk = false;
        boolean negateFall = false;

        for (ItemStack armor : entity.getArmorSlots()) {
            if (armor.getItem() instanceof CustomArmorItem armorItem)
            {
                waterWalk |= armorItem.configType.isOnWaterWalking();
                negateFall |= armorItem.configType.isNegateFallDamage();
            }
        }

        if (negateFall)
            entity.fallDistance = 0F;

        // If in water and near surface, keep them from sinking
        if (waterWalk && entity.isInWater() && entity.getDeltaMovement().y < 0)
        {
            Vec3 v = entity.getDeltaMovement();
            entity.setDeltaMovement(v.x, 0.08, v.z);
            entity.fallDistance = 0;
        }
    }

    public static void handleJumpModifier(LivingEntity entity)
    {
        float mul = 1F;
        for (ItemStack armor : entity.getArmorSlots())
        {
            if (armor.getItem() instanceof CustomArmorItem armorItem)
                mul *= armorItem.configType.getJumpModifier();
        }

        if (Math.abs(mul - 1F) < 1e-6f)
            return;

        Vec3 v = entity.getDeltaMovement();
        double newY = v.y * mul;
        entity.setDeltaMovement(v.x, newY, v.z);
        entity.hurtMarked = true;
    }

    public static void applyOldArmorRatioSystem(LivingHurtEvent event, LivingEntity entity)
    {
        float incoming = event.getAmount();
        if (incoming <= 0F)
            return;

        double rSum = 0.0;

        for (ItemStack stack : entity.getArmorSlots())
        {
            if (stack.getItem() instanceof CustomArmorItem armorItem)
            {
                double r = armorItem.getConfigType().getDefence();
                if (r > 0.0)
                    rSum += r;
            }
        }

        if (rSum <= 0.0)
            return;

        // Clamp so you never “heal” damage by accident
        rSum = Math.min(1.0, Math.max(0.0, rSum));

        float absorbed = (float) (incoming * rSum);
        float remaining = incoming - absorbed;

        event.setAmount(remaining);
    }

    public static boolean tryApplyIgnoreArmorShot(LivingHurtEvent event, LivingEntity entity, DamageSource source)
    {
        float damage = event.getAmount();
        if (damage <= 0.0F)
            return false;

        ShootableType shootableType = FlanDamageSources.getShootableTypeFromSource(source).orElse(null);
        if (shootableType == null)
            return false;

        // No ignore-armor behavior configured
        if (shootableType.getIgnoreArmorProbability() <= 0.0F)
            return false;

        // Random roll failed → fall back to normal armor handling
        if (entity.getRandom().nextFloat() >= shootableType.getIgnoreArmorProbability())
            return false;

        float originalDamage = damage;

        // Strip absorption hearts first
        float absorption = entity.getAbsorptionAmount();
        damage = Math.max(damage - absorption, 0.0F);
        entity.setAbsorptionAmount(absorption - (originalDamage - damage));

        // Apply ignore-armor damage multiplier
        damage *= shootableType.getIgnoreArmorDamageFactor();

        if (damage > 0.0F)
        {
            float health = entity.getHealth();

            // Directly hurt the entity (armor is 100% bypassed here)
            entity.setHealth(health - damage);

            // Update combat tracker (for death messages, stats, etc.)
            entity.getCombatTracker().recordDamage(source, damage);

            // Optionally adjust absorption again (mirroring the old logic)
            entity.setAbsorptionAmount(entity.getAbsorptionAmount() - damage);
        }

        //  Cancel the event so vanilla damage and your armor scaling don't run
        event.setCanceled(true);
        return true;
    }

    public static void applyArmorBulletDefense(LivingHurtEvent event, LivingEntity entity)
    {
        float totalNormalDef = 0.0F;
        float totalBulletDef = 0.0F;

        // Sum up defences from all 4 armor slots
        for (ItemStack stack : entity.getArmorSlots())
        {
            if (!(stack.getItem() instanceof CustomArmorItem armorItem))
                continue;
            totalNormalDef += (float) armorItem.getConfigType().getDefence();
            totalBulletDef += (float) armorItem.getConfigType().getBulletDefence();
        }

        totalNormalDef = Mth.clamp(totalNormalDef, 0F, 1F);
        totalBulletDef = Mth.clamp(totalBulletDef, 0F, 1F);

        float current = event.getAmount();
        float denom = 1.0F - totalNormalDef;
        float target = 1.0F - totalBulletDef;

        // If denom is 0, normal defence would be "100%". Just fall back to using bulletDef directly.
        float factor = (denom <= 0.0001F) ? target : (target / denom);

        float finalDamage = current * factor;
        if (finalDamage < 0.0F)
            finalDamage = 0.0F;

        event.setAmount(finalDamage);
    }
}