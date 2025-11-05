package com.wolffsarmormod.common;

import com.wolffsarmormod.ArmorMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansDamageSources
{
    public static final ResourceKey<DamageType> FLANS_GUN = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ArmorMod.MOD_ID, "flans_gun"));
    public static final ResourceKey<DamageType> FLANS_GUN_HEADSHOT = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ArmorMod.MOD_ID, "flans_gun_headshot"));
    public static final ResourceKey<DamageType> FLANS_EXPLOSION = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ArmorMod.MOD_ID, "flans_explosion"));

    public static DamageSource createDamageSource(Level level, @Nullable Entity direct, @Nullable Entity attacker, ResourceKey<DamageType> damageType)
    {
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageType);
        return new DamageSource(holder, direct, attacker);
    }
}
