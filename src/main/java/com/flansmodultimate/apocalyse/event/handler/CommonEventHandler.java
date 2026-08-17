package com.flansmodultimate.apocalyse.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import com.flansmodultimate.apocalyse.common.world.ApocalypseSavedData;
import com.flansmodultimate.apocalyse.common.world.ApocalypseWorldgen;
import com.flansmodultimate.config.ModApocalypseConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber(modid = FlansMod.MOD_ID)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonEventHandler
{
    private static final int MAX_WORLDGEN_CHUNKS_PER_TICK = 64;
    private static final Queue<PendingWorldgen> PENDING_WORLDGEN = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event)
    {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level))
            return;

        // ChunkEvent.Load is fired before the chunk's completion task has necessarily
        // returned. Accessing the level here can synchronously request this or a
        // neighbouring chunk and make the server thread wait on its own task. Defer
        // generation until the server tick, after the load callback has unwound.
        PENDING_WORLDGEN.add(new PendingWorldgen(level, event.getChunk()));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (event.getServer() == null)
            return;

        runPendingWorldgen(event.getServer());

        if (!ModApocalypseConfig.apocalypseDimensionEnabled()
            || !ModApocalypseConfig.apocalypseMobsEnabled())
            return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers())
        {
            if (!player.serverLevel().dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL) || player.isSpectator())
                continue;
            if (player.getRandom().nextInt(ModApocalypseConfig.apocalypseWanderingSurvivorRarity()) != 0)
                continue;
            spawnWanderingSurvivor(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        PENDING_WORLDGEN.removeIf(pending -> pending.level().getServer() == event.getServer());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ModApocalypseConfig.apocalypseDimensionEnabled())
            return;
        if (player.serverLevel().dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL))
            ApocalypseSavedData.get(player.serverLevel()).setDeathPoint(player.getUUID(), player.blockPosition());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !ModApocalypseConfig.apocalypseDimensionEnabled()
            || !ModApocalypseConfig.apocalypseRespawnInApocalypse())
            return;

        ServerLevel targetLevel = player.server.getLevel(ApocalypseContent.APOCALYPSE_LEVEL);
        if (targetLevel == null)
            return;

        ApocalypseSavedData.get(targetLevel).getDeathPoint(player.getUUID())
            .flatMap(deathPoint -> ApocalypseWorldgen.findSafeSurface(targetLevel, deathPoint, ModApocalypseConfig.apocalypseSpawnRadius(), targetLevel.random))
            .ifPresent(pos -> player.teleportTo(targetLevel, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Collections.emptySet(), player.getYRot(), player.getXRot()));
    }

    private static void spawnWanderingSurvivor(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        AABB nearby = player.getBoundingBox().inflate(48.0D);
        if (level.getEntitiesOfClass(SurvivorEntity.class, nearby).size() >= 4)
            return;

        BlockPos center = player.blockPosition();
        ApocalypseWorldgen.findSafeSurface(level, center, 32, level.random)
            .filter(pos -> pos.distSqr(center) > 144.0D)
            .ifPresent(pos -> ApocalypseWorldgen.spawnSurvivor(level, pos));
    }

    private static void runPendingWorldgen(MinecraftServer server)
    {
        // Only process the snapshot queued before this tick. Generation may load
        // neighbouring new chunks, whose work must wait for the following tick too.
        int pendingCount = Math.min(PENDING_WORLDGEN.size(), MAX_WORLDGEN_CHUNKS_PER_TICK);
        for (int i = 0; i < pendingCount; i++)
        {
            PendingWorldgen pending = PENDING_WORLDGEN.poll();
            if (pending == null)
                return;
            if (pending.level().getServer() != server)
            {
                PENDING_WORLDGEN.add(pending);
                continue;
            }
            if (server.getLevel(pending.level().dimension()) == pending.level())
                ApocalypseWorldgen.generate(pending.level(), pending.chunk());
        }
    }

    private record PendingWorldgen(ServerLevel level, ChunkAccess chunk) {}
}
