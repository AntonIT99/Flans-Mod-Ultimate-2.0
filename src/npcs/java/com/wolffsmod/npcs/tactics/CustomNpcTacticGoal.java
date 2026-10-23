package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Server-side replacement for the tactical movements absent from the NeoForge Custom NPCs build. The selected movement
 * precedes its attack goals, as on Forge; equipped weapons fire independently. A Flan tactic takes precedence without
 * changing the stored Custom NPCs choice. A throwable gun's cooldown leaves movement to its melee attack. Rush and None leave the ordinary attack movement in place.
 */
public final class CustomNpcTacticGoal extends Goal
{
    private final EntityNPCInterface npc;
    private LivingEntity target;
    private int variant;
    private boolean ranged;
    private double range;
    private float health;
    private int retry;
    private int replan;
    private double angle;
    private double orbitRadius;
    private double decay;
    private int direction;
    private boolean navigating;
    private Vec3 destination;

    public CustomNpcTacticGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        health = npc.getHealth();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean ineligible()
    {
        return npc.isKilled() || npc.ais.onAttack != 0 || NpcEquipmentRangedGoal.rushes(npc) || NpcTactics.of(npc) != Tactic.CUSTOM_NPCS;
    }

    @Override
    public boolean canUse()
    {
        if (ineligible() || --retry > 0)
            return false;
        target = npc.getTarget();
        if (target == null || !target.isAlive())
            return false;
        CustomNpcTactics settings = CustomNpcTactics.of(npc);
        variant = settings.getVariant();
        ranged = NpcEquipment.fightsAtRange(npc);
        range = settings.getRange();
        destination = null;
        return switch (variant)
        {
            case CustomNpcTactics.STAGGER -> stagger();
            case CustomNpcTactics.SURROUND -> surround();
            case CustomNpcTactics.HIT_AND_RUN -> retreat();
            case CustomNpcTactics.AMBUSH -> ambush();
            case CustomNpcTactics.STALK -> !npc.isInRange(target, range);
            default -> false;
        };
    }

    private boolean fired()
    {
        return npc.getRangedTask() != null && npc.getRangedTask().hasFired();
    }

    private boolean stagger()
    {
        if (!ranged)
            return !npc.isInRange(target, range);
        if (fired())
            destination = DefaultRandomPos.getPos(npc, (int) range, 1);
        return destination != null;
    }

    private boolean surround()
    {
        retry = 10;
        if (ranged)
            range = npc.stats.ranged.getRange();
        return !npc.isInRange(target, range / 2D) && (ranged || npc.isInRange(target, range));
    }

    private boolean retreat()
    {
        if (ranged ? !fired() : npc.getHealth() == health)
            return false;
        if (npc.ais.directLOS && !npc.getSensing().hasLineOfSight(target))
            return false;
        destination = DefaultRandomPos.getPosAway(npc, 16, 7, target.position());
        return destination != null && target.distanceToSqr(destination) >= target.distanceToSqr(npc);
    }

    private boolean ambush()
    {
        if (npc.isInRange(target, range) || !npc.canNpcSee(target))
            return false;
        destination = hidden(false);
        if (destination == null)
            retry = 10;
        return destination != null;
    }

    @Override
    public boolean canContinueToUse()
    {
        if (ineligible() || !navigating || target == null || !target.isAlive() || npc.getTarget() != target || CustomNpcTactics.of(npc).getVariant() != variant)
            return false;
        return switch (variant)
        {
            case CustomNpcTactics.STAGGER -> ranged ? !npc.getNavigation().isDone() : !npc.isInRange(target, range);
            case CustomNpcTactics.SURROUND -> !npc.isInRange(target, range / 2D) && npc.isInRange(target, range * 1.5D) && !npc.isInWater();
            case CustomNpcTactics.HIT_AND_RUN -> !npc.getNavigation().isDone();
            case CustomNpcTactics.AMBUSH -> !npc.getNavigation().isDone() && !npc.isInRange(target, range) || !npc.canNpcSee(target) && (!npc.isInRange(target, range) || npc.ais.directLOS);
            case CustomNpcTactics.STALK -> !npc.isInRange(target, range);
            default -> false;
        };
    }

    @Override
    public void start()
    {
        if (target == null)
            return;
        replan = 0;
        navigating = true;
        // Custom NPCs clears its target when its ranged goal yields movement. Keep this engagement alive.
        if (npc.getTarget() == null)
            npc.setTarget(target);
        if (variant == CustomNpcTactics.SURROUND)
        {
            direction = npc.getRandom().nextInt(10) > 5 ? 1 : -1;
            decay = npc.getRandom().nextFloat() + range / 16D;
            orbitRadius = npc.distanceTo(target);
            angle = Math.atan2(npc.getZ() - target.getZ(), npc.getX() - target.getX());
        }
        else if (destination != null)
            move(destination, speed());
    }

