package com.flansmodultimate.common.recipe;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class GunpowderRecipeCondition implements ICondition
{
    public static final DeferredRegister<MapCodec<? extends ICondition>> CODECS =
        DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, FlansMod.FLANSMOD_ID);
    private static final MapCodec<GunpowderRecipeCondition> CODEC = MapCodec.unit(new GunpowderRecipeCondition());
    public static final Supplier<MapCodec<GunpowderRecipeCondition>> REGISTERED_CODEC =
        CODECS.register("add_gunpowder_recipe", () -> CODEC);

    @Override
    public MapCodec<? extends ICondition> codec()
    {
        return CODEC;
    }

    @Override
    public boolean test(IContext context)
    {
        return ModCommonConfig.addGunpowderRecipe();
    }

}
