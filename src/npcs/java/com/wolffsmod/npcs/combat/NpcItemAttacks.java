package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

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
            damage = EnchantmentHelper.modifyDamage((ServerLevel) npc.level(), weapon, living, npc.damageSources().mobAttack(npc), damage);
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
        DamageSource source = npc.damageSources().mobAttack(npc);
        EnchantmentHelper.doPostAttackEffectsWithItemSource((ServerLevel) npc.level(), target, source, weapon);
        if (target instanceof LivingEntity living)
        {
            double knockback = EnchantmentHelper.modifyKnockback((ServerLevel) npc.level(), weapon, target, source,
                (float) EquipmentAttributes.value(weapon, EquipmentSlot.MAINHAND, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK, 0D));
            if (knockback > 0D)
                living.knockback(knockback * 0.5D, Math.sin(Math.toRadians(npc.getYRot())), -Math.cos(Math.toRadians(npc.getYRot())));
            // Modern weapons split successful-hit handling from durability loss.
            if (weapon.getItem().hurtEnemy(weapon, living, npc) && !weapon.isEmpty())
                weapon.getItem().postHurtEnemy(weapon, living, npc);
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
        int usedAmmo = arrowItem.isInfinite(ammo, weapon, npc) ? 0 : EnchantmentHelper.processAmmoUse((ServerLevel) npc.level(), weapon, ammo, 1);
        if (usedAmmo > ammo.getCount())
            return false;
        ItemStack projectileAmmo = ammo.copyWithCount(1);
        if (usedAmmo == 0)
            projectileAmmo.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
        // Modern arrows retain the firing weapon and apply its enchantment effects natively.
        AbstractArrow arrow = bow.customArrow(arrowItem.createArrow(npc.level(), projectileAmmo, npc, weapon), projectileAmmo, weapon);
        arrow.setCritArrow(true);
        shoot(arrow, npc, target, 3F, 1F, 0.05D);
        arrow.pickup = usedAmmo == 0 ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
        if (!npc.level().addFreshEntity(arrow))
            return false;
        ammo.shrink(usedAmmo);
        weapon.hurtAndBreak(1, npc, EquipmentSlot.MAINHAND);
        npc.playSound(SoundEvents.ARROW_SHOOT, 1F, 1F);
        return true;
    }

    private static boolean crossbow(EntityNPCInterface npc, LivingEntity target, ItemStack weapon, CrossbowItem crossbow)
    {
        if (!CrossbowItem.isCharged(weapon) && !chargeCrossbow(npc, weapon, crossbow))
            return false;
        // Vanilla crossbows accept living shooters and preserve charged ammunition, piercing and multishot.
        boolean rocket = weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).contains(Items.FIREWORK_ROCKET);
        float speed = rocket ? 1.6F : CROSSBOW_VELOCITY;
        Vec3 direction = ProjectileAim.direction(target.getEyePosition().subtract(npc.getEyePosition()), NpcEquipment.enabled(npc, Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO, speed,
            !rocket && NpcEquipment.enabled(npc, Feature.BALLISTIC_AIM) ? 0.05D : 0D, rocket ? 1D : 0.99D, false);
        npc.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        npc.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        crossbow.performShooting(npc.level(), npc, InteractionHand.MAIN_HAND, weapon, speed, 1F, null);
        return true;
    }

    private static boolean chargeCrossbow(EntityNPCInterface npc, ItemStack weapon, CrossbowItem crossbow)
    {
        ItemStack ammo = ItemStackWrapper.MCItem(npc.inventory.getProjectile());
        if (ammo.isEmpty() || !crossbow.getAllSupportedProjectiles().test(ammo))
            return false;
        int usedAmmo = EnchantmentHelper.processAmmoUse((ServerLevel) npc.level(), weapon, ammo, 1);
        if (usedAmmo > ammo.getCount())
            return false;
        List<ItemStack> projectiles = new ArrayList<>();
        int count = EnchantmentHelper.processProjectileCount((ServerLevel) npc.level(), weapon, npc, 1);
        for (int i = 0; i < count; i++)
        {
            ItemStack projectileAmmo = ammo.copyWithCount(1);
            if (usedAmmo == 0 || i > 0)
                projectileAmmo.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            projectiles.add(projectileAmmo);
        }
        weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(projectiles));
        ammo.shrink(usedAmmo);
        return true;
    }

    private static boolean throwable(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        boolean trident = weapon.getItem() instanceof TridentItem;
        if (trident && EnchantmentHelper.getTridentSpinAttackStrength(weapon, npc) > 0)
            return false;
        if (trident)
            weapon.hurtAndBreak(1, npc, EquipmentSlot.MAINHAND);
        Projectile projectile = projectile(npc, weapon);
        if (projectile == null)
            return false;
        float velocity = throwableVelocity(weapon);
        shoot(projectile, npc, target, velocity, 1F, trident || weapon.getItem() instanceof ThrowablePotionItem ? 0.05D : 0.03D);
        if (!npc.level().addFreshEntity(projectile))
            return false;
        weapon.shrink(1);
        npc.setItemSlot(EquipmentSlot.MAINHAND, weapon.isEmpty() ? ItemStack.EMPTY : weapon);
        npc.playSound(trident ? SoundEvents.TRIDENT_THROW.value() : SoundEvents.SNOWBALL_THROW, 1F, 1F);
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
