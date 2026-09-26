package com.flansmodultimate.npcs;

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
    public static final String MOD_ID = "flansmodultimate_npcs";
    public static final Logger LOG = LogUtils.getLogger();

    public NpcsMod(FMLJavaModLoadingContext context)
    {
        LOG.info("Loading Flan's Ultimate 2: NPC Vehicles & Soldiers");
    }
}
