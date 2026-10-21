package com.wolffsmod.npcs;

import com.mojang.logging.LogUtils;
import com.wolffsmod.npcs.model.FlanModelEntities;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.platform.network.NpcNetworkPlatform;
import com.wolffsmod.npcs.teams.TeamsBots;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entrypoint of Flan's Ultimate 2: NPC Vehicles &amp; Soldiers, which lets Custom NPCs soldiers use Flan's Mod
 * Ultimate vehicles and weapons. Both mods are mandatory dependencies declared in the module's mod metadata. Game
 * events reach the module through {@code platform.NpcsGameEvents}.
 */
@Mod(NpcsMod.MOD_ID)
public class NpcsMod
{
    public static final String MOD_ID = "wolffsmodnpcs";
    public static final Logger log = LogUtils.getLogger();

    @SuppressWarnings("java:S1118")
    public NpcsMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(FlanModelEntities::registerEntityTypes);
        modEventBus.addListener(FlanModelEntities::registerAttributes);
        NpcNetwork.register();
        NpcNetworkPlatform.init(modEventBus);
        TeamsBots.register();
    }
}
