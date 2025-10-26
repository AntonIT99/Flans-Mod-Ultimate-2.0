package com.wolffsarmormod.common.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class FlansDamageTypes
{
    public static final String MODID = "yourmod";

    public static final ResourceKey<DamageType> FLANS_GUN =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "flans_gun"));
    public static final ResourceKey<DamageType> FLANS_GUN_HEADSHOT =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "flans_gun_headshot"));
    public static final ResourceKey<DamageType> FLANS_EXPLOSION =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "flans_explosion"));

    private FlansDamageTypes() {}
}
