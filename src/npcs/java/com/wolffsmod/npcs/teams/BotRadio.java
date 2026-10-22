package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.TeamsCapturePoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Bots calling out on their side's radio what happens at the objectives: a point taken or lost, a held point under
 * attack, enemy armour closing in on one. The call comes from the side's bot nearest the place, or from headquarters,
 * and only its side's players hear it. Each call about a point waits a while before it is made again. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotRadio
{
    private static final String PREFIX = "wolffsmodnpcs.radio.";
    /** Ticks before the same call about the same point is made again. */
    private static final long ATTACK_REPEAT = 60L * 20L;
    private static final long ARMOUR_REPEAT = 90L * 20L;
    /** Blocks beyond a point's radius within which enemy armour counts as closing in. */
    private static final double ARMOUR_RANGE = 48D;

    /** A change of owner heard from the Teams events, called out at the next pass. */
    private record Change(TeamsCapturePoint point, int previousOwner)
    {}

    private static final List<Change> CHANGES = new ArrayList<>();
    /** When each call was last made, by side, point and kind of call. */
    private static final Map<String, Long> LAST_CALL = new HashMap<>();

    static void pointChanged(TeamsCapturePoint point, int previousOwner)
    {
        CHANGES.add(new Change(point, previousOwner));
    }

    static void forget()
    {
        CHANGES.clear();
        LAST_CALL.clear();
    }

    static void tick(MinecraftServer server, ServerLevel level, BotBattle battle)
    {
        List<Change> changes = List.copyOf(CHANGES);
        CHANGES.clear();
        if (!battle.getSettings().isEnabled() || !battle.getSettings().isRadio())
            return;
        long now = level.getGameTime();
        changes.forEach(change -> callChange(server, level, battle, change));
        for (TeamsCapturePoint point : battle.getGround().capturePoints())
        {
            if (point.ownerSide() >= 0)
                callThreats(server, level, battle, point, now);
        }
    }

    /** A point taken: the side that took it hears so, and the side that lost it. */
    private static void callChange(MinecraftServer server, ServerLevel level, BotBattle battle, Change change)
    {
        TeamsCapturePoint point = change.point();
        if (point.ownerSide() >= 0)
            call(server, level, battle, point.ownerSide(), Component.translatable(PREFIX + "captured", label(point)), point.position());
        if (change.previousOwner() >= 0 && change.previousOwner() != point.ownerSide())
            call(server, level, battle, change.previousOwner(), Component.translatable(PREFIX + "lost", label(point)), point.position());
    }

    /** A held point under attack, or with enemy armour closing in, called out once in a while. */
    private static void callThreats(MinecraftServer server, ServerLevel level, BotBattle battle, TeamsCapturePoint point, long now)
    {
        int owner = point.ownerSide();
        boolean attacked = point.contested() || point.capturingSide() >= 0 && point.capturingSide() != owner;
        if (attacked && due(owner, point, "attack", now, ATTACK_REPEAT))
            call(server, level, battle, owner, Component.translatable(PREFIX + "under_attack", label(point)), point.position());
        if (armourNear(server, level, battle, owner, point) && due(owner, point, "armour", now, ARMOUR_REPEAT))
            call(server, level, battle, owner, Component.translatable(PREFIX + "armour", label(point)), point.position());
    }

    private static boolean due(int side, TeamsCapturePoint point, String call, long now, long repeat)
    {
        String key = side + ":" + point.id() + ":" + call;
        Long last = LAST_CALL.get(key);
        if (last != null && now - last < repeat)
            return false;
        LAST_CALL.put(key, now);
        return true;
    }

    /** Whether enemy armour, bots or players in vehicles, is closing in on a point a side holds. */
    private static boolean armourNear(MinecraftServer server, ServerLevel level, BotBattle battle, int side, TeamsCapturePoint point)
    {
        double reach = point.radius() + ARMOUR_RANGE;
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity != null && entity.isAlive() && battle.sideOf(slot.getTeam()) != side && entity.position().distanceTo(point.position()) <= reach && BotTargeting.armoured(battle, entity))
                return true;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            boolean enemy = FlansTeams.getTeam(player).map(team -> battle.getGround().sideOf(team) != side).orElse(false);
            if (enemy && player.isAlive() && player.level() == level && player.position().distanceTo(point.position()) <= reach && BotTargeting.armoured(battle, player))
                return true;
        }
        return false;
    }

    /** Sends a call to every player of a side, spoken by the side's bot nearest the place it is about. */
    private static void call(MinecraftServer server, ServerLevel level, BotBattle battle, int side, Component text, Vec3 about)
    {
        Component speaker = speaker(level, battle, side, about);
        Component message = Component.translatable(PREFIX + "message", speaker, text).withStyle(ChatFormatting.GRAY);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (battle.getGround().sideOf(player) == side)
                player.sendSystemMessage(message);
        }
    }

    private static Component speaker(ServerLevel level, BotBattle battle, int side, Vec3 about)
    {
        BotSlot nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof LivingEntity living && living.isAlive() && battle.sideOf(slot.getTeam()) == side && !slot.getName().isEmpty())
            {
                double distance = entity.position().distanceToSqr(about);
                if (distance < best)
                {
                    best = distance;
                    nearest = slot;
                }
            }
        }
        return nearest == null ? Component.translatable(PREFIX + "headquarters") : Component.literal(nearest.getName());
    }

    private static Component label(TeamsCapturePoint point)
    {
        return Component.literal(String.valueOf(point.symbol())).withStyle(ChatFormatting.YELLOW);
    }
}
