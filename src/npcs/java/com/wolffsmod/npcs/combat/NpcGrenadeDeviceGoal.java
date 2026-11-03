package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.projectiles.FlansGrenadeDevices;
import com.flansmodultimate.api.projectiles.GrenadeDeviceProperties;
import com.flansmodultimate.api.projectiles.IProximityTarget;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcRepairs;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcCombatMovement;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Plants carried mines and remote charges in the enemy's approach, withdraws beyond their blast, and detonates owned
 * charges only around a visible enemy with the thrower and allies clear. Deploys needed supply bags and visits friendly
 * bags for healing, effects or finite ammunition. Never loads chunks or ignores explicit movement restrictions.
 */
public final class NpcGrenadeDeviceGoal extends Goal
{
    private static final int SCAN_TICKS = 20;
    private static final int DEPLOY_TICKS = 600;
    private static final int SUPPLY_TICKS = 100;
    private static final int ACTION_TICKS = 160;
    private static final double SEARCH = 32D;
    private static final double BAG_SEARCH = 12D;
    private static final double PLANT_DISTANCE = 3D;
    private static final int MAX_MINES = 3;

    private final EntityNPCInterface npc;
    private long nextScan;
    private long nextDeploy;
    private long nextSupply;
    private long until;
    @Nullable
    private Entity bag;
    @Nullable
    private Vec3 escape;
    @Nullable
    private BlockPos support;
    private ItemStack planting = ItemStack.EMPTY;
    private final Map<UUID, Long> refused = new HashMap<>();

    public NpcGrenadeDeviceGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean enabled()
    {
        return !npc.level().isClientSide && npc.isAlive() && !npc.isKilled() && !npc.isNoAi() && !CustomNpcsPlatform.frozen() && !npc.isPassenger() && NpcEquipment.enabled(npc, Feature.GRENADES)
            && TacticKind.of(npc) == TacticKind.STANDARD && !NpcRepairs.active(npc) && !NpcThrowGoal.throwing(npc);
    }

