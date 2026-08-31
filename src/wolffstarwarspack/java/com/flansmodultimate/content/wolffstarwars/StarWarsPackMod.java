package com.flansmodultimate.content.wolffstarwars;

import com.flansmodultimate.PackagedContentPackApi;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StarWarsPackMod.MOD_ID)
public class StarWarsPackMod
{
    public static final String MOD_ID = "flansmodultimate_wolffstarwars";

    public StarWarsPackMod(FMLJavaModLoadingContext context)
    {
        PackagedContentPackApi.register(context, MOD_ID, "flans_content", "flans_models");
    }
}
