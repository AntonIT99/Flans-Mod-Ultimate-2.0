package com.wolffsmod.npcs;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entrypoint of Flan's Ultimate 2: NPC Vehicles &amp; Soldiers, which lets Custom NPCs soldiers use Flan's Mod
 * Ultimate vehicles and weapons. Both mods are mandatory dependencies declared in {@code mods.toml}.
 */
@Mod(NpcsMod.MOD_ID)
public class NpcsMod
{
    public static final String MOD_ID = "wolffsmodnpcs";
    public static final Logger log = LogUtils.getLogger();

    public NpcsMod(FMLJavaModLoadingContext context)
    {
    }
}
