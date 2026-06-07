package com.flansmodultimate.apocalyse.common.util;

import com.flansmodultimate.apocalyse.ApocalypseContent;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class ApocalypseDamageSources
{
    private ApocalypseDamageSources()
    {
    }

    public static DamageSource sulphuricAcid(Level level)
    {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(ApocalypseContent.SULPHURIC_ACID_DAMAGE);
        return new DamageSource(holder);
    }
}
