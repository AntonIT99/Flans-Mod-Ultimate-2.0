package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketProneState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Lying prone. A player goes prone with the Go Prone key: the server decides, forces the crawling pose on the player and tells
 * everyone who sees them, since every client works out a player's pose again each tick. Any entity lying in the crawling
 * pose, or that an addon's rule calls prone, shoots with the prone accuracy of its gun.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProneSupport
{
    /** Ticks after standing up before the player may lie down again. */
    public static final int LIE_DOWN_COOLDOWN = 10;

    private static final List<Predicate<LivingEntity>> RULES = new CopyOnWriteArrayList<>();

    public static void addRule(Predicate<LivingEntity> rule)
    {
        Objects.requireNonNull(rule);
        if (!RULES.contains(rule))
            RULES.add(rule);
    }

    public static void removeRule(Predicate<LivingEntity> rule)
    {
        RULES.remove(rule);
    }

    /** Whether the entity lies prone: in the crawling pose on land, or by an addon's rule. Either side. */
    public static boolean isProne(LivingEntity entity)
    {
        return entity.isVisuallyCrawling() || isProneByRule(entity);
    }

    /** Whether an addon's rule calls the entity prone, whatever its vanilla pose. */
    public static boolean isProneByRule(LivingEntity entity)
    {
        for (Predicate<LivingEntity> rule : RULES)
            if (rule.test(entity))
                return true;
        return false;
    }

    /** Whether the entity braces its gun at least as a sneaking one does, against knockback and for recoil recovery. */
    public static boolean isBraced(LivingEntity entity)
    {
        return entity.isCrouching() || isProne(entity);
    }

    /**
     * How far the model lies flat, from 0 standing to 1 prone: the swim amount of an entity in the crawling pose, which eases
     * the body down, and 1 for one prone by a rule, whose renderer lays it flat at once.
     */
    public static float lyingAmount(LivingEntity entity, float swimAmount)
    {
        if (entity.isVisuallyCrawling())
            return swimAmount;
        return isProneByRule(entity) ? 1F : 0F;
    }

    /** Whether something keeps the player from lying, or staying, prone right now: riding, swimming, flying, gliding or sleeping. */
    public static boolean isKeptUp(Player player)
    {
        return !player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping() || player.isFallFlying() || player.getAbilities().flying || player.isInWater()
            || player.isInLava();
    }

    /** Server side: a request from the player's Go Prone key. */
    public static void request(ServerPlayer player, boolean prone)
    {
        PlayerData data = PlayerData.getInstance(player);
        if (prone == data.isProne())
        {
            // The client may have shown a change the server never made; tell it again.
            PacketHandler.sendTo(new PacketProneState(player.getUUID(), prone), player);
            return;
        }
        if (prone)
        {
            if (ModCommonConfig.isProneDisabled())
            {
                player.displayClientMessage(Component.translatable("message.flansmodultimate.prone_disabled"), true);
                return;
            }
            if (isKeptUp(player) || player.level().getGameTime() - data.getProneChangeTime() < LIE_DOWN_COOLDOWN)
            {
                PacketHandler.sendTo(new PacketProneState(player.getUUID(), false), player);
                return;
            }
        }
        setProne(player, prone);
    }

    /** Server side: lays the player down or stands them up, and tells everyone who sees them. */
    public static void setProne(ServerPlayer player, boolean prone)
    {
        PlayerData data = PlayerData.getInstance(player);
        if (data.isProne() != prone)
            data.setProneChangeTime(player.level().getGameTime());
        data.setProne(prone);
        apply(player, prone);
        PacketHandler.sendToTracking(new PacketProneState(player.getUUID(), prone), player);
    }

    /**
     * Server side: tells a player who just started seeing another one whether this one lies prone. Sent either way, since the
     * receiver may still remember an earlier entity of the same player lying prone.
     */
    public static void sendState(ServerPlayer player, ServerPlayer receiver)
    {
        PacketHandler.sendTo(new PacketProneState(player.getUUID(), PlayerData.getInstance(player).isProne()), receiver);
    }

    /**
     * Server side: stands up a player whose client builds a new player entity, after respawning or changing dimension. Those
     * who see the new entity hear its state when they start seeing it.
     */
    public static void reset(ServerPlayer player)
    {
        PlayerData.getInstance(player).setProne(false);
        apply(player, false);
        PacketHandler.sendTo(new PacketProneState(player.getUUID(), false), player);
    }

    /** Server side: forgets the prone state of a player who left. */
    public static void forget(Player player)
    {
        PlayerData.getInstance(player).setProne(false);
    }

    /** Forces or releases the crawling pose; releasing lets vanilla pick crouching or crawling where the player cannot stand. */
    public static void apply(Player player, boolean prone)
    {
        if (prone)
        {
            player.setForcedPose(Pose.SWIMMING);
            player.setSprinting(false);
        }
        else if (player.getForcedPose() == Pose.SWIMMING)
            player.setForcedPose(null);
    }

    /**
     * Each player tick, on both sides. The server stands up a player who may no longer lie prone; each side keeps the forced
     * pose of a prone player, whose entity a client may have built anew, and keeps them from sprinting.
     */
    public static void tick(Player player)
    {
        PlayerData data = PlayerData.getInstance(player);
        if (!data.isProne())
            return;
        if (player instanceof ServerPlayer serverPlayer && (isKeptUp(player) || ModCommonConfig.isProneDisabled()))
        {
            setProne(serverPlayer, false);
            return;
        }
        if (player.getForcedPose() != Pose.SWIMMING || player.isSprinting())
            apply(player, true);
    }
}
