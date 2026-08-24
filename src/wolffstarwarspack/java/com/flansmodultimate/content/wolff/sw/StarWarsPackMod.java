package com.flansmodultimate.content.wolff.sw;

import com.flansmodultimate.PackagedContentPackApi;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(StarWarsPackMod.MOD_ID)
public class StarWarsPackMod
{
    public static final String MOD_ID = "flansmodultimate_wolffstarwars";

    public StarWarsPackMod(ModContainer context)
    {
        PackagedContentPackApi.register(context, MOD_ID, "flans_content", "flans_models");
    }
}
