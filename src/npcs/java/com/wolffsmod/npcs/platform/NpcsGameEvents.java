package com.wolffsmod.npcs.platform;

import com.wolffsmod.npcs.NpcDistanceCommands;
import com.wolffsmod.npcs.NpcFactionCommands;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.diag.NpcBenchmarkCommands;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.ServerBenchmark;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.sim.NpcActivity;
import com.wolffsmod.npcs.teams.BotCommands;
import com.wolffsmod.npcs.teams.FactionCommands;
import com.wolffsmod.npcs.teams.SkirmishCommands;
import com.wolffsmod.npcs.teams.TeamsBots;
import com.wolffsmod.npcs.teams.WarCommands;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Version boundary for the module's game events: hands Forge's events to loader-neutral handlers. Forge 1.20.1 posts one
 * tick event with a phase, where NeoForge posts the start and end of a tick as separate events.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = NpcsMod.MOD_ID)
public final class NpcsGameEvents
{
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            NpcNetwork.startTracking(event.getTarget(), player);
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level)
            NpcActivity.levelTick(level);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            ServerBenchmark.serverTickStart();
            return;
        }
        ServerBenchmark.serverTickEnd(event.getServer());
        NpcServerSettings.serverTick(event.getServer());
        TeamsBots.serverTick(event.getServer());
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event)
    {
        TeamsBots.entityDied(event.getEntity(), event.getSource());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event)
    {
        ServerBenchmark.serverStopping(event.getServer());
        TeamsBots.serverStopping(event.getServer());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event)
    {
        NpcDistanceCommands.register(event.getDispatcher());
        NpcFactionCommands.register(event.getDispatcher());
        NpcBenchmarkCommands.register(event.getDispatcher());
        BotCommands.register(event.getDispatcher());
        FactionCommands.register(event.getDispatcher());
        SkirmishCommands.register(event.getDispatcher());
        WarCommands.register(event.getDispatcher());
    }
}