    private double speed()
    {
        if (variant == CustomNpcTactics.AMBUSH)
            return ranged ? 1.3D : 1.2D;
        return variant == CustomNpcTactics.HIT_AND_RUN ? 1D : 1.2D;
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        npc.getLookControl().setLookAt(target, 30F, 30F);
        switch (variant)
        {
            case CustomNpcTactics.STAGGER -> staggerTick();
            case CustomNpcTactics.SURROUND -> orbit();
            case CustomNpcTactics.HIT_AND_RUN -> {
                npc.getNavigation().setSpeedModifier(npc.isInRange(target, 7D) ? 1.2D : 1D);
                if (!npc.isInRange(target, npc.stats.aggroRange) || npc.isInRange(target, range))
                    health = npc.getHealth();
            }
            case CustomNpcTactics.STALK -> stalk();
            default -> {
                // Ambush waits in cover until the enemy enters its engagement distance.
            }
        }
    }

    private void staggerTick()
    {
        if (!ranged && replan-- <= 0)
            zigzag();
    }

    private void zigzag()
    {
        Path path = npc.getNavigation().createPath(target, 0);
        replan = 10;
        if (path == null || path.getNodeCount() < range)
            return;
        var node = path.getNode(Math.min(path.getNodeCount() - 1, (int) range / 2));
        Vec3 ahead = new Vec3(node.x, node.y, node.z);
        Vec3 point = null;
        for (int attempt = 0; attempt < 10 && point == null; attempt++)
            point = DefaultRandomPos.getPosTowards(npc, (int) range, 3, ahead, Math.PI / 2D);
        move(point != null && target.distanceToSqr(point) < target.distanceToSqr(ahead) ? point : ahead, 1.3D);
    }

    private void orbit()
    {
        if (!npc.getNavigation().isDone() || !npc.onGround() || npc.isInWater())
            return;
        Vec3 point = new Vec3(target.getX() + orbitRadius * Math.cos(angle), target.getBoundingBox().maxY, target.getZ() + orbitRadius * Math.sin(angle));
        if (!move(point, 1.3D))
        {
            retry = 60;
            navigating = false;
            return;
        }
        angle += Math.toRadians(15D * direction);
        if (!ranged)
            orbitRadius = Math.max(range / 2D, orbitRadius - decay);
    }

    private void stalk()
    {
        if (replan-- > 0)
            return;
        Vec3 towardNpc = npc.getBoundingBox().getCenter().subtract(target.getEyePosition()).normalize();
        boolean lookingAway = target.getViewVector(1F).normalize().dot(towardNpc) < 0.6D;
        if (!npc.getNavigation().isDone() && lookingAway)
            return;
        if (lookingAway || npc.canNpcSee(target))
        {
            Vec3 point = hidden(lookingAway);
            if (point != null)
            {
                destination = point;
                move(point, lookingAway ? 1D : 1.33D);
                replan = 60;
            }
            else
                replan = 100;
        }
    }

    /** Searches loaded, supported hiding positions around the NPC; stalking favours progress toward the enemy. */
    @Nullable
    private Vec3 hidden(boolean approach)
    {
        for (int radius = approach ? 8 : 1; radius >= 1 && radius <= 8; radius += approach ? -1 : 1)
        {
            Vec3 best = hiddenAtRadius(radius, approach);
            if (best != null)
                return best;
        }
        return null;
    }

    @Nullable
    private Vec3 hiddenAtRadius(int radius, boolean approach)
    {
        Vec3 best = null;
        double bestDistance = approach ? npc.distanceToSqr(target) : 3D;
        for (BlockPos pos : BlockPos.betweenClosed(npc.blockPosition().offset(-radius, -2, -radius), npc.blockPosition().offset(radius, 2, radius)))
        {
            Vec3 point = hidingSpot(pos);
            if (point == null || point.equals(destination))
                continue;
            double distance = approach ? target.distanceToSqr(point) : npc.distanceToSqr(point);
            if (approach ? distance <= bestDistance : distance > bestDistance)
            {
                best = point;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Nullable
    private Vec3 hidingSpot(BlockPos pos)
    {
        if (!npc.level().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || !npc.level().getBlockState(pos).isSolidRender(npc.level(), pos)
            || npc.level().getBlockState(pos.above()).isSolidRender(npc.level(), pos.above()) || npc.level().getBlockState(pos.above(2)).isSolidRender(npc.level(), pos.above(2)))
            return null;
        Vec3 point = Vec3.atBottomCenterOf(pos.above());
        ClipContext ray = new ClipContext(target.getEyePosition(), point.add(0D, npc.getEyeHeight(), 0D), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, npc);
        return npc.level().clip(ray).getType() != HitResult.Type.MISS && npc.getNavigation().createPath(pos.above(), 0) != null ? point : null;
    }

    private boolean move(Vec3 point, double speed)
    {
        return npc.getNavigation().moveTo(point.x, point.y, point.z, speed);
    }

    @Override
    public void stop()
    {
        npc.getNavigation().stop();
        if (variant == CustomNpcTactics.SURROUND || variant == CustomNpcTactics.AMBUSH && target != null && !npc.isInRange(target, range))
            retry = 60;
        target = null;
    }
}
