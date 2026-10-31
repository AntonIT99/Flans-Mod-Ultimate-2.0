package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.InfoType;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;

/** Melee rules shared by hulls, proxies, protected riders and NPC model targets. */
public final class VehicleMeleeDamage
{
    private static final Set<String> MELEE_IDS = Set.of("player", "mob", "npc", "melee");

    private VehicleMeleeDamage()
    {}

    public static boolean isMelee(DamageSource source)
    {
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION))
            return false;
        return source.is(FlanDamageSources.MELEE) || MELEE_IDS.contains(source.getMsgId()) || source.getDirectEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity();
    }

    public static float againstVehicle(DamageSource source, float amount)
    {
        if (!isMelee(source))
            return amount;
        ItemStack weapon = ItemStack.EMPTY;
        if (source instanceof WeaponSource hit)
            weapon = hit.weapon;
        else if (source.getDirectEntity() instanceof LivingEntity attacker)
            weapon = attacker.getMainHandItem();
        if (!(weapon.getItem() instanceof GunItem gun))
            return amount;
        float ordinary = gun.getConfigType().getMeleeDamage(weapon, false);
        float vehicle = gun.getConfigType().getMeleeDamage(weapon, true);
        if (ordinary > 0F)
            return amount * vehicle / ordinary;
        return amount > 0F ? Math.max(0F, vehicle) : 0F;
    }

    /** A threshold of exactly zero accepts all hits; the threshold is not subtracted from accepted damage. */
    public static boolean belowThreshold(float threshold, float amount)
    {
        return threshold > 0F && amount < threshold;
    }

    public static DamageSource source(LivingEntity attacker, ItemStack weapon)
    {
        return new WeaponSource(FlanDamageSources.createDamageSource(attacker.level(), attacker, FlanDamageSources.MELEE), weapon);
    }

    public static DamageSource source(DamageSource original, ItemStack weapon)
    {
        return weapon.getItem() instanceof GunItem ? new WeaponSource(original, weapon) : original;
    }

    /** The striking weapon, including an offhand weapon, retained when the melee source was composed. */
    public static Optional<InfoType> weaponType(DamageSource source)
    {
        return source instanceof WeaponSource hit && hit.weapon.getItem() instanceof IFlanItem<?> item ? Optional.of(item.getConfigType()) : Optional.empty();
    }

    public static boolean protectRider(LivingEntity rider, DamageSource source, float amount)
    {
        Entity vehicle = rider.getVehicle();
        if (!(vehicle instanceof Driveable) && !(vehicle instanceof Seat))
            return false;
        if (!rider.level().isClientSide && isMelee(source) && source.getEntity() != rider && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            vehicle.hurt(source, amount);
        return true;
    }

    private static final class WeaponSource extends DamageSource
    {
        private final DamageSource original;
        private final ItemStack weapon;

        private WeaponSource(DamageSource original, ItemStack weapon)
        {
            super(original.typeHolder(), original.getDirectEntity(), original.getEntity());
            this.original = original;
            this.weapon = weapon.copy();
        }

        @Override
        public @NotNull Component getLocalizedDeathMessage(@NotNull LivingEntity killed)
        {
            return FlanDamageSources.deathMessage(original, killed, weapon.getHoverName());
        }
    }
}
