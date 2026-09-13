package com.flansmodultimate.common.recipe;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ModCommonConfig;
import com.google.gson.JsonObject;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

import net.minecraft.resources.ResourceLocation;

public final class GunpowderRecipeCondition implements ICondition
{
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "add_gunpowder_recipe");
    public static final Serializer SERIALIZER = new Serializer();

    @Override
    public ResourceLocation getID()
    {
        return ID;
    }

    @Override
    public boolean test(IContext context)
    {
        return ModCommonConfig.addGunpowderRecipe();
    }

    public static final class Serializer implements IConditionSerializer<GunpowderRecipeCondition>
    {
        @Override
        public void write(JsonObject json, GunpowderRecipeCondition condition)
        {
        }

        @Override
        public GunpowderRecipeCondition read(JsonObject json)
        {
            return new GunpowderRecipeCondition();
        }

        @Override
        public ResourceLocation getID()
        {
            return ID;
        }
    }
}
