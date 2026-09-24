package com.flansmodultimate.apocalyse.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import com.flansmodultimate.apocalyse.common.util.ApocalypseDriveableHelper;
import com.flansmodultimate.apocalyse.common.world.ApocalypseEventManager;
import com.flansmodultimate.apocalyse.common.world.ApocalypseSavedData;
import com.flansmodultimate.apocalyse.common.world.ApocalypseWorldgen;
import com.flansmodultimate.config.ModApocalypseConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Collections;

@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonEventHandler
{
    private static final double WANDERING_SURVIVOR_DISTANCE = 50.0D;

    /** Runs at the end of every server tick. */
    public static void onServerTick(@Nullable MinecraftServer server)
    {
        if (server == null)
            return;

        ApocalypseEventManager.tick(server);

        if (!ModApocalypseConfig.apocalypseDimensionEnabled())
            return;

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (!player.serverLevel().dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL) || player.isSpectator())
                continue;
            if (player.getRandom().nextInt(ModApocalypseConfig.apocalypseFlyByRarity()) == 0)
                ApocalypseDriveableHelper.spawnFlyBy(player.serverLevel(), player.position(), player.getRandom());
            if (!ModApocalypseConfig.apocalypseMobsEnabled())
                continue;
            if (player.getRandom().nextInt(ModApocalypseConfig.apocalypseWanderingSurvivorRarity()) != 0)
                continue;
            spawnWanderingSurvivor(player);
        }
    }

    /**
     * Arms the apocalypse when a mecha running an AI chip appears in the world.
     *
     * <p>This is the placement hook: a mecha reaches the world through its item, through a
     * command, or out of a structure, and every one of those routes ends here.</p>
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide)
            return;
        ApocalypseEventManager.onDriveableSpawned(event.getEntity());
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
        AABB nearby = player.getBoundingBox().inflate(WANDERING_SURVIVOR_DISTANCE + 16.0D);
        if (level.getEntitiesOfClass(SurvivorEntity.class, nearby).size() >= 4)
            return;

        // As in 1.12.2: only after dark, and on a ring 50 blocks out from the player.
        if (level.isDay())
            return;
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        int x = Mth.floor(player.getX() + Math.cos(angle) * WANDERING_SURVIVOR_DISTANCE);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * WANDERING_SURVIVOR_DISTANCE);
        if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z)))
            return;
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        ApocalypseWorldgen.spawnSurvivor(level, pos);
    }
}
