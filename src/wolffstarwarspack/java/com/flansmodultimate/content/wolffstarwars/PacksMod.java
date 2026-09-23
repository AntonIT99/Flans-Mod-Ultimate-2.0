package com.flansmodultimate.content.wolffstarwars;

import com.flansmodultimate.PackagedContentPackApi;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(PacksMod.MOD_ID)
public class PacksMod
{
    public static final String MOD_ID = "flansmodultimate_wolffstarwars";

    public PacksMod(ModContainer context)
    {
        PackagedContentPackApi.register(context, MOD_ID, "flans_content", "flans_models");
    }
}
