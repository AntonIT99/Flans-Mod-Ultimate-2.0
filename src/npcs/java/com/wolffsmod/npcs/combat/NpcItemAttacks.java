package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

/** Executes equipped items rather than copying NPC projectile/effect settings into synthetic projectiles. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcItemAttacks
{
    // Vanilla's crossbow launch velocity, independent of pi.
    @SuppressWarnings("java:S9133")
    private static final float CROSSBOW_VELOCITY = 3.15F;

    public static boolean melee(EntityNPCInterface npc, Entity target)
    {
        if (npc.level().isClientSide || !target.isAlive())
            return false;
        ItemStack weapon = npc.getMainHandItem();
        float damage = (float) NpcEquipment.meleeDamage(weapon);
        if (target instanceof LivingEntity living)
        {
            damage += EnchantmentHelper.getDamageBonus(weapon, living.getMobType());
            NpcEvent.MeleeAttackEvent event = new NpcEvent.MeleeAttackEvent(npc.wrappedNPC, living, damage);
            if (EventHooks.onNPCAttacksMelee(npc, event))
                return false;
            damage = event.damage;
        }
        if (!target.hurt(npc.damageSources().mobAttack(npc), damage))
            return false;
        applyMeleeEffects(npc, target, weapon);
        return true;
    }

    private static void applyMeleeEffects(EntityNPCInterface npc, Entity target, ItemStack weapon)
    {
        int fire = NpcEquipment.fireSeconds(weapon);
        if (fire > 0)
            target.setSecondsOnFire(fire);
        if (target instanceof LivingEntity living)
        {
            double knockback = EquipmentAttributes.value(weapon, EquipmentSlot.MAINHAND, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK, 0D)
                + EnchantmentHelper.getKnockbackBonus(npc);
            if (knockback > 0D)
                living.knockback(knockback * 0.5D, Math.sin(Math.toRadians(npc.getYRot())), -Math.cos(Math.toRadians(npc.getYRot())));
            EnchantmentHelper.doPostHurtEffects(living, npc);
            EnchantmentHelper.doPostDamageEffects(npc, living);
            weapon.getItem().hurtEnemy(weapon, living, npc);
            npc.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
        npc.setLastHurtMob(target);
    }

    public static boolean ranged(EntityNPCInterface npc, LivingEntity target)
    {
        if (npc.level().isClientSide || target == null || !target.isAlive() || target.level() != npc.level())
            return false;
        ItemStack weapon = npc.getMainHandItem();
        Vec3 aim = target.getEyePosition().subtract(npc.getEyePosition());
        if (FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY).isPresent())
            return FlansEquipment.fire(npc, InteractionHand.MAIN_HAND, npc.getEyePosition(), flanDirection(npc, target, weapon, aim), NpcEquipment.enabled(npc, Feature.FLAN_SOUNDS),
                NpcEquipment.enabled(npc, Feature.SHOOT_PARTICLES));
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

    public static void launched(EntityNPCInterface npc, LivingEntity target)
    {
        npc.updateClient = true;
        EventHooks.onNPCRangedLaunched(npc, new NpcEvent.RangedLaunchedEvent(npc.wrappedNPC, target, NpcEquipment.flan(npc).map(properties -> (float) properties.damage()).orElse(0F)));
    }

    private static Vec3 flanDirection(EntityNPCInterface npc, LivingEntity target, ItemStack weapon, Vec3 direct)
    {
        if (NpcEquipment.flan(npc).map(properties -> properties.speed() <= 0D || properties.throwable()).orElse(true))
            return direct;
        var shot = FlansEquipment.getLoadedAmmunition(weapon)
            .flatMap(ammo -> FlansProjectiles.prepare(npc, ammo, weapon, null, null, false, new ProjectileParameters(FlansEquipment.getLoadedRoundIndex(weapon), true, 0F, 0F, 1F)));
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
        if (shot.launch(npc.getEyePosition(), direction, NpcEquipment.enabled(npc, Feature.FLAN_SOUNDS)).isEmpty())
            return false;
        weapon.shrink(1);
        npc.setItemSlot(EquipmentSlot.MAINHAND, weapon.isEmpty() ? ItemStack.EMPTY : weapon);
        return true;
    }

    public static boolean loadFlanGun(EntityNPCInterface npc)
    {
        // NPC ammunition is infinite: load from a detached copy large enough to fill every gun slot, never from the projectile slot itself.
        ItemStack spare = ItemStackWrapper.MCItem(npc.inventory.getProjectile()).copyWithCount(Integer.MAX_VALUE);
        return FlansEquipment.loadMagazine(npc, npc.getMainHandItem(), spare);
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
        shoot(arrow, npc, target, 3F, 1F, 0.05D);
        boolean infinite = ammo.is(Items.ARROW) && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, weapon) > 0;
        arrow.pickup = infinite ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
        if (!npc.level().addFreshEntity(arrow))
            return false;
        if (!infinite)
            ammo.shrink(1);
        weapon.hurtAndBreak(1, npc, holder -> holder.broadcastBreakEvent(InteractionHand.MAIN_HAND));
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
        float speed = rocket ? 1.6F : CROSSBOW_VELOCITY;
        Vec3 direction = ProjectileAim.direction(target.getEyePosition().subtract(npc.getEyePosition()), NpcEquipment.enabled(npc, Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO, speed,
            !rocket && NpcEquipment.enabled(npc, Feature.BALLISTIC_AIM) ? 0.05D : 0D, rocket ? 1D : 0.99D, false);
        npc.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        npc.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        CrossbowItem.performShooting(npc.level(), npc, InteractionHand.MAIN_HAND, weapon, speed, 1F);
        CrossbowItem.setCharged(weapon, false);
        return true;
    }

    private static boolean throwable(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        boolean trident = weapon.getItem() instanceof TridentItem;
        if (trident && EnchantmentHelper.getRiptide(weapon) > 0)
            return false;
        if (trident)
            weapon.hurtAndBreak(1, npc, holder -> holder.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        Projectile projectile = projectile(npc, weapon);
        if (projectile == null)
            return false;
        float velocity = throwableVelocity(weapon);
        shoot(projectile, npc, target, velocity, 1F, trident || weapon.getItem() instanceof ThrowablePotionItem ? 0.05D : 0.03D);
        if (!npc.level().addFreshEntity(projectile))
            return false;
        weapon.shrink(1);
        npc.setItemSlot(EquipmentSlot.MAINHAND, weapon.isEmpty() ? ItemStack.EMPTY : weapon);
        npc.playSound(trident ? SoundEvents.TRIDENT_THROW : SoundEvents.SNOWBALL_THROW, 1F, 1F);
        return true;
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
