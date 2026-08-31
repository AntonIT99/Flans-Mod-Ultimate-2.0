package com.flansmodultimate.content.warfare44;

import com.flansmodultimate.PackagedContentPackApi;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Warfare44PackMod.MOD_ID)
public class Warfare44PackMod
{
    public static final String MOD_ID = "flansmodultimate_warfare44";

    public Warfare44PackMod(FMLJavaModLoadingContext context)
    {
        PackagedContentPackApi.register(context, MOD_ID, "flans_content", "flans_models");
    }
}
