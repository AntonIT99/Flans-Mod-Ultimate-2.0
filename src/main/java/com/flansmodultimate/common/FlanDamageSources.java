package com.flansmodultimate.common;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.types.ShootableType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

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
        return new DamageSource(holder, directAttacker, indirectAttacker);
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
