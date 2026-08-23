package com.flansmodultimate.apocalyse.common;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseDamageSources
{
    public static final ResourceKey<DamageType> SULPHURIC_ACID = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "sulphuric_acid"));

    public static DamageSource sulphuricAcid(Level level)
    {
        Holder<DamageType> holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SULPHURIC_ACID);
        return new DamageSource(holder);
    }
}
