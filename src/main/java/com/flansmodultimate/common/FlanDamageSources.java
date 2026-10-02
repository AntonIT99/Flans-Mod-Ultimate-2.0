package com.flansmodultimate.common;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Shootable;
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
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageType);
        return new FlanDamageSource(holder, directAttacker, indirectAttacker);
    }

    /**
     * A Flan's damage source. Its death message adds how far away the killer was, as 1.7.10 did, unless
     * kill messages or the distance are switched off in the common config.
     */
    private static final class FlanDamageSource extends DamageSource
    {
        private FlanDamageSource(Holder<DamageType> type, @Nullable Entity directAttacker, @Nullable Entity indirectAttacker)
        {
            super(type, directAttacker, indirectAttacker);
        }

        @Override
        @NotNull
        public Component getLocalizedDeathMessage(@NotNull LivingEntity killed)
        {
            Component message = super.getLocalizedDeathMessage(killed);
            Entity killer = getEntity();
            if (killer == null || killer == killed || !ModCommonConfig.enableKillMessages() || !ModCommonConfig.showDistanceInKillMessage())
                return message;
            String distance = String.format(Locale.ROOT, "%.1f", killed.distanceTo(killer));
            return Component.translatable("death.flansmodultimate.distance", message, distance);
        }
    }

    public static DamageSource createDamageSource(Level level, @Nullable Entity attacker, ResourceKey<DamageType> damageType)
    {
        return createDamageSource(level, attacker, attacker, damageType);
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
