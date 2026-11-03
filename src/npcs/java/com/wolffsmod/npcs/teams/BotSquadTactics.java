package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.TeamsCapturePoint;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Adapts the existing commander pass to cached squad leadership; never scans the level or requests a path. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotSquadTactics
{
    record Field(List<Vec3> enemies, List<Vec3> homes, List<Vec3> posts, List<Vec3> routes, List<TeamsCapturePoint> points, int side)
    {}

    static void update(ServerLevel level, BotBattle battle, Doctrine.SquadView view, Field field, int lane)
    {
        BotSquad squad = battle.getSquads().get(view.id());
        SquadTask task = squad.getTask();
        if (task == null || squad.getRole() != BotRole.INFANTRY || view.carrying() || squad.getOrder() != null)
        {
            squad.getTactics().clear();
            return;
        }
        long now = level.getGameTime();
        List<SquadCombat.Member> members = squad.getCombat().commandMembers(level, battle, squad, now);
        Vec3 threat = field.enemies().stream().filter(enemy -> enemy.distanceToSqr(view.position()) <= 9216D || enemy.distanceToSqr(task.target()) <= 4096D)
            .min(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(view.position()))).orElse(null);
        Vec3 refuge = refuge(view.position(), threat, field.homes());
        Vec3 approach = squad.getApproach().waypoint(task);
        double postReach = task.radius() + 24D;
        List<Vec3> nearbyPosts = field.posts().stream().filter(post -> post.distanceToSqr(task.target()) <= postReach * postReach).limit(32L).toList();
        var context = new SquadTactics.Context(task, approach == null ? task.target() : approach, view.readiness(), refuge, threat, secured(field, task),
            coveredRally(level, battle, squad, view.position()), nearbyPosts);
        squad.getTactics().update(context, members, now);
        if (squad.getTactics().takeRetry())
            squad.getApproach().retry(task, view.position(), lane, field.routes(), now);
        faceSectors(level, squad, members);
    }

    private static boolean secured(Field field, SquadTask task)
    {
        return field.points().stream().anyMatch(point -> point.ownerSide() == field.side() && !point.contested() && (point.capturingSide() < 0 || point.capturingSide() == field.side())
            && point.position().distanceToSqr(task.target()) < 1D);
    }

    private static Vec3 refuge(Vec3 position, @Nullable Vec3 threat, List<Vec3> homes)
    {
        Vec3 safe = homes.stream().filter(home -> threat == null || home.distanceToSqr(threat) >= position.distanceToSqr(threat) + 64D)
            .min(Comparator.comparingDouble(home -> home.distanceToSqr(position))).orElse(null);
        if (safe != null)
            return safe;
        Vec3 away = threat == null ? new Vec3(-1D, 0D, 0D) : position.subtract(threat).multiply(1D, 0D, 1D).normalize();
        if (away.lengthSqr() < 1.0E-6D)
            away = new Vec3(-1D, 0D, 0D);
        return position.add(away.scale(24D));
    }

    /** A currently occupied cover stand is already proven reachable; no extra cover or navigation search is needed. */
    @Nullable
    private static Vec3 coveredRally(ServerLevel level, BotBattle battle, BotSquad squad, Vec3 leader)
    {
        for (int slotId : squad.getSlots())
        {
            BotSlot slot = battle.getSlots().get(slotId);
            Entity entity = slot == null || slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof EntityNPCInterface npc && npc.isAlive() && !npc.isKilled() && npc.position().distanceToSqr(leader) <= 256D)
            {
                var safety = NpcTacticState.of(npc).getSafety();
                if (safety.getCover() != null && safety.getDestination() != null && npc.position().distanceToSqr(safety.getDestination()) <= 4D)
                    return npc.position();
            }
        }
        return null;
    }

    private static void faceSectors(ServerLevel level, BotSquad squad, List<SquadCombat.Member> members)
    {
        SquadTask task = squad.getTask();
        if (task == null || squad.getTactics().getPhase() != SquadTactics.Phase.SECURE)
            return;
        for (SquadCombat.Member member : members)
        {
            Entity entity = level.getEntity(member.id());
            var directive = squad.getTactics().order(task, member.id());
            if (entity instanceof EntityNPCInterface npc && directive != null && npc.getTarget() == null && npc.getNavigation().isDone())
            {
                Vec3 outward = directive.spot().subtract(task.target()).multiply(1D, 0D, 1D).normalize();
                if (outward.lengthSqr() < 1.0E-6D)
                    outward = directive.facing();
                Vec3 look = npc.getEyePosition().add(outward.scale(8D));
                npc.getLookControl().setLookAt(look.x, look.y, look.z);
            }
        }
    }
}
