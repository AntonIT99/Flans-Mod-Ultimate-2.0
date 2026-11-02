package com.wolffsmod.npcs.platform;

import com.wolffsmod.npcs.NpcDistanceCommands;
import com.wolffsmod.npcs.NpcFactionCommands;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.combat.NpcSkill;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.diag.NpcBenchmarkCommands;
import com.wolffsmod.npcs.diag.NpcBudgets;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.NpcWatch;
import com.wolffsmod.npcs.diag.ServerBenchmark;
import com.wolffsmod.npcs.names.SoldierNameAssociations;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.ServerControlPacket;
import com.wolffsmod.npcs.sim.NpcActivity;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.NpcInactiveReturn;
import com.wolffsmod.npcs.sim.NpcPathWorkers;
import com.wolffsmod.npcs.sim.NpcServerRules;
import com.wolffsmod.npcs.teams.AllyNameTags;
import com.wolffsmod.npcs.teams.BotCommands;
import com.wolffsmod.npcs.teams.FactionCommands;
import com.wolffsmod.npcs.teams.FriendlyFireCommands;
import com.wolffsmod.npcs.teams.NpcDuties;
import com.wolffsmod.npcs.teams.NpcSuppression;
import com.wolffsmod.npcs.teams.SkirmishCommands;
import com.wolffsmod.npcs.teams.TeamsBots;
import com.wolffsmod.npcs.teams.WarCommands;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Version boundary for the module's game events: hands NeoForge's events to loader-neutral handlers. NeoForge posts the
 * start and end of a tick as separate events, where Forge 1.20.1 posts one event with a phase.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = NpcsMod.MOD_ID)
public final class NpcsGameEvents
{
    @SubscribeEvent
    @SuppressWarnings({"unused", "java:S1172"}) // NeoForge invokes this subscriber; startup needs no event data.
    public static void serverStarting(ServerStartingEvent event)
    {
        SoldierNameAssociations.load();
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            NpcNetwork.startTracking(event.getTarget(), player);
    }

    /** Players draw NPC name tags at the server's range, so each one gets the server settings on joining. */
    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            NpcNetwork.sendTo(player, ServerControlPacket.values(NpcServerSettings.get(player.server)));
    }

    /** Fired before the player is saved, so a vehicle NPC they ride stays in the world. */
    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            NpcCrew.loggingOut(player);
    }

    @SubscribeEvent
    public static void levelTickEnd(LevelTickEvent.Post event)
    {
        if (event.getLevel() instanceof ServerLevel level)
        {
            NpcActivity.levelTick(level);
            NpcInactiveReturn.levelTick(level);
        }
    }

    @SubscribeEvent
    public static void serverTickStart(ServerTickEvent.Pre event)
    {
        ServerBenchmark.serverTickStart();
    }

    @SubscribeEvent
    public static void serverTickEnd(ServerTickEvent.Post event)
    {
        ServerBenchmark.serverTickEnd(event.getServer());
        NpcServerSettings.serverTick(event.getServer());
        NpcWatch.serverTick(event.getServer().getTickCount());
        TeamsBots.serverTick(event.getServer());
        NpcSuppression.serverTick(event.getServer());
        NpcDuties.serverTick(event.getServer());
        AllyNameTags.serverTick(event.getServer());
    }

    /** Runs first, so that a player Cinematic mode keeps alive counts as no death to anyone. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cinematicDeath(LivingDeathEvent event)
    {
        if (!event.getEntity().level().isClientSide && NpcServerRules.survivesCinematic(event.getEntity(), event.getSource()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event)
    {
        TeamsBots.entityDied(event.getEntity(), event.getSource());
    }

    /**
     * Fired after armour and absorption, just before the health is taken, so a non-lethal NPC and Cinematic mode know how much
     * the player has left.
     */
    @SubscribeEvent
    public static void livingDamage(LivingDamageEvent.Pre event)
    {
        if (event.getEntity().level().isClientSide)
            return;
        float amount = NpcSkill.damageTo(event.getEntity(), event.getSource(), event.getNewDamage());
        event.setNewDamage(NpcServerRules.cinematicDamage(event.getEntity(), event.getSource(), amount));
    }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event)
    {
        TeamsBots.entityDrops(event.getEntity(), event.getDrops());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event)
    {
        ServerBenchmark.serverStopping(event.getServer());
        TeamsBots.serverStopping(event.getServer());
        NpcActivity.clear();
        NpcCombatants.clear();
        NpcBudgets.clear();
        NpcPathWorkers.clear();
        NpcWatch.clear();
        NpcSkill.forget();
        AllyNameTags.serverStopping();
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
        FriendlyFireCommands.register(event.getDispatcher());
    }
}
