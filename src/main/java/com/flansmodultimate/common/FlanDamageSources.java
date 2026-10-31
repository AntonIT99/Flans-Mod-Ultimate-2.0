package com.flansmodultimate.common;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.parts.IFlanDamageModel;
import com.flansmodultimate.common.driveables.damage.VehicleMeleeDamage;
import com.flansmodultimate.common.entity.*;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanDamageSources
{
    public static final ResourceKey<DamageType> MELEE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "melee"));
    public static final ResourceKey<DamageType> SHOOTABLE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "shootable"));
    public static final ResourceKey<DamageType> HEADSHOT = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "headshot"));
    public static final ResourceKey<DamageType> EXPLOSION = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "explosion"));

    public static DamageSource createDamageSource(Level level, @Nullable Entity directAttacker, @Nullable Entity indirectAttacker, ResourceKey<DamageType> damageType)
    {
        return createDamageSource(level, directAttacker, indirectAttacker, damageType, null);
    }

    /** Carries the weapon at firing time, including hitscan shots with no projectile entity. */
    public static DamageSource createDamageSource(Level level, @Nullable Entity directAttacker, @Nullable Entity indirectAttacker, ResourceKey<DamageType> damageType, @Nullable InfoType weapon)
    {
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageType);
        return new FlanDamageSource(holder, directAttacker, indirectAttacker, weapon);
    }

    /**
     * A Flan's damage source. Its death message adds how far away the killer was, as 1.7.10 did, unless
     * kill messages or the distance are switched off in the common config.
     */
    private static final class FlanDamageSource extends DamageSource
    {
        @Nullable
        private final InfoType weapon;

        private FlanDamageSource(Holder<DamageType> type, @Nullable Entity directAttacker, @Nullable Entity indirectAttacker, @Nullable InfoType weapon)
        {
            super(type, directAttacker, indirectAttacker);
            this.weapon = weapon;
        }

        @Override
        @NotNull
        public Component getLocalizedDeathMessage(@NotNull LivingEntity killed)
        {
            return deathMessage(this, killed, getWeaponType(this).map(InfoType::getNameComponent).orElse(null));
        }
    }

    /** Adds the actual killing weapon and optional distance, independently of the attacker's current held item. */
    public static Component deathMessage(DamageSource source, LivingEntity killed, @Nullable Component weapon)
    {
        Component message = new DamageSource(source.typeHolder(), source.getDirectEntity(), source.getEntity()).getLocalizedDeathMessage(killed);
        if (!ModCommonConfig.enableKillMessages())
            return message;
        Entity attacker = source.getEntity() == null ? source.getDirectEntity() : source.getEntity();
        boolean flanMessage = isShootableDamage(source) || source.is(MELEE) || source.is(EXPLOSION);
        if (attacker != null && (flanMessage || weapon != null))
            message = Component.translatable("death.attack." + source.getMsgId() + (flanMessage ? ".player" : ""), killed.getDisplayName(), attacker.getDisplayName());
        if (weapon != null)
            message = Component.translatable("death.flansmodultimate.weapon", message, weapon);
        Entity killer = source.getEntity();
        if (killer == null || killer == killed || !ModCommonConfig.showDistanceInKillMessage())
            return message;
        String distance = String.format(Locale.ROOT, "%.1f", killed.distanceTo(killer));
        return Component.translatable("death.flansmodultimate.distance", message, distance);
    }

    public static DamageSource createDamageSource(Level level, @Nullable Entity attacker, ResourceKey<DamageType> damageType)
    {
        return createDamageSource(level, attacker, attacker, damageType);
    }

    /** Retains ordinary player, mob and unmanned-impact damage rules while naming the vehicle that struck. */
    public static DamageSource vehicleImpact(Entity vehicle, @Nullable Entity controller)
    {
        DamageSource original;
        if (controller instanceof Player player)
            original = vehicle.level().damageSources().playerAttack(player);
        else if (controller instanceof LivingEntity living)
            original = vehicle.level().damageSources().mobAttack(living);
        else
            original = vehicle.level().damageSources().flyIntoWall();
        return new FlanDamageSource(original.typeHolder(), vehicle, original.getEntity(), null);
    }

    /** Resolves the weapon from the damage itself before considering a melee attacker's held item. */
    public static Optional<InfoType> getWeaponType(DamageSource source)
    {
        Optional<InfoType> meleeWeapon = VehicleMeleeDamage.weaponType(source);
        if (meleeWeapon.isPresent())
            return meleeWeapon;
        if (source instanceof FlanDamageSource flan && flan.weapon != null)
            return Optional.of(flan.weapon);
        Entity direct = source.getDirectEntity();
        if (direct instanceof Bullet bullet && bullet.getFiredShot() != null && bullet.getFiredShot().getFireableGun() != null)
            return Optional.of(bullet.getFiredShot().getFireableGun().getType());
        if (direct instanceof Shootable shootable)
            return Optional.of(shootable.getConfigType());
        if (direct instanceof ThrownGun thrownGun)
            return Optional.ofNullable(thrownGun.getGunType());
        if (direct instanceof Driveable driveable)
            return Optional.ofNullable(driveable.getConfigType());
        if (direct instanceof AAGun gun)
            return Optional.of(gun.getConfigType());
        if (direct instanceof IFlanDamageModel model && model.flansmodultimateDamageModel().orElse(null) instanceof InfoType type)
            return Optional.of(type);
        if (source.is(MELEE) && source.getEntity() instanceof LivingEntity living && living.getMainHandItem().getItem() instanceof IFlanItem<?> item)
            return Optional.of(item.getConfigType());
        return Optional.empty();
    }

    public static boolean isShootableDamage(DamageSource source)
    {
        return source.is(FlanDamageSources.SHOOTABLE) || source.is(FlanDamageSources.HEADSHOT);
    }

    public static Optional<ShootableType> getShootableTypeFromSource(DamageSource source)
    {
        if (source.getDirectEntity() instanceof Shootable shootable)
        {
            return Optional.of(shootable.getConfigType());
        }
        return Optional.empty();
    }
}
