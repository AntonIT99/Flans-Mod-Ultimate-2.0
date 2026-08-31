package com.flansmodultimate.content.manus;

import com.flansmodultimate.PackagedContentPackApi;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Set;

@Mod(PacksMod.MOD_ID)
public class PacksMod
{
    public static final String MOD_ID = "flansmodultimate_manuspacks";

    public PacksMod(FMLJavaModLoadingContext context)
    {
        PackagedContentPackApi.register(context, MOD_ID, "flans_content", "flans_models", Set.of("parts"));
    }
}
