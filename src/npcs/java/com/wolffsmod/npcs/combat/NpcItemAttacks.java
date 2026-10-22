package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.flansmodultimate.platform.PlatformSounds;
import com.flansmodultimate.platform.item.ItemStackData;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.NpcPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.EventHooks;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Executes equipped items rather than copying NPC projectile/effect settings into synthetic projectiles. The statistics the
 * NPC keeps its own value for, chosen in Main hand and Off hand properties, replace the item's while it attacks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcItemAttacks
{
    // Vanilla's crossbow launch velocity, independent of pi.
    @SuppressWarnings("java:S9133")
    public static final float CROSSBOW_VELOCITY = 3.15F;

    /**
     * Strikes with the selected hand, alternating only when melee dual wield is enabled. Each custom melee statistic comes from the NPC instead:
     * damage, knockback, and its effect in place of the item's enchantment and hit effects.
     */
    public static boolean melee(EntityNPCInterface npc, Entity target)
    {
        if (npc.level().isClientSide || !target.isAlive())
            return false;
        InteractionHand hand = NpcEquipment.meleeHand(npc);
        ItemStack weapon = hand == InteractionHand.OFF_HAND ? npc.getOffhandItem() : npc.getMainHandItem();
        NpcMeleeState state = NpcEquipment.enabled(npc, Feature.MELEE_DUAL_WIELD) ? ((NpcMeleeState.Holder) npc).wolffsmodnpcsMeleeState() : null;
        if (state != null)
            state.begin(hand);
        try
        {
            return melee(npc, target, weapon, hand);
        }
        finally
        {
            if (state != null)
                state.finish(hand);
        }
    }

    private static boolean melee(EntityNPCInterface npc, Entity target, ItemStack weapon, InteractionHand hand)
    {
        boolean itemDamage = NpcEquipment.inherits(npc, EquipmentProperty.MELEE_DAMAGE);
        float damage = itemDamage ? (float) NpcEquipment.meleeDamage(weapon) : npc.stats.melee.getStrength();
        if (target instanceof LivingEntity living)
        {
            if (itemDamage)
                damage += EnchantmentHelper.getDamageBonus(weapon, living.getMobType());
            NpcEvent.MeleeAttackEvent event = new NpcEvent.MeleeAttackEvent(npc.wrappedNPC, living, damage);
            if (EventHooks.onNPCAttacksMelee(npc, event))
                return false;
            damage = event.damage;
        }
        if (!hurtMelee(npc, target, weapon, damage))
            return false;
        applyMeleeEffects(npc, target, weapon, hand);
        return true;
    }

    /** Matches Custom NPCs' rapid melee policy using the cadence the attack AI actually chose. */
    private static boolean hurtMelee(EntityNPCInterface npc, Entity target, ItemStack weapon, float damage)
    {
        int delay = NpcEquipment.itemMelee(npc, EquipmentProperty.MELEE_DELAY) ? NpcEquipment.meleeDelay(weapon) : npc.stats.melee.getDelay();
        NpcMeleeState state = NpcEquipment.enabled(npc, Feature.MELEE_DUAL_WIELD) ? ((NpcMeleeState.Holder) npc).wolffsmodnpcsMeleeState() : null;
        long tick = npc.level().getGameTime();
        // The previous hand's recovery controls when this strike arrives, even if this weapon itself is slower.
        boolean rapid = delay < 10 || state != null && state.followsRapidHit(target.getId(), tick);
        int previousImmunity = target.invulnerableTime;
        if (rapid)
            target.invulnerableTime = 0;
        boolean accepted = false;
        try
        {
            accepted = target.hurt(npc.damageSources().mobAttack(npc), damage);
            if (accepted && state != null)
                state.hit(target.getId(), tick, delay);
            return accepted;
        }
        finally
        {
            // A shield, damage hook or other rejection must not expose the victim to another attacker.
            if (rapid && !accepted)
                target.invulnerableTime = previousImmunity;
        }
    }

    private static void applyMeleeEffects(EntityNPCInterface npc, Entity target, ItemStack weapon, InteractionHand hand)
    {
        boolean itemEffect = NpcEquipment.inherits(npc, EquipmentProperty.MELEE_EFFECT);
        int fire = NpcEquipment.fireSeconds(weapon, npc);
        if (itemEffect && fire > 0)
            target.setSecondsOnFire(fire);
        if (target instanceof LivingEntity living)
        {
            if (NpcEquipment.inherits(npc, EquipmentProperty.MELEE_KNOCKBACK))
            {
                double knockback = EquipmentAttributes.value(weapon, EquipmentSlot.MAINHAND, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK, 0D)
                    + NpcPlatform.meleeKnockbackBonus(npc, weapon);
                if (knockback > 0D)
                    living.knockback(knockback * 0.5D, Math.sin(Math.toRadians(npc.getYRot())), -Math.cos(Math.toRadians(npc.getYRot())));
            }
            else
                npcKnockback(npc, living);
            if (itemEffect)
                meleeEnchantmentEffects(npc, living, weapon);
            else
                npcEffect(npc, living);
            weapon.getItem().hurtEnemy(weapon, living, npc);
            npc.setItemSlot(hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND, weapon);
        }
        npc.setLastHurtMob(target);
    }

    private static void meleeEnchantmentEffects(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        EnchantmentHelper.doPostHurtEffects(target, npc);
        if (NpcEquipment.enabled(npc, Feature.MELEE_DUAL_WIELD))
            NpcPlatform.meleePostDamageEffects(npc, target, weapon);
        else
            EnchantmentHelper.doPostDamageEffects(npc, target);
    }

    /** The push Custom NPCs gives a target with the NPC's own melee knockback. */
    private static void npcKnockback(EntityNPCInterface npc, LivingEntity target)
    {
        int knockback = npc.stats.melee.getKnockback();
        if (knockback == 0)
            return;
        double yaw = Math.toRadians(npc.getYRot());
        target.push(-Math.sin(yaw) * knockback * 0.5D, 0.1D + knockback * 0.04D, Math.cos(yaw) * knockback * 0.5D);
        npc.setDeltaMovement(npc.getDeltaMovement().multiply(0.6D, 1D, 0.6D));
    }

    /** The effect Custom NPCs applies with the NPC's own melee stats: a potion effect, or fire. */
    private static void npcEffect(EntityNPCInterface npc, LivingEntity target)
    {
        int type = npc.stats.melee.getEffectType();
        int seconds = npc.stats.melee.getEffectTime();
        if (type == 0)
            return;
        if (type == NpcEquipmentImport.FIRE_EFFECT)
        {
            target.setSecondsOnFire(seconds);
            return;
        }
        MobEffect effect = PotionEffectType.getMCType(type);
        if (effect != null)
            target.addEffect(new MobEffectInstance(effect, seconds * 20, npc.stats.melee.getEffectStrength()));
    }

    public static boolean ranged(EntityNPCInterface npc, LivingEntity target)
    {
        if (npc.level().isClientSide || target == null || !target.isAlive() || target.level() != npc.level())
            return false;
        ItemStack weapon = NpcEquipment.rangedWeapon(npc);
        boolean fired = ranged(npc, target, weapon);
        if (fired)
            NpcAmmunition.refillEmptySlots(npc);
        return fired;
    }

    private static boolean ranged(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        if (weapon == npc.getOffhandItem() && weapon != npc.getMainHandItem())
            return fireFlan(npc, target, InteractionHand.OFF_HAND);
        if (weapon != npc.getMainHandItem())
        {
            boolean thrown = FlansProjectiles.isGrenade(weapon) ? grenade(npc, target, weapon) : throwFlan(npc, target, weapon);
            if (thrown && weapon.isEmpty())
                npc.inventory.weapons.remove(1);
            return thrown;
        }
        if (FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY).isPresent())
            return fireFlan(npc, target, InteractionHand.MAIN_HAND);
        if (FlansProjectiles.isGrenade(weapon))
            return grenade(npc, target, weapon);
        if (weapon.getItem() instanceof NpcWeaponAdapter adapter)
            return adapter.fireNpcWeapon(npc, target, weapon);
        if (weapon.getItem() instanceof BowItem bow)
            return bow(npc, target, weapon, bow);
        if (weapon.getItem() instanceof CrossbowItem crossbow)
            return crossbow(npc, target, weapon, crossbow);
        return throwable(npc, target, weapon);
    }

    /** Fires the Flan weapon held in either hand with its own ammunition, attachments and sounds. */
    public static boolean fireFlan(EntityNPCInterface npc, LivingEntity target, InteractionHand hand)
    {
        if (npc.level().isClientSide || target == null || !target.isAlive() || target.level() != npc.level())
            return false;
        ItemStack weapon = npc.getItemInHand(hand);
        var flanWeapon = FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY);
        if (flanWeapon.isEmpty())
            return false;
        if (flanWeapon.get().throwable())
        {
            boolean thrown = throwFlan(npc, target, weapon);
            if (thrown)
                npc.setItemInHand(hand, weapon.isEmpty() ? ItemStack.EMPTY : weapon);
            return thrown;
        }
        Vec3 aim = target.getEyePosition().subtract(npc.getEyePosition());
        boolean itemSound = NpcEquipment.inherits(npc, EquipmentProperty.FIRING_SOUND.inHand(hand));
        boolean fired = FlansEquipment.fire(npc, hand, npc.getEyePosition(), flanDirection(npc, target, hand, weapon, aim), itemSound && NpcEquipment.enabled(npc, Feature.FIRE_SOUNDS),
            NpcEquipment.enabled(npc, Feature.SHOOT_PARTICLES), NpcEquipment.rangedOverrides(npc, hand));
        if (fired && !itemSound)
            playOwnFiringSound(npc);
        return fired;
    }

    /** The NPC's own firing sound, played in place of the item's when the NPC keeps its own. */
    private static void playOwnFiringSound(EntityNPCInterface npc)
    {
        SoundEvent sound = npc.stats.ranged.getSoundEvent(0);
        if (sound != null)
            npc.playSound(sound, 2F, 1F);
    }

    private static boolean throwFlan(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        var prepared = FlansEquipment.prepareThrow(npc, weapon);
        if (prepared.isEmpty())
            return false;
        var shot = prepared.get();
        Vec3 direction = ProjectileAim.direction(target.getEyePosition().subtract(npc.getEyePosition()), NpcEquipment.enabled(npc, Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO,
            shot.speed(), NpcEquipment.enabled(npc, Feature.BALLISTIC_AIM) ? shot.gravity() : 0D, shot.drag(), false);
        var launched = shot.launch(npc.getEyePosition(), direction, NpcEquipment.enabled(npc, Feature.THROW_SOUNDS));
        if (launched.isEmpty())
            return false;
        finishThrow(npc, weapon, launched);
        return true;
    }

    private static void finishThrow(EntityNPCInterface npc, ItemStack weapon, List<Entity> launched)
    {
        if (NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            weapon.shrink(1);
        else
            for (Entity entity : launched)
                if (entity instanceof AbstractArrow arrow)
                    arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        npc.updateClient = true;
        NpcThrowPresentation.clear(npc);
    }

    /** A held Flan gun swung in melee plays its melee animation and sound; other items have nothing to add. */
    public static void meleeFeedback(EntityNPCInterface npc)
    {
        if (npc.level().isClientSide)
            return;
        InteractionHand hand = NpcEquipment.meleeHand(npc);
        ItemStack weapon = hand == InteractionHand.OFF_HAND ? npc.getOffhandItem() : npc.getMainHandItem();
        if (FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY).isEmpty())
            return;
        if (NpcEquipment.enabled(npc, Feature.GUN_MELEE_ANIMATION))
            FlansEquipment.animateMelee(npc, hand);
        if (NpcEquipment.enabled(npc, Feature.MELEE_SOUNDS))
            FlansEquipment.playMeleeSound(npc, hand);
    }

    public static void launched(EntityNPCInterface npc, LivingEntity target, InteractionHand hand)
    {
        npc.updateClient = true;
        float damage = NpcEquipment.inherits(npc, EquipmentProperty.RANGED_DAMAGE.inHand(hand))
            ? NpcEquipment.flan(npc, hand).map(properties -> (float) properties.damage()).orElse(0F)
            : npc.stats.ranged.getStrength();
        EventHooks.onNPCRangedLaunched(npc, new NpcEvent.RangedLaunchedEvent(npc.wrappedNPC, target, damage));
    }

    private static Vec3 flanDirection(EntityNPCInterface npc, LivingEntity target, InteractionHand hand, ItemStack weapon, Vec3 direct)
    {
        boolean ownSpeed = !NpcEquipment.inherits(npc, EquipmentProperty.RANGED_SPEED.inHand(hand));
        if (NpcEquipment.flan(npc, hand).map(properties -> !ownSpeed && properties.speed() <= 0D || properties.throwable()).orElse(true))
            return direct;
        // The NPC's own speed stands in for the gun's; ammunition that declares a velocity still wins, as when it fires.
        ProjectileParameters parameters = new ProjectileParameters(FlansEquipment.getLoadedRoundIndex(weapon), !ownSpeed, 0F, 0F,
            ownSpeed ? (float) NpcEquipment.speed(npc.stats.ranged.getSpeed()) : 1F);
        var shot = FlansEquipment.getLoadedAmmunition(weapon).flatMap(ammo -> FlansProjectiles.prepare(npc, ammo, weapon, null, null, false, parameters));
        return shot.map(value -> ProjectileAim.direction(direct, NpcEquipment.enabled(npc, Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO, value.speed(),
            NpcEquipment.enabled(npc, Feature.BALLISTIC_AIM) ? value.gravity() : 0D, value.drag(), false)).orElse(direct);
    }

    private static boolean grenade(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        if (!NpcEquipment.enabled(npc, Feature.GRENADES))
            return false;
        var prepared = FlansProjectiles.prepare(npc, weapon, ItemStack.EMPTY, null, null, false, new ProjectileParameters(0, true, 0F, 0F, 1F));
        if (prepared.isEmpty())
            return false;
        var shot = prepared.get();
        Vec3 direction = ProjectileAim.direction(target.getEyePosition().subtract(npc.getEyePosition()), NpcEquipment.enabled(npc, Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO,
            shot.speed(), NpcEquipment.enabled(npc, Feature.BALLISTIC_AIM) ? shot.gravity() : 0D, shot.drag(), false);
        var launched = shot.launch(npc.getEyePosition(), direction, NpcEquipment.enabled(npc, Feature.THROW_SOUNDS));
        if (launched.isEmpty())
            return false;
        finishThrow(npc, weapon, launched);
        if (weapon == npc.getMainHandItem())
            npc.setItemSlot(EquipmentSlot.MAINHAND, weapon.isEmpty() ? ItemStack.EMPTY : weapon);
        return true;
    }

    public static boolean loadFlanGun(EntityNPCInterface npc, InteractionHand hand)
    {
        if (NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return NpcAmmunition.load(npc, hand);
        // NPC ammunition is infinite: load from a detached copy large enough to fill every gun slot, never from the projectile slot itself.
        ItemStack weapon = npc.getItemInHand(hand);
        return NpcAmmunition.infiniteTemplate(npc, weapon).map(template -> FlansEquipment.loadMagazine(npc, weapon, template.copyWithCount(Integer.MAX_VALUE))).orElse(false);
    }

    private static boolean bow(EntityNPCInterface npc, LivingEntity target, ItemStack weapon, BowItem bow)
    {
        ItemStack ammo = ItemStackWrapper.MCItem(npc.inventory.getProjectile());
        if (!(ammo.getItem() instanceof ArrowItem arrowItem) || !bow.getAllSupportedProjectiles().test(ammo))
            return false;
        AbstractArrow arrow = bow.customArrow(arrowItem.createArrow(npc.level(), ammo, npc));
        int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, weapon);
        if (power > 0)
            arrow.setBaseDamage(arrow.getBaseDamage() + power * 0.5D + 0.5D);
        arrow.setKnockback(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, weapon));
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, weapon) > 0)
            arrow.setSecondsOnFire(100);
        arrow.setCritArrow(true);
        float speed = speed(npc, 3F);
        // An arrow deals its base damage times its speed, so the NPC's own damage is spread over the speed it leaves at.
        if (!NpcEquipment.inherits(npc, EquipmentProperty.RANGED_DAMAGE))
            arrow.setBaseDamage(npc.stats.ranged.getStrength() / speed);
        shoot(arrow, npc, target, speed, inaccuracy(npc), 0.05D);
        boolean infinite = ammo.is(Items.ARROW) && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, weapon) > 0;
        arrow.pickup = infinite ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
        if (!npc.level().addFreshEntity(arrow))
            return false;
        if (!infinite)
            ammo.shrink(1);
        ItemStackData.hurtAndBreak(weapon, 1, npc, EquipmentSlot.MAINHAND);
        npc.playSound(SoundEvents.ARROW_SHOOT, 1F, 1F);
        return true;
    }

    private static boolean crossbow(EntityNPCInterface npc, LivingEntity target, ItemStack weapon, CrossbowItem crossbow)
    {
        if (!CrossbowItem.isCharged(weapon))
        {
            ItemStack ammo = ItemStackWrapper.MCItem(npc.inventory.getProjectile());
            if (ammo.isEmpty() || !crossbow.getAllSupportedProjectiles().test(ammo))
                return false;
            ListTag projectiles = new ListTag();
            int count = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, weapon) > 0 ? 3 : 1;
            for (int i = 0; i < count; i++)
                projectiles.add(ammo.copyWithCount(1).save(new CompoundTag()));
            weapon.getOrCreateTag().put("ChargedProjectiles", projectiles);
            ammo.shrink(1);
            CrossbowItem.setCharged(weapon, true);
        }
        // Vanilla crossbows accept living shooters and preserve charged ammunition, piercing and multishot.
        boolean rocket = CrossbowItem.containsChargedProjectile(weapon, Items.FIREWORK_ROCKET);
        float speed = speed(npc, rocket ? 1.6F : CROSSBOW_VELOCITY);
        Vec3 direction = ProjectileAim.direction(target.getEyePosition().subtract(npc.getEyePosition()), NpcEquipment.enabled(npc, Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO, speed,
            !rocket && NpcEquipment.enabled(npc, Feature.BALLISTIC_AIM) ? 0.05D : 0D, rocket ? 1D : 0.99D, false);
        npc.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        npc.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        CrossbowItem.performShooting(npc.level(), npc, InteractionHand.MAIN_HAND, weapon, speed, inaccuracy(npc));
        CrossbowItem.setCharged(weapon, false);
        return true;
    }

    private static boolean throwable(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        boolean trident = weapon.getItem() instanceof TridentItem;
        if (trident && EnchantmentHelper.getRiptide(weapon) > 0)
            return false;
        if (trident)
            ItemStackData.hurtAndBreak(weapon, 1, npc, EquipmentSlot.MAINHAND);
        Projectile projectile = projectile(npc, weapon);
        if (projectile == null)
            return false;
        float velocity = speed(npc, throwableVelocity(weapon));
        shoot(projectile, npc, target, velocity, inaccuracy(npc), trident || weapon.getItem() instanceof ThrowablePotionItem ? 0.05D : 0.03D);
        if (!npc.level().addFreshEntity(projectile))
            return false;
        weapon.shrink(1);
        npc.setItemSlot(EquipmentSlot.MAINHAND, weapon.isEmpty() ? ItemStack.EMPTY : weapon);
        npc.playSound(trident ? PlatformSounds.tridentThrow() : SoundEvents.SNOWBALL_THROW, 1F, 1F);
        return true;
    }

    /** The item's launch speed in blocks per tick, or the NPC's own speed when it keeps that. */
    private static float speed(EntityNPCInterface npc, float item)
    {
        return NpcEquipment.inherits(npc, EquipmentProperty.RANGED_SPEED) ? item : (float) NpcEquipment.speed(npc.stats.ranged.getSpeed());
    }

    /** Vanilla inaccuracy 1, which items shoot with, or the NPC's own accuracy expressed the same way. */
    private static float inaccuracy(EntityNPCInterface npc)
    {
        if (NpcEquipment.inherits(npc, EquipmentProperty.RANGED_ACCURACY))
            return 1F;
        return (float) (NpcEquipment.spread(npc.stats.ranged.getAccuracy()) / NpcEquipmentStats.VANILLA_INACCURACY_SPREAD);
    }

    private static float throwableVelocity(ItemStack weapon)
    {
        if (weapon.getItem() instanceof TridentItem)
            return 2.5F;
        return weapon.getItem() instanceof ThrowablePotionItem ? 0.5F : 1.5F;
    }

    @org.jetbrains.annotations.Nullable
    private static Projectile projectile(EntityNPCInterface npc, ItemStack weapon)
    {
        if (weapon.getItem() instanceof TridentItem)
        {
            ThrownTrident trident = new ThrownTrident(npc.level(), npc, weapon.copyWithCount(1));
            trident.pickup = AbstractArrow.Pickup.ALLOWED;
            return trident;
        }
        if (weapon.getItem() instanceof ThrowablePotionItem)
        {
            ThrownPotion potion = new ThrownPotion(npc.level(), npc);
            potion.setItem(weapon.copyWithCount(1));
            return potion;
        }
        if (weapon.is(Items.SNOWBALL))
            return new Snowball(npc.level(), npc);
        if (weapon.is(Items.EGG))
            return new ThrownEgg(npc.level(), npc);
        return weapon.is(Items.ENDER_PEARL) ? new ThrownEnderpearl(npc.level(), npc) : null;
    }

    private static void shoot(Projectile projectile, EntityNPCInterface npc, LivingEntity target, float speed, float spread, double gravity)
    {
        Vec3 direction = ProjectileAim.direction(target.getEyePosition().subtract(npc.getEyePosition()), NpcEquipment.enabled(npc, Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO, speed,
            NpcEquipment.enabled(npc, Feature.BALLISTIC_AIM) ? gravity : 0D, 0.99D, false);
        projectile.shoot(direction.x, direction.y, direction.z, speed, spread);
    }
}
