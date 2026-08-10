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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;

import java.util.Collections;

@EventBusSubscriber(modid = FlansMod.MOD_ID)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonEventHandler
{
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event)
    {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level))
            return;
        ChunkAccess chunk = event.getChunk();
        ApocalypseWorldgen.generate(level, chunk);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (event.getServer() == null
            || !ModApocalypseConfig.apocalypseDimensionEnabled()
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
}
