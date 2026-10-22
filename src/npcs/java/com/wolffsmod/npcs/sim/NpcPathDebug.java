package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.drivers.NpcDriver;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcTactics;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.joml.Vector3f;

import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Shows where an NPC with the movement debug switch is going: its remaining path as red dust, the point its driving
 * controller steers for as green sparks, and the end of the path as a flame. For an NPC at the controls of a Flan's
 * driveable, the sparks mark the point it steers or flies for. Once a second it also writes what the NPC is doing to
 * the server log, with what it does at the controls. The particles are sent by the server, so every nearby player sees
 * the same picture.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPathDebug
{
    private static final int PARTICLE_INTERVAL = 10;
    private static final int LOG_INTERVAL = 20;
    /** Most path markers drawn per update; a longer path is sampled evenly. */
    static final int MAX_MARKERS = 48;
    private static final double VIEW_DISTANCE = 96D;
    private static final ParticleOptions PATH = new DustParticleOptions(new Vector3f(1F, 0.1F, 0.1F), 1.2F);

    public static void tick(EntityNPCInterface npc, NpcVehicleState state)
    {
        if (!(npc.level() instanceof ServerLevel level) || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.PATH_DEBUG))
            return;
        int phase = npc.tickCount + npc.getId();
        if (phase % PARTICLE_INTERVAL == 0)
            draw(level, npc, state);
        if (phase % LOG_INTERVAL == 0 && NpcsMod.log.isInfoEnabled())
            NpcsMod.log.info(describe(npc, state));
    }

    private static void draw(ServerLevel level, EntityNPCInterface npc, NpcVehicleState state)
    {
        Path path = npc.getNavigation().getPath();
        if (path != null && !path.isDone())
        {
            int first = path.getNextNodeIndex();
            int count = path.getNodeCount() - first;
            int stride = stride(count);
            for (int node = first; node < path.getNodeCount(); node += stride)
                show(level, PATH, path.getEntityPosAtNode(npc, node).add(0D, 0.3D, 0D));
            show(level, ParticleTypes.FLAME, path.getEntityPosAtNode(npc, path.getNodeCount() - 1).add(0D, 0.6D, 0D));
        }
        Vec3 steer = state.getDriving().getSteerPoint();
        if (steer == null)
            steer = NpcDriver.of(npc).debugPoint().orElse(null);
        if (steer != null)
            show(level, ParticleTypes.HAPPY_VILLAGER, steer.add(0D, 0.8D, 0D));
    }

    /** @return how many nodes to step by so that at most {@link #MAX_MARKERS} of them are drawn */
    static int stride(int nodes)
    {
        return Math.max(1, (nodes + MAX_MARKERS - 1) / MAX_MARKERS);
    }

    private static void show(ServerLevel level, ParticleOptions particle, Vec3 position)
    {
        for (ServerPlayer player : level.players())
        {
            if (player.distanceToSqr(position) <= VIEW_DISTANCE * VIEW_DISTANCE)
                level.sendParticles(player, particle, true, position.x, position.y, position.z, 1, 0D, 0D, 0D, 0D);
        }
    }

    private static String describe(EntityNPCInterface npc, NpcVehicleState state)
    {
        Path path = npc.getNavigation().getPath();
        NpcDriving driving = state.getDriving();
        NpcFlight flight = state.getFlight();
        NpcTacticState tactic = NpcTacticState.of(npc);
        return String.format(Locale.ROOT, "NPC %s id=%d pos=%.1f,%.1f,%.1f path=%s hull=%.0f throttle=%.2f%s flight=%s terrain=%d clearance=%.1f target=%s aligned=%s tactic=%s/%s%s%s",
            npc.getName().getString(), npc.getId(), npc.getX(), npc.getY(), npc.getZ(), path == null || path.isDone() ? "none" : (path.getNodeCount() - path.getNextNodeIndex()) + " nodes",
            state.getHullYaw(), driving.getThrottle(), driving.isReversing() ? " reverse" : "", flight.state(npc), flight.getTerrain(), flight.getClearance(),
            npc.getTarget() == null ? "none" : npc.getTarget().getName().getString(), state.isAligned(), NpcTactics.of(npc), tactic.getPhase(), tactic.getCover() == null ? "" : " cover",
            NpcDriver.of(npc).describe().map(controls -> " driveable=" + controls).orElse(""));
    }
}
