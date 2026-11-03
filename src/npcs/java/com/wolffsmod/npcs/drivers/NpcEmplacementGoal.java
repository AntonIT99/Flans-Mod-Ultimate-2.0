package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.FlansEmplacements;
import com.flansmodultimate.api.driveables.IEmplacement;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.tactics.NpcCombatMovement;
import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.*;

/** Soldiers use free emplacements or deploy a carried gun for a fight, inside or outside a Teams round. Server thread. */
public final class NpcEmplacementGoal extends Goal
{
    private static final double SEARCH_RANGE = 16D;
    private static final double FIRE_RANGE = 160D;
    private static final double FIRE_CONE = Math.cos(Math.toRadians(3D));
    private static final int SEARCH_INTERVAL = 40;
    private static final int BOARD_TIMEOUT = 200;
    private static final int IDLE_TIMEOUT = 300;
    private static final int REFUSAL_TICKS = 400;

    private final EntityNPCInterface npc;
    private final Map<UUID, Long> refused = new HashMap<>();
    @Nullable
    private Entity mount;
    private long nextSearch;
    private int boarding;
    private int idle;

    public NpcEmplacementGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean enabled()
    {
        return !npc.level().isClientSide && npc.isAlive() && !npc.isKilled() && !npc.isNoAi() && npc.ais.onAttack == 0 && !CustomNpcsPlatform.frozen()
            && NpcEquipment.enabled(npc, Feature.MAN_EMPLACEMENTS) && TeamsBots.mayManEmplacements(npc) && NpcModels.flan(npc) == null && NpcCrew.seat(npc) < 0;
    }

    @Override
    public boolean canUse()
    {
        if (!enabled())
            return false;
        if (npc.getVehicle() instanceof IEmplacement)
        {
            mount = npc.getVehicle();
            return true;
        }
        LivingEntity target = npc.getTarget();
        long now = npc.level().getGameTime();
        if (npc.isPassenger() || target == null || !target.isAlive() || now < nextSearch || now < NpcDriver.of(npc).getBoardAfter() || !NpcCombatMovement.allowed(npc))
            return false;
        nextSearch = now + SEARCH_INTERVAL;
        refused.values().removeIf(until -> until <= now);
        mount = npc.level()
            .getEntities(npc, npc.getBoundingBox().inflate(SEARCH_RANGE),
                candidate -> candidate instanceof IEmplacement emplacement && candidate.isAlive() && !candidate.isVehicle() && !refused.containsKey(candidate.getUUID())
                    && !NpcDriver.of(npc).refuses(candidate.getUUID(), now) && usable(emplacement, target))
            .stream().filter(candidate -> ((IEmplacement) candidate).getEmplacementOperatorPosition(npc).filter(point -> NpcCombatMovement.permits(npc, point)).isPresent())
            .min(Comparator.comparingDouble(npc::distanceToSqr)).orElse(null);
        if (mount == null)
            mount = deploy(target).orElse(null);
        return mount != null;
    }

    private boolean usable(IEmplacement emplacement, LivingEntity target)
    {
        return emplacement.getEmplacementType().filter(type -> emplacement.hasEmplacementAmmunition() || !NpcAmmunition.emplacementTemplate(npc, type).isEmpty()).isPresent()
            && engageable(emplacement, target);
    }

    private boolean engageable(IEmplacement emplacement, LivingEntity target)
    {
        Vec3 point = target.getBoundingBox().getCenter();
        Vec3 origin = emplacement.getEmplacementOrigin();
        return target.isAlive() && origin.distanceToSqr(point) <= FIRE_RANGE * FIRE_RANGE && origin.distanceToSqr(point) >= 16D && emplacement.canEmplacementAimAt(point)
            && npc.level().clip(new ClipContext(origin, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc)).getType() == HitResult.Type.MISS;
    }

