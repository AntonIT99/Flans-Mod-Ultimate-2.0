package com.wolffsarmormod.common.damagesource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansDamageSources
{
    public static DamageSource createDamageSource(Level level, @Nullable Entity direct, @Nullable Entity attacker, boolean headshot)
    {
        var key = headshot ? FlansDamageTypes.FLANS_GUN_HEADSHOT : FlansDamageTypes.FLANS_GUN;
        var holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);

        if (direct != null && attacker != null)
            return new DamageSource(holder, direct, attacker);
        if (direct != null)
            return new DamageSource(holder, direct);
        if (attacker != null)
            return new DamageSource(holder, attacker);

        return new DamageSource(holder);
    }
}
