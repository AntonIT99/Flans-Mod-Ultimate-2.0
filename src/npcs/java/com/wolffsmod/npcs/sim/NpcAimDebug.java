package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.wolffsmod.npcs.combat.NpcRangedAttack;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.combat.ProjectileAim;
import com.wolffsmod.npcs.network.DebugLabelPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcTactics;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shows what an NPC with the aim debug switch is aiming at: a line of dust to its target, green while it sees the target
 * and red while it does not, a blue mark where it leads the shot to, its aggro range as a yellow ring and its firing range
 * as an orange one, and a label above its head with its target, tactic and ranges. The server draws it, so every nearby
 * player sees the same picture.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcAimDebug
{
    private static final int INTERVAL = 10;
    private static final int RING_INTERVAL = 20;
    private static final double VIEW_DISTANCE = 96D;
    /** Blocks within which a player sees a mark of the rings, which reach farther than the NPC is drawn. */
    private static final double RING_VIEW_DISTANCE = 256D;
    private static final int MAX_LINE_MARKS = 64;
    private static final int MIN_RING_MARKS = 16;
    private static final int MAX_RING_MARKS = 96;
    /** Blocks between two marks of a ring, where the ring is small enough not to need fewer. */
    private static final double RING_SPACING = 3D;
    private static final String KEY = "wolffsmodnpcs.aim_debug.";
    private static final ParticleOptions CLEAR = new DustParticleOptions(new Vector3f(0.2F, 1F, 0.2F), 0.8F);
    private static final ParticleOptions BLOCKED = new DustParticleOptions(new Vector3f(1F, 0.15F, 0.15F), 0.8F);
    private static final ParticleOptions LEAD = new DustParticleOptions(new Vector3f(0.2F, 0.5F, 1F), 2F);
    private static final ParticleOptions AGGRO = new DustParticleOptions(new Vector3f(1F, 0.9F, 0.1F), 1.5F);
    private static final ParticleOptions FIRING = new DustParticleOptions(new Vector3f(1F, 0.5F, 0.05F), 1.5F);

    /** Where the NPC leads a shot to, and how many ticks the round flies there. */
    private record Lead(Vec3 point, double ticks)
    {}

    public static void tick(EntityNPCInterface npc, NpcVehicleState state)
    {
        if (!(npc.level() instanceof ServerLevel level) || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.AIM_DEBUG))
            return;
        int phase = npc.tickCount + npc.getId();
        if (phase % INTERVAL != 0)
            return;
        LivingEntity target = npc.getTarget();
        Lead lead = null;
        boolean sight = false;
        if (target != null)
        {
            Vec3 eye = npc.getEyePosition();
            Vec3 aimed = target.getBoundingBox().getCenter();
            sight = npc.getSensing().hasLineOfSight(target);
            line(level, eye, aimed, sight ? CLEAR : BLOCKED);
            lead = lead(npc, state, target, eye, aimed);
            if (lead != null)
                show(level, LEAD, lead.point(), VIEW_DISTANCE);
        }
        if (phase % RING_INTERVAL == 0)
        {
            ring(level, npc, npc.stats.aggroRange, AGGRO);
            ring(level, npc, npc.stats.ranged.getRange(), FIRING);
        }
        DebugLabelPacket label = new DebugLabelPacket(npc.getId(), lines(npc, target, sight, lead));
        for (ServerPlayer player : level.players())
        {
            if (player.distanceToSqr(npc) <= VIEW_DISTANCE * VIEW_DISTANCE)
                NpcNetwork.sendTo(player, label);
        }
    }

    /** @return where the NPC leads the round it would fire next, or null when it fires no Flan round or leads nothing */
    @Nullable
    private static Lead lead(EntityNPCInterface npc, NpcVehicleState state, LivingEntity target, Vec3 eye, Vec3 aimed)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (!options.enabled(Feature.LEAD_TARGET))
            return null;
        Optional<ProjectileShot> shot = NpcRangedAttack.preview(npc);
        if (shot.isEmpty())
            return null;
        Vec3 displacement = aimed.subtract(eye);
        double gravity = options.enabled(Feature.BALLISTIC_AIM) ? shot.get().gravity() : 0D;
        Optional<TargetMotion> motion = options.enabled(Feature.ADVANCED_LEAD) ? state.motionOf(target) : Optional.empty();
        ProjectileAim.Solution solution = motion.isPresent()
            ? ProjectileAim.solve(displacement, motion.get(), shot.get().speed(), gravity, shot.get().drag(), false)
            : ProjectileAim.solve(displacement, target.getDeltaMovement(), shot.get().speed(), gravity, shot.get().drag(), false);
        if (solution.ticks() <= 0D)
            return null;
        Vec3 offset = motion.map(tracked -> tracked.lead(solution.ticks())).orElseGet(() -> target.getDeltaMovement().scale(solution.ticks()));
        return new Lead(aimed.add(offset), solution.ticks());
    }

    private static List<Component> lines(EntityNPCInterface npc, @Nullable LivingEntity target, boolean sight, @Nullable Lead lead)
    {
        List<Component> lines = new ArrayList<>();
        if (target == null)
            lines.add(Component.translatable(KEY + "no_target"));
        else
            lines.add(Component.translatable(KEY + "target", target.getName(), decimal(Math.sqrt(npc.distanceToSqr(target))), Component.translatable(KEY + (sight ? "sight" : "no_sight"))));
        NpcTacticState tactic = NpcTacticState.of(npc);
        boolean reloading = tactic.isReloading(npc.level().getGameTime());
        lines.add(Component.translatable(KEY + "tactic", Component.translatable("wolffsmodnpcs.tuning.tactic." + NpcTactics.of(npc).name().toLowerCase(Locale.ROOT)),
            tactic.getPhase().name().toLowerCase(Locale.ROOT), Component.translatable(KEY + (reloading ? "reloading" : "ready"))));
        lines.add(Component.translatable(KEY + "ranges", npc.stats.aggroRange, npc.stats.ranged.getRange()));
        if (lead != null && target != null)
            lines.add(Component.translatable(KEY + "lead", decimal(lead.point().distanceTo(target.getBoundingBox().getCenter())), decimal(lead.ticks())));
        return lines;
    }

    private static String decimal(double value)
    {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** Marks the line between two points, at most {@link #MAX_LINE_MARKS} times and about once a block. */
    private static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle)
    {
        double length = from.distanceTo(to);
        int marks = (int) Math.max(2, Math.min(MAX_LINE_MARKS, Math.ceil(length)));
        for (int mark = 1; mark <= marks; mark++)
            show(level, particle, from.lerp(to, (double) mark / marks), VIEW_DISTANCE);
    }

    /** Marks a circle of the given radius around the NPC's feet. */
    private static void ring(ServerLevel level, EntityNPCInterface npc, double radius, ParticleOptions particle)
    {
        if (radius <= 0D)
            return;
        int marks = (int) Math.max(MIN_RING_MARKS, Math.min(MAX_RING_MARKS, Math.round(2D * Math.PI * radius / RING_SPACING)));
        double y = npc.getY() + 0.2D;
        for (int mark = 0; mark < marks; mark++)
        {
            double angle = 2D * Math.PI * mark / marks;
            show(level, particle, new Vec3(npc.getX() + Math.cos(angle) * radius, y, npc.getZ() + Math.sin(angle) * radius), RING_VIEW_DISTANCE);
        }
    }

    private static void show(ServerLevel level, ParticleOptions particle, Vec3 position, double viewDistance)
    {
        for (ServerPlayer player : level.players())
        {
            if (player.distanceToSqr(position) <= viewDistance * viewDistance)
                level.sendParticles(player, particle, true, position.x, position.y, position.z, 1, 0D, 0D, 0D, 0D);
        }
    }
}
