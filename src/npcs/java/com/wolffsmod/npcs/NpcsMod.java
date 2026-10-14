package com.wolffsmod.npcs;

import com.mojang.logging.LogUtils;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.ServerBenchmark;
import com.wolffsmod.npcs.model.FlanModelEntities;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.sim.NpcActivity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
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

    @SuppressWarnings("java:S1118")
    public NpcsMod(IEventBus modEventBus)
    {
        modEventBus.addListener(FlanModelEntities::registerEntityTypes);
        modEventBus.addListener(FlanModelEntities::registerAttributes);
        modEventBus.addListener(NpcNetwork::register);
        NeoForge.EVENT_BUS.addListener(NpcNetwork::startTracking);
        NeoForge.EVENT_BUS.addListener(NpcActivity::levelTick);
        NeoForge.EVENT_BUS.addListener(ServerBenchmark::serverTickStart);
        NeoForge.EVENT_BUS.addListener(ServerBenchmark::serverTick);
        NeoForge.EVENT_BUS.addListener(ServerBenchmark::serverStopping);
        NeoForge.EVENT_BUS.addListener(NpcServerSettings::serverTick);
    }
}