    private Optional<Entity> deploy(LivingEntity target)
    {
        double distanceSquared = npc.distanceToSqr(target);
        if (distanceSquared < 16D || distanceSquared > FIRE_RANGE * FIRE_RANGE)
            return Optional.empty();
        List<ItemStack> carried = NpcAmmunition.supplies(npc);
        ItemStack weapon = carried.stream().filter(FlansEquipment::isDeployable).findFirst().orElse(ItemStack.EMPTY);
        if (weapon.isEmpty())
            return Optional.empty();
        Vec3 toward = target.position().subtract(npc.position());
        Direction facing = Direction.fromYRot(Math.toDegrees(Math.atan2(-toward.x, toward.z)));
        for (int distance = 1; distance <= 2; distance++)
        {
            for (int dy = 0; dy >= -1; dy--)
            {
                BlockPos support = npc.blockPosition().relative(facing, distance).offset(0, dy, 0);
                Optional<IEmplacement> placed = FlansEmplacements.deploy(npc, weapon, support, facing);
                if (placed.isPresent())
                {
                    NpcAmmunition.clearUsedUp(npc, weapon);
                    npc.updateClient = true;
                    return placed.filter(Entity.class::isInstance).map(Entity.class::cast);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean canContinueToUse()
    {
        return enabled() && mount != null && mount.isAlive() && idle < IDLE_TIMEOUT && boarding < BOARD_TIMEOUT && npc.level().getGameTime() >= NpcDriver.of(npc).getBoardAfter()
            && (!mount.isVehicle() || mount.getFirstPassenger() == npc) && (!npc.isPassenger() || npc.getVehicle() == mount);
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void start()
    {
        boarding = 0;
        idle = 0;
    }

    @Override
    public void tick()
    {
        if (!(mount instanceof IEmplacement emplacement))
            return;
        if (npc.getVehicle() != mount)
        {
            board(emplacement);
            return;
        }
        npc.getNavigation().stop();
        IContentType type = emplacement.getEmplacementType().orElse(null);
        if (type == null)
            return;
        emplacement.reloadEmplacement(npc, () -> NpcAmmunition.takeEmplacementAmmo(npc, type), !NpcEquipment.enabled(npc, Feature.CONSUME_AMMO));
        LivingEntity target = npc.getTarget();
        if (target == null || !usable(emplacement, target))
        {
            idle++;
            emplacement.setEmplacementFiring(false);
            return;
        }
        idle = 0;
        Vec3 point = target.getBoundingBox().getCenter();
        emplacement.aimEmplacement(npc, point);
        boolean aligned = aligned(emplacement, target, point);
        emplacement.setEmplacementFiring(aligned && clearOfAllies(emplacement.getEmplacementOrigin(), point) && !NpcSkill.holdsFire(npc, target));
    }

    /** Within a few degrees of the target, or with the barrel line through its box, so close and moving targets are not held back. */
    private static boolean aligned(IEmplacement emplacement, LivingEntity target, Vec3 point)
    {
        Vec3 origin = emplacement.getEmplacementOrigin();
        Vec3 direction = emplacement.getEmplacementDirection();
        return direction.dot(point.subtract(origin).normalize()) >= FIRE_CONE
            || target.getBoundingBox().inflate(0.25D).clip(origin, origin.add(direction.scale(origin.distanceTo(point) + 2D))).isPresent();
    }

    private boolean clearOfAllies(Vec3 origin, Vec3 point)
    {
        return npc.level().getEntities(npc, new AABB(origin, point).inflate(0.5D), entity -> entity instanceof LivingEntity && NpcDrivers.ally(npc, entity)).stream()
            .noneMatch(ally -> ally.getBoundingBox().inflate(0.2D).clip(origin, point).isPresent());
    }

    private void board(IEmplacement emplacement)
    {
        boarding++;
        Vec3 position = emplacement.getEmplacementOperatorPosition(npc).orElse(null);
        if (position == null)
        {
            boarding = BOARD_TIMEOUT;
            return;
        }
        npc.getLookControl().setLookAt(mount, 30F, 30F);
        boolean reachable = npc.position().distanceToSqr(position) <= 2.25D || npc.getBoundingBox().inflate(1.5D).intersects(mount.getBoundingBox());
        if (reachable
            && npc.level().clip(new ClipContext(npc.getEyePosition(), emplacement.getEmplacementOrigin(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc)).getType() == HitResult.Type.MISS)
        {
            if (!npc.startRiding(mount))
                boarding = BOARD_TIMEOUT;
        }
        else if ((boarding == 1 || boarding % 20 == 0) && !npc.getNavigation().moveTo(position.x, position.y, position.z, 1D))
            boarding = BOARD_TIMEOUT;
    }

    @Override
    public void stop()
    {
        if (mount instanceof IEmplacement emplacement)
        {
            emplacement.setEmplacementFiring(false);
            // A goal that only interrupts this one leaves the gunner at the gun; it resumes from canUse.
            if (!canContinueToUse())
            {
                refused.put(mount.getUUID(), npc.level().getGameTime() + REFUSAL_TICKS);
                if (npc.getVehicle() == mount)
                    npc.stopRiding();
            }
        }
        npc.getNavigation().stop();
        mount = null;
    }
}
