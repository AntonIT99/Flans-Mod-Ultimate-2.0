package com.flansmodultimate.content;

import com.flansmodultimate.PackagedContentPackApi;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Set;

@Mod(OfficialPacksMod.MOD_ID)
public class OfficialPacksMod
{
    public static final String MOD_ID = "flansmodultimate_officialpacks";

    public OfficialPacksMod(FMLJavaModLoadingContext context)
    {
        PackagedContentPackApi.register(context, MOD_ID, "flans_content", "flans_models", Set.of("parts"));
    }
}
