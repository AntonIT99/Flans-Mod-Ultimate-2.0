package com.wolffsmod.npcs;

import com.mojang.logging.LogUtils;
import com.wolffsmod.npcs.model.FlanModelEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entrypoint of Flan's Ultimate 2: NPC Vehicles &amp; Soldiers, which lets Custom NPCs soldiers use Flan's Mod
 * Ultimate vehicles and weapons. Both mods are mandatory dependencies declared in {@code neoforge.mods.toml}.
 */
@Mod(NpcsMod.MOD_ID)
public class NpcsMod
{
    public static final String MOD_ID = "wolffsmodnpcs";
    public static final Logger log = LogUtils.getLogger();

    public NpcsMod(IEventBus modEventBus)
    {
        modEventBus.addListener(FlanModelEntities::registerEntityTypes);
        modEventBus.addListener(FlanModelEntities::registerAttributes);
    }
}