    @Override
    public boolean canUse()
    {
        if (!enabled())
            return false;
        long now = npc.level().getGameTime();
        if (now < nextScan)
            return false;
        nextScan = now + SCAN_TICKS;
        refused.values().removeIf(time -> time <= now);
        List<Entity> devices = FlansGrenadeDevices.find(npc.level(), npc.getBoundingBox().inflate(SEARCH));
        detonateCharges(devices);
        clearPlan();
        if (avoidMine(devices) || seekBag(devices, now) || now >= nextDeploy && planDeployment(devices))
        {
            until = now + ACTION_TICKS;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse()
    {
        return enabled() && npc.level().getGameTime() < until && (bag != null && !bag.isRemoved() && NpcDeviceSupplies.needs(npc, bag) || !planting.isEmpty() || escape != null);
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        if (!planting.isEmpty())
        {
            deploy();
            return;
        }
        if (bag != null)
        {
            if (npc.distanceToSqr(bag) <= 9D && npc.hasLineOfSight(bag))
            {
                if (NpcDeviceSupplies.use(npc, bag))
                    npc.swing(InteractionHand.MAIN_HAND);
                nextSupply = npc.level().getGameTime() + SUPPLY_TICKS;
                bag = null;
                until = 0L;
            }
            else
                move(bag.position());
            return;
        }
        if (escape != null)
        {
            if (npc.position().distanceToSqr(escape) <= 1D)
                escape = null;
            else
                move(escape);
        }
    }

    private void move(Vec3 destination)
    {
        if (!NpcCombatMovement.permits(npc, destination) || !npc.getNavigation().moveTo(destination.x, destination.y, destination.z, 1D))
            until = 0L;
    }

    private boolean avoidMine(List<Entity> devices)
    {
        for (Entity device : devices)
        {
            var properties = FlansGrenadeDevices.properties(device).orElseThrow();
            double clearance = Math.max(Math.max(properties.livingTrigger(), properties.driveableTrigger()) + 2D, hazard(device) + 2D);
            if (!properties.mine() || !friendly(device) || npc.distanceToSqr(device) >= clearance * clearance)
                continue;
            Vec3 away = npc.position().subtract(device.position()).multiply(1D, 0D, 1D).normalize();
            if (away.lengthSqr() < 0.01D)
                away = npc.getLookAngle().multiply(-1D, 0D, -1D).normalize();
            Vec3 point = npc.position().add(away.scale(clearance + 1D));
            if (reachable(point))
            {
                escape = point;
                return true;
            }
        }
        return false;
    }

    private boolean seekBag(List<Entity> devices, long now)
    {
        if (now < nextSupply)
            return false;
        bag = devices.stream().filter(device -> npc.distanceToSqr(device) <= BAG_SEARCH * BAG_SEARCH && friendly(device) && !refused.containsKey(device.getUUID()))
            .filter(device -> NpcDeviceSupplies.needs(npc, device)).sorted(Comparator.comparingDouble(npc::distanceToSqr))
            .filter(device -> npc.distanceToSqr(device) <= 9D || reachable(device.position())).findFirst().orElse(null);
        return bag != null;
    }

    private boolean friendly(Entity device)
    {
        return FlansGrenadeDevices.ownedBy(device, npc) || FlansGrenadeDevices.owner(device).filter(owner -> NpcDrivers.ally(npc, owner)).isPresent();
    }

    private boolean planDeployment(List<Entity> devices)
    {
        for (ItemStack stack : NpcAmmunition.supplies(npc))
        {
            Optional<GrenadeDeviceProperties> properties = FlansGrenadeDevices.properties(stack);
            if (properties.isEmpty())
                continue;
            if (properties.get().supplyBag() ? planBag(stack, devices) : planTrap(stack, properties.get(), devices))
            {
                planting = stack;
                return true;
            }
        }
        return false;
    }

    private boolean planBag(ItemStack stack, List<Entity> devices)
    {
        if (devices.stream().anyMatch(device -> npc.distanceToSqr(device) <= BAG_SEARCH * BAG_SEARCH && friendly(device) && NpcDeviceSupplies.needs(npc, device)))
            return false;
        // Nearby allies may be moving away or fighting, so deploy for a recipient that will actually use the bag.
        return NpcDeviceSupplies.needs(npc, stack) && ground(npc.position().add(npc.getLookAngle().multiply(1D, 0D, 1D).normalize()));
    }

    private boolean planTrap(ItemStack stack, GrenadeDeviceProperties properties, List<Entity> devices)
    {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive() || npc.ais.onAttack != 0 || NpcDrivers.holdsFire(npc) || NpcCrew.holdsFire(npc) || !npc.hasLineOfSight(target) || !target.onGround()
            || !targetForMine(properties, target))
            return false;
        long existing = devices.stream().filter(device -> FlansGrenadeDevices.ownedBy(device, npc))
            .filter(device -> FlansGrenadeDevices.properties(device).map(value -> properties.remote() ? value.remote() : value.mine()).orElse(false)).count();
        double radius = NpcThrowing.hazard(stack);
        if (existing >= (properties.remote() ? 1 : MAX_MINES) || npc.distanceTo(target) < radius + 10D || npc.distanceTo(target) > SEARCH || radius <= 0D)
            return false;
        double trigger = Math.max(properties.livingTrigger(), properties.driveableTrigger());
        if (trigger >= PLANT_DISTANCE - 0.5D && properties.armingTicks() < 20D + (trigger - PLANT_DISTANCE + 1D) / 0.15D)
            return false;
        Vec3 direction = target.position().subtract(npc.position()).multiply(1D, 0D, 1D).normalize();
        Vec3 point = npc.position().add(direction.scale(PLANT_DISTANCE));
        Vec3 withdrawal = npc.position().subtract(direction.scale(Math.max(radius, trigger) + 3D));
        if (alliesInDanger(point, Math.max(radius, trigger) + 2D) || !reachable(withdrawal) || !ground(point))
            return false;
        escape = withdrawal;
        return true;
    }

    static boolean targetForMine(GrenadeDeviceProperties properties, LivingEntity target)
    {
        Entity ride = target.getVehicle();
        boolean vehicle = vehicleTarget(target) || ride != null && vehicleTarget(ride);
        return properties.remote() || (vehicle ? properties.driveableTrigger() > 0D : properties.livingTrigger() > 0D);
    }

    private static boolean vehicleTarget(Entity entity)
    {
        return entity instanceof IProximityTarget proximity && proximity.flansmodultimateDriveableProximityTarget() || entity instanceof ISeat seat && seat.getDriveable() != null
            || FlansEntityTypes.getDriveableType(entity).isPresent();
    }

    private boolean ground(Vec3 point)
    {
        var hit = npc.level().clip(new ClipContext(point.add(0D, 1D, 0D), point.add(0D, -2D, 0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != net.minecraft.core.Direction.UP || hit.getLocation().distanceToSqr(point) > 4D)
            return false;
        support = hit.getBlockPos();
        return true;
    }

    private boolean reachable(Vec3 point)
    {
        if (!NpcCombatMovement.permits(npc, point))
            return false;
        var path = npc.getNavigation().createPath(BlockPos.containing(point), 0);
        return path != null && path.canReach();
    }

    private void deploy()
    {
        ItemStack stack = planting;
        planting = ItemStack.EMPTY;
        if (support == null || NpcAmmunition.supplies(npc).stream().noneMatch(carried -> carried == stack))
        {
            clearPlan();
            return;
        }
        boolean trap = FlansGrenadeDevices.properties(stack).map(value -> !value.supplyBag()).orElse(false);
        Vec3 point = Vec3.atBottomCenterOf(support.above());
        if (trap && alliesInDanger(point, NpcThrowing.hazard(stack) + 2D))
        {
            clearPlan();
            return;
        }
        Vec3 facing = point.subtract(npc.position()).multiply(1D, 0D, 1D);
        if (facing.lengthSqr() < 0.01D)
            facing = new Vec3(1D, 0D, 0D);
        Optional<Entity> placed = FlansGrenadeDevices.place(npc, stack, support, facing, NpcEquipment.enabled(npc, Feature.THROW_SOUNDS));
        if (placed.isEmpty())
        {
            clearPlan();
            nextDeploy = npc.level().getGameTime() + SCAN_TICKS * 2L;
            return;
        }
        if (NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            NpcAmmunition.consume(npc, stack);
        if (!trap && NpcDeviceSupplies.use(npc, placed.get()))
            nextSupply = npc.level().getGameTime() + SUPPLY_TICKS;
        npc.swing(InteractionHand.MAIN_HAND);
        npc.updateClient = true;
        nextDeploy = npc.level().getGameTime() + DEPLOY_TICKS;
    }

    private void detonateCharges(List<Entity> devices)
    {
        if (npc.ais.onAttack != 0 || NpcDrivers.holdsFire(npc) || NpcCrew.holdsFire(npc))
            return;
        for (Entity device : devices)
            if (shouldDetonate(device) && FlansGrenadeDevices.detonate(device, npc))
                npc.swing(InteractionHand.MAIN_HAND);
    }

    private boolean shouldDetonate(Entity device)
    {
        if (!FlansGrenadeDevices.canDetonate(device, npc))
            return false;
        double radius = hazard(device);
        if (radius <= 0D || npc.distanceTo(device) < radius + 2D || alliesInDanger(device.position(), radius + 2D))
            return false;
        return !npc.level().getEntitiesOfClass(LivingEntity.class, device.getBoundingBox().inflate(Math.min(radius, 8D)),
            other -> other.isAlive() && other.distanceToSqr(device) <= radius * radius && NpcDrivers.hostile(npc, other) && npc.hasLineOfSight(other)).isEmpty();
    }

    private boolean alliesInDanger(Vec3 point, double radius)
    {
        return !npc.level().getEntities(npc, new net.minecraft.world.phys.AABB(point, point).inflate(radius),
            other -> other.isAlive() && other.getBoundingBox().inflate(radius).contains(point) && friendlyOccupant(other)).isEmpty();
    }

    private boolean friendlyOccupant(Entity entity)
    {
        return NpcDrivers.ally(npc, entity) || entity.getPassengers().stream().anyMatch(this::friendlyOccupant);
    }

    private static double hazard(Entity device)
    {
        return FlansGrenadeDevices.radii(device).map(NpcBlastSafety::reach).orElse(0D);
    }

    private void clearPlan()
    {
        planting = ItemStack.EMPTY;
        support = null;
        escape = null;
        bag = null;
    }

    @Override
    public void stop()
    {
        if (bag != null)
            refused.put(bag.getUUID(), npc.level().getGameTime() + SUPPLY_TICKS);
        clearPlan();
        npc.getNavigation().stop();
    }
}
