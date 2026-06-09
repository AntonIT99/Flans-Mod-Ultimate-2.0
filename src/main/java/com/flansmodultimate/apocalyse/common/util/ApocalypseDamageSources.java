package com.flansmodultimate.apocalyse.common.util;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseDamageSources
{
    public static final ResourceKey<DamageType> SULPHURIC_ACID = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "sulphuric_acid"));

    public static DamageSource sulphuricAcid(Level level)
    {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(SULPHURIC_ACID);
        return new DamageSource(holder);
    }
}
