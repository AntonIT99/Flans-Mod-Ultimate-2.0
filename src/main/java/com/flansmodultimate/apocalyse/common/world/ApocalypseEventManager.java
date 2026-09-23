package com.flansmodultimate.apocalyse.common.world;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.AiMechaEntity;
import com.flansmodultimate.apocalyse.common.entity.InventoryHolderEntity;
import com.flansmodultimate.apocalyse.common.entity.NukeDropEntity;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketApocalypseCountdown;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Runs the apocalypse itself: the countdown an AI-chip mecha starts, the bombardment while
 * it runs, and the one-way trip it ends with.
 *
 * <p>Placing a mecha whose engine is an AI chip arms the trigger. The machine then spends
 * the countdown thrashing on the spot and calling nukes down around itself, and when the
 * timer runs out it disintegrates and takes whoever the configured teleport option covers
 * with it.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseEventManager
{
    /** Nukes land within this many blocks of the trigger, once a second. */
    private static final double NUKE_SPREAD = 150.0D;
    private static final int NUKE_INTERVAL_TICKS = 20;
    /** Radius covered by the NEARBY teleport options. */
    private static final double NEARBY_RADIUS = 50.0D;
    /** How far from the entry point a landing site is looked for on arrival. */
    private static final int ARRIVAL_SEARCH_RADIUS = 32;

    /**
     * Arms the trigger if {@code entity} is a mecha running an AI chip.
     *
     * <p>Only one apocalypse can be counting down at a time, and never from inside the
     * apocalypse itself.</p>
     */
    public static void onDriveableSpawned(Entity entity)
    {
        // An apocalypse guard is a mecha too, and must never be able to start another one.
        if (!(entity instanceof Mecha mecha) || entity instanceof AiMechaEntity
            || !(entity.level() instanceof ServerLevel level))
            return;
        if (!ModApocalypseConfig.apocalypseDimensionEnabled()
            || level.dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL))
            return;
        if (mecha.getDriveableData() == null)
            return;
        PartType engine = mecha.getDriveableData().getEngine();
        if (engine == null || !engine.isAiChip())
            return;

        ApocalypseSavedData data = ApocalypseSavedData.get(level);
        if (data.isCountdownRunning())
            return;

        int ticks = ModApocalypseConfig.apocalypseCountdownLength();
        data.startCountdown(ticks, mecha.getUUID(), mecha.getOwnerId(), level.dimension());
        FlansMod.log.info("An AI chip mecha has armed the apocalypse. {} ticks remain.", ticks);
        broadcastCountdown(level.getServer(), ticks);
    }

    /** Advances an armed countdown by one tick. */
    public static void tick(MinecraftServer server)
    {
        ServerLevel storage = server.getLevel(Level.OVERWORLD);
        if (storage == null)
            return;
        ApocalypseSavedData data = ApocalypseSavedData.get(storage);
        if (!data.isCountdownRunning())
            return;

        ServerLevel level = server.getLevel(data.getCountdownLevel());
        Mecha mecha = level == null ? null : findMecha(level, data.getCountdownMechaId());
        if (mecha == null || !mecha.isAlive())
        {
            // The trigger was destroyed or unloaded; 1.7.10 simply cancelled the event.
            data.clearCountdown();
            broadcastCountdown(server, 0);
            return;
        }

        int remaining = data.getCountdownTicks() - 1;
        data.setCountdownTicks(remaining);
        thrash(level, mecha);

        if (remaining % NUKE_INTERVAL_TICKS == 0)
        {
            dropNuke(level, mecha);
            broadcastCountdown(server, remaining);
        }

        if (remaining <= 0)
            begin(server, level, mecha, data);
    }

    /** The machine shakes itself apart on the spot while the timer runs. */
    private static void thrash(ServerLevel level, Mecha mecha)
    {
        float yaw = mecha.getYaw() + level.random.nextFloat() * 10F;
        float pitch = (float) level.random.nextGaussian() * 3F;
        mecha.setOrientation(Mth.wrapDegrees(yaw), Mth.clamp(pitch, -20F, 20F), 0F);
    }

    private static void dropNuke(ServerLevel level, Mecha mecha)
    {
        if (!ModApocalypseConfig.apocalypseNukeDropsEnabled())
            return;
        NukeDropEntity nuke = new NukeDropEntity(ApocalypseContent.nukeDrop.get(), level);
        double x = mecha.getX() + level.random.nextGaussian() * NUKE_SPREAD;
        double z = mecha.getZ() + level.random.nextGaussian() * NUKE_SPREAD;
        nuke.moveTo(x, level.getMaxBuildHeight() - 1, z, 0F, 0F);
        level.addFreshEntity(nuke);
    }

    private static void begin(MinecraftServer server, ServerLevel level, Mecha mecha, ApocalypseSavedData data)
    {
        FlansMod.log.info("The apocalypse has begun!");
        BlockPos entryPoint = mecha.blockPosition();
        List<ServerPlayer> travellers = selectTravellers(server, level, mecha, data.getCountdownPlacerId());
        data.clearCountdown();
        broadcastCountdown(server, 0);
        mecha.discard();

        for (ServerPlayer player : travellers)
            sendPlayerToApocalypse(player, entryPoint);
    }

    /** Resolves the configured teleport option into the players it actually covers. */
    private static List<ServerPlayer> selectTravellers(MinecraftServer server, ServerLevel level, Mecha mecha,
                                                       @Nullable UUID placerId)
    {
        ServerPlayer placer = placerId == null ? null : server.getPlayerList().getPlayer(placerId);
        List<ServerPlayer> travellers = new ArrayList<>();
        switch (ModApocalypseConfig.apocalypseTeleportOption())
        {
            case PLACER_ONLY ->
            {
                if (placer != null && placer.serverLevel() == level)
                    travellers.add(placer);
            }
            case DIM -> travellers.addAll(level.players());
            case NEARBY ->
            {
                for (ServerPlayer player : level.players())
                {
                    if (player.distanceToSqr(mecha) < NEARBY_RADIUS * NEARBY_RADIUS)
                        travellers.add(player);
                }
            }
            // The opt-in variants never moved anybody on their own in 1.7.10 either; players
            // reach the apocalypse through a portal instead.
            case DIM_OPT_IN, NEARBY_OPT_IN -> announceOptIn(level);
        }
        travellers.removeIf(ServerPlayer::isSpectator);
        return travellers;
    }

    private static void announceOptIn(ServerLevel level)
    {
        for (ServerPlayer player : level.players())
            player.sendSystemMessage(Component.translatable("message.flansmodultimate.apocalypse_begun"));
    }

    /**
     * Takes one player to the apocalypse, leaving their belongings behind in a stand-in and
     * handing them the tools to start again.
     */
    public static void sendPlayerToApocalypse(ServerPlayer player, BlockPos entryPoint)
    {
        ServerLevel target = player.server.getLevel(ApocalypseContent.APOCALYPSE_LEVEL);
        if (target == null)
        {
            player.displayClientMessage(Component.translatable("message.flansmodultimate.apocalypse_dimension_unavailable"), true);
            return;
        }

        ServerLevel origin = player.serverLevel();
        leaveBelongingsBehind(player);
        ApocalypseSavedData.get(origin).setEntryPoint(player.getUUID(), entryPoint);

        BlockPos arrival = ApocalypseWorldgen
            .findSafeSurface(target, new BlockPos(entryPoint.getX(), entryPoint.getY(), entryPoint.getZ()), ARRIVAL_SEARCH_RADIUS, target.random)
            .orElseGet(() -> surfaceAbove(target, entryPoint));
        player.teleportTo(target, arrival.getX() + 0.5D, arrival.getY(), arrival.getZ() + 0.5D,
            Collections.emptySet(), player.getYRot(), player.getXRot());
        player.setPortalCooldown();
        giveStarterKit(player);
        player.sendSystemMessage(Component.translatable("message.flansmodultimate.apocalypse_begun"));
    }

    /**
     * Copies the player's belongings into a stand-in left where they were standing, then
     * empties their inventory.
     */
    private static void leaveBelongingsBehind(ServerPlayer player)
    {
        InventoryHolderEntity holder = InventoryHolderEntity.createFor(player);
        if (holder != null)
            player.level().addFreshEntity(holder);
        player.getInventory().clearContent();
        player.containerMenu.broadcastChanges();
    }

    private static void giveStarterKit(ServerPlayer player)
    {
        addOrDrop(player, new ItemStack(Items.STONE_PICKAXE));
        addOrDrop(player, new ItemStack(Items.STONE_SHOVEL));
        addOrDrop(player, new ItemStack(Blocks.OAK_LOG, 8));
        addOrDrop(player, new ItemStack(Items.COOKED_BEEF, 4));
    }

    private static void addOrDrop(ServerPlayer player, ItemStack stack)
    {
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    private static BlockPos surfaceAbove(ServerLevel level, BlockPos entryPoint)
    {
        BlockPos pos = new BlockPos(entryPoint.getX(), level.getMaxBuildHeight() - 1, entryPoint.getZ());
        while (pos.getY() > level.getMinBuildHeight() + 1 && level.getBlockState(pos.below()).isAir())
            pos = pos.below();
        return pos;
    }

    @Nullable
    private static Mecha findMecha(ServerLevel level, @Nullable UUID id)
    {
        if (id == null)
            return null;
        return level.getEntity(id) instanceof Mecha mecha ? mecha : null;
    }

    private static void broadcastCountdown(MinecraftServer server, int ticks)
    {
        PacketApocalypseCountdown packet = new PacketApocalypseCountdown(ticks);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            PacketHandler.sendTo(packet, player);
    }
}
