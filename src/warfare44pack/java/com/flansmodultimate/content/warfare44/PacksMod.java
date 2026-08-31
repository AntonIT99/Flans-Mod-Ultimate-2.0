package com.flansmodultimate.content.warfare44;

import com.flansmodultimate.PackagedContentPackApi;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PacksMod.MOD_ID)
public class PacksMod
{
    public static final String MOD_ID = "flansmodultimate_warfare44";

    public PacksMod(FMLJavaModLoadingContext context)
    {
        PackagedContentPackApi.register(context, MOD_ID, "flans_content", "flans_models");
    }
}
