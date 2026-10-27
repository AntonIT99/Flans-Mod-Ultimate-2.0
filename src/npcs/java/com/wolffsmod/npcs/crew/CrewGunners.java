package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.ReloadCycle;
import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileParameters;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.combat.NpcMuzzleFlashes;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.MountMotion;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The seat guns of a vehicle NPC that its crew man, or that aim and fire by themselves in seats nobody sits in, as the
 * vehicle's switches say. While the vehicle fights, each gun picks its own target within its own arc, whatever the turret
 * and the other guns fire at: the nearest enemy of the vehicle it can reach. A gun fires once it has turned onto its
 * target within its limits, the target is within the vehicle's firing range and nothing solid stands in the way. It
 * selects compatible ammunition from the vehicle, then the gunner, then the gun's default, without using any up, and
 * waits out the gun's reload after each item of ammunition, as a crewed driveable's seat gun does. The crew member is the
 * shooter, so its hits are credited to it; an unmanned gun's are credited to the vehicle. Server thread; one instance per
 * vehicle NPC.
 */
public final class CrewGunners
{
    /** Ticks between checks whether the line of fire of a gun is clear. */
    private static final int SIGHT_INTERVAL = 5;
    /** Ticks between looks for what a gun fires at. */
    private static final int TARGET_INTERVAL = 10;
    /** Most enemies, nearest first, whose line of fire one look for a gun's target checks. */
    private static final int MAX_CANDIDATES = 6;
    /** Degrees a wanted aim may lie beyond the seat's limits and still count as within the gun's arc. */
    private static final float ARC_SLACK = 1F;
    /** Most shots one gun fires in a tick, however short its cadence. */
    private static final int MAX_SHOTS_PER_TICK = 8;
    /** Pause, in ticks, of a gun whose definition states none. */
    private static final double MIN_SHOT_DELAY = 1D;
    /** Ticks between looks at how a gun's round flies. */
    private static final int BALLISTICS_INTERVAL = 20;

    private final Map<Integer, Gun> guns = new HashMap<>();

    /**
     * What the vehicle's guns fire at this tick, and with what.
     *
     * @param target
     *            the vehicle's own target
     */
    private record Context(EntityNPCInterface vehicle, IContentType definition, FlanModelEntity model, LivingEntity target, NpcVehicleState state, NpcWeaponOptions options)
    {}

    /** What one seat gun is doing. */
    private static final class Gun
    {
        /** Ticks until the gun may fire again; below 1, it fires within this tick. */
        private double cooldown;
        /** Rounds fired since the last reload, and the trigger index that takes the barrels in turn. */
        private int fired;
        private int sequence;
        private int sightCheckedAt = Integer.MIN_VALUE;
        private boolean clear;
        /** What the gun picked to fire at, when it last looked, and when it last looked whether its line to it is clear; null while it bears on nothing. */
        @Nullable
        private LivingEntity target;
        private int chosenAt = Integer.MIN_VALUE;
        private int reachCheckedAt = Integer.MIN_VALUE;
        /** What the gun was laid on last, whose line of fire {@link #clear} tells. */
        @Nullable
        private LivingEntity laidOn;
        /** How the gun's round flies, looked up every so often; no speed while it is not known. */
        private int ballisticsAt = Integer.MIN_VALUE;
        private double speed = Double.NaN;
        private double gravity;
        private double drag = 1D;
    }

    /** Runs once per server tick of the vehicle, after its seat guns have been aimed. */
    public void tick(EntityNPCInterface vehicle)
    {
        guns.values().forEach(gun -> gun.cooldown = Math.max(0D, gun.cooldown - 1D));
        NpcWeaponOptions options = ((NpcWeaponSettings) vehicle.stats).wolffsmodnpcsWeaponOptions();
        FlanModelEntity model = NpcModels.flan(vehicle);
        IContentType definition = model == null ? null : model.getInfoType();
        LivingEntity target = vehicle.getTarget();
        NpcVehicleState state = NpcVehicleState.of(vehicle);
        if (definition == null || !engages(vehicle, options, target, state))
            return;
        Context context = new Context(vehicle, definition, model, target, state, options);
        for (WeaponMount mount : state.crewSeats(vehicle))
            gunner(vehicle, options, mount).ifPresent(gunner -> fire(context, gunner, mount));
    }

    /**
     * @return who fires the seat's gun: the crew member sitting behind it, or the vehicle itself for a seat nobody sits in
     *         while its guns fire unmanned; none for a seat without a gun of its own, or one another rider sits in
     */
    private static Optional<EntityNPCInterface> gunner(EntityNPCInterface vehicle, NpcWeaponOptions options, WeaponMount mount)
    {
        if (mount.driver() || mount.gun().isEmpty())
            return Optional.empty();
        Optional<EntityNPCInterface> crew = NpcCrew.occupant(vehicle, mount.seat());
        if (crew.isPresent())
            return options.enabled(Feature.MANNED_GUNS_FIRE) ? crew : Optional.empty();
        return options.enabled(Feature.UNMANNED_GUNS_FIRE) && !NpcCrew.occupied(vehicle, mount.seat()) ? Optional.of(vehicle) : Optional.empty();
    }

    /** Whether the vehicle has a target its seat guns may fire at at all. */
    private static boolean engages(EntityNPCInterface vehicle, NpcWeaponOptions options, @Nullable LivingEntity target, NpcVehicleState state)
    {
        boolean anyFire = options.enabled(Feature.MANNED_GUNS_FIRE) || options.enabled(Feature.UNMANNED_GUNS_FIRE);
        boolean allowed = anyFire && options.enabled(Feature.PASSENGER_GUNS) && !options.enabled(Feature.TURRET_LOCKED);
        return allowed && target != null && target.isAlive() && target.level() == vehicle.level() && vehicle.isAlive() && !vehicle.isKilled() && !state.getFlight().holdsFire();
    }

    private void fire(Context context, EntityNPCInterface gunner, WeaponMount mount)
    {
        EntityNPCInterface vehicle = context.vehicle();
        IContentType definition = context.definition();
        NpcVehicleState state = context.state();
        Gun gun = guns.computeIfAbsent(mount.seat(), seat -> new Gun());
        IContentType weapon = mount.gun().orElseThrow();
        ItemStack ammo = NpcAmmunition.crewGunTemplate(vehicle, gunner, weapon);
        LivingEntity target = target(context, mount, gun);
        if (target != gun.laidOn)
        {
            gun.laidOn = target;
            gun.sightCheckedAt = Integer.MIN_VALUE;
        }
        Vec3 aimPoint = NpcVehicleState.aimPoint(target);
        ReloadCycle cycle = FlansEntityTypes.getSeatReloadCycle(definition, mount.seat(), ammo).orElse(null);
        if (ammo.isEmpty() || cycle == null || !lay(context, gunner, mount, gun, ammo, target))
            return;
        for (int shot = 0; shot < MAX_SHOTS_PER_TICK && gun.cooldown < 1D; shot++)
        {
            WeaponMuzzle muzzle = FlansProjectiles.getSeatMuzzle(definition, mount.seat(), gun.sequence, state.turretAim(), state.seatAim(mount.seat())).orElse(null);
            Vec3 origin = muzzle == null ? null : NpcModels.world(vehicle, context.model(), muzzle.offset(), state.getHullYaw());
            if (origin == null || !clear(vehicle, gun, origin, aimPoint) || !launch(context, gunner, mount, ammo, gun, origin, muzzle))
                return;
            effects(context, gunner, weapon, muzzle, origin, state.seatFacing(mount), shot == 0);
            gun.sequence = gun.sequence >= Integer.MAX_VALUE / 2 ? 0 : gun.sequence + 1;
            gun.fired++;
            if (gun.fired >= cycle.volleys())
            {
                gun.fired = 0;
                gun.cooldown += cycle.reloadTicks();
            }
            else
                gun.cooldown += Math.max(cycle.shotDelay(), MIN_SHOT_DELAY / MAX_SHOTS_PER_TICK);
        }
    }

    /**
     * @return what the gun fires at, chosen as {@link #choose} says: looked for again every so often, more often while it
     *         bears on nothing, and at once when its target dies or the vehicle's movement takes it out of the gun's reach.
     *         While it bears on nothing it is laid on the vehicle's target, ready for when the hull or turret turns.
     */
    private static LivingEntity target(Context context, WeaponMount mount, Gun gun)
    {
        if (!context.options().enabled(Feature.GUNNER_TARGETS))
            return context.target();
        EntityNPCInterface vehicle = context.vehicle();
        NpcVehicleState state = context.state();
        Vec3 origin = FlansProjectiles.getSeatMuzzle(context.definition(), mount.seat(), gun.sequence, state.turretAim(), state.seatAim(mount.seat()))
            .map(muzzle -> NpcModels.world(vehicle, context.model(), muzzle.offset(), state.getHullYaw())).orElse(null);
        LivingEntity current = gun.target;
        boolean lost = current != null && (origin == null || !fightable(vehicle, current) || !reachable(context, mount, gun, origin, current));
        int interval = current == null ? SIGHT_INTERVAL : TARGET_INTERVAL;
        if (lost || gun.chosenAt == Integer.MIN_VALUE || vehicle.tickCount - gun.chosenAt >= interval || vehicle.tickCount < gun.chosenAt)
        {
            gun.chosenAt = vehicle.tickCount;
            gun.reachCheckedAt = vehicle.tickCount;
            LivingEntity kept = lost ? null : current;
            gun.target = origin == null ? null : choose(context, mount, origin, kept);
        }
        return gun.target == null ? context.target() : gun.target;
    }

    /**
     * Whether the gun still reaches its target as the hull and turret stand now: within its arc and range, looked at every
     * tick, and with a clear line to it, looked at every few ticks.
     */
    private static boolean reachable(Context context, WeaponMount mount, Gun gun, Vec3 origin, LivingEntity target)
    {
        if (!inArc(context, mount, origin, target))
            return false;
        int now = context.vehicle().tickCount;
        if (gun.reachCheckedAt != Integer.MIN_VALUE && now - gun.reachCheckedAt < SIGHT_INTERVAL && now >= gun.reachCheckedAt)
            return true;
        gun.reachCheckedAt = now;
        return lineClear(context.vehicle(), origin, NpcVehicleState.aimPoint(target));
    }

    /**
     * Each gun chooses for itself, whatever the turret and the other guns fire at: it keeps what it fires at while it bears
     * on it, and otherwise takes the nearest enemy of the vehicle it bears on, the vehicle's own target among them.
     *
     * @param origin
     *            where the gun's rounds leave from
     * @return what the gun fires at; null while it bears on nothing
     */
    @Nullable
    private static LivingEntity choose(Context context, WeaponMount mount, Vec3 origin, @Nullable LivingEntity last)
    {
        EntityNPCInterface vehicle = context.vehicle();
        // Staying on its target spares the gun turning away mid-burst.
        if (last != null && bears(context, mount, origin, last))
            return last;
        double range = vehicle.stats.ranged.getRange();
        List<LivingEntity> candidates = new ArrayList<>(vehicle.level().getEntitiesOfClass(LivingEntity.class, vehicle.getBoundingBox().inflate(range), candidate -> candidate != context.target()
            && candidate != last && candidate.getRootVehicle() != vehicle.getRootVehicle() && fightable(vehicle, candidate) && inArc(context, mount, origin, candidate)));
        if (context.target() != last && inArc(context, mount, origin, context.target()))
            candidates.add(context.target());
        candidates.sort(Comparator.comparingDouble(vehicle::distanceToSqr));
        NPCAttackSelector enemies = new NPCAttackSelector(vehicle);
        int checked = 0;
        for (LivingEntity candidate : candidates)
        {
            // The vehicle's own rules of whom it attacks, which may look for a line of sight from it; its target it attacks already.
            if (checked < MAX_CANDIDATES && (candidate == context.target() || enemies.isEntityApplicable(candidate)))
            {
                checked++;
                if (lineClear(vehicle, origin, NpcVehicleState.aimPoint(candidate)))
                    return candidate;
            }
        }
        return null;
    }

    /** Whether the gun bears on an entity: within the vehicle's firing range, within its arc and with nothing solid in the way. */
    private static boolean bears(Context context, WeaponMount mount, Vec3 origin, LivingEntity entity)
    {
        return inArc(context, mount, origin, entity) && lineClear(context.vehicle(), origin, NpcVehicleState.aimPoint(entity));
    }

    /** Whether an entity lies within the vehicle's firing range and within the arc the seat gun turns through as the hull and turret stand. */
    private static boolean inArc(Context context, WeaponMount mount, Vec3 origin, LivingEntity entity)
    {
        EntityNPCInterface vehicle = context.vehicle();
        if (entity.distanceTo(vehicle) > vehicle.stats.ranged.getRange())
            return false;
        NpcVehicleState state = context.state();
        float frame = state.getHullYaw() + (mount.onTurret() ? state.turretAim().yaw() : 0F);
        MountAim wanted = MountMotion.relative(NpcVehicleState.aimPoint(entity).subtract(origin), frame);
        return MountMotion.error(mount.clamp(wanted), wanted) <= ARC_SLACK;
    }

    /** Whether an entity can still be fought: alive, not knocked out and in the vehicle's world. */
    private static boolean fightable(EntityNPCInterface vehicle, LivingEntity entity)
    {
        return entity.isAlive() && entity.level() == vehicle.level() && !(entity instanceof EntityNPCInterface npc && npc.isKilled());
    }

    private static boolean lineClear(EntityNPCInterface vehicle, Vec3 origin, Vec3 aimPoint)
    {
        return vehicle.level().clip(new ClipContext(origin, aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle)).getType() == HitResult.Type.MISS;
    }

    /**
     * Lays the gun where its rounds have to go, even while it reloads, so that it is on target when it may fire again.
     *
     * @return whether it may fire now: it is ready, the target is within the vehicle's firing range and the gun points there
     */
    private static boolean lay(Context context, EntityNPCInterface gunner, WeaponMount mount, Gun gun, ItemStack ammo, LivingEntity target)
    {
        EntityNPCInterface vehicle = context.vehicle();
        NpcVehicleState state = context.state();
        Optional<WeaponMuzzle> muzzle = FlansProjectiles.getSeatMuzzle(context.definition(), mount.seat(), gun.sequence, state.turretAim(), state.seatAim(mount.seat()));
        if (muzzle.isEmpty())
            return false;
        Vec3 lead = lead(context, gunner, mount.gun().orElseThrow(), ammo, gun, NpcModels.world(vehicle, context.model(), muzzle.get().offset(), state.getHullYaw()), target);
        state.leadSeat(mount.seat(), lead);
        return gun.cooldown < 1D && target.distanceTo(vehicle) <= vehicle.stats.ranged.getRange() && aligned(context, mount, lead);
    }

    /**
     * Whether the seat gun points where its rounds have to go, turned within its limits; without Fire only when aligned, it
     * only has to be able to turn there.
     *
     * @param lead
     *            the world direction the rounds have to leave in
     */
    private static boolean aligned(Context context, WeaponMount mount, Vec3 lead)
    {
        NpcVehicleState state = context.state();
        float frame = state.getHullYaw() + (mount.onTurret() ? state.turretAim().yaw() : 0F);
        MountAim wanted = MountMotion.relative(lead, frame);
        if (MountMotion.error(mount.clamp(wanted), wanted) > ARC_SLACK)
            return false;
        return !context.options().enabled(Feature.FIRE_WHEN_ALIGNED) || state.seatPoints(context.vehicle(), mount, lead);
    }

    /** @return the world direction the gun's rounds have to leave the muzzle in to meet the target, led as the vehicle leads its own shots */
    private static Vec3 lead(Context context, EntityNPCInterface gunner, IContentType weapon, ItemStack ammo, Gun gun, Vec3 origin, LivingEntity target)
    {
        EntityNPCInterface vehicle = context.vehicle();
        if (vehicle.tickCount - gun.ballisticsAt >= BALLISTICS_INTERVAL || vehicle.tickCount < gun.ballisticsAt)
        {
            gun.ballisticsAt = vehicle.tickCount;
            ProjectileShot shot = FlansProjectiles.prepare(gunner, ammo, ItemStack.EMPTY, weapon, null, false, new ProjectileParameters(gun.fired, true, 0F, 0F, 1F)).orElse(null);
            gun.speed = shot == null ? Double.NaN : shot.speed();
            gun.gravity = shot == null ? 0D : shot.gravity();
            gun.drag = shot == null ? 1D : shot.drag();
        }
        Vec3 toTarget = NpcVehicleState.aimPoint(target).subtract(origin);
        if (Double.isNaN(gun.speed))
            return toTarget.normalize();
        boolean arc = context.options().enabled(Feature.BALLISTIC_AIM);
        return context.state().lead(toTarget, target, context.options(), gun.speed, arc ? gun.gravity : 0D, arc ? gun.drag : 1D, false);
    }

    /** Whether nothing solid stands between the gun and the target, looked at every few ticks. */
    private static boolean clear(EntityNPCInterface vehicle, Gun gun, Vec3 origin, Vec3 aimPoint)
    {
        if (gun.sightCheckedAt == Integer.MIN_VALUE || vehicle.tickCount - gun.sightCheckedAt >= SIGHT_INTERVAL || vehicle.tickCount < gun.sightCheckedAt)
        {
            gun.sightCheckedAt = vehicle.tickCount;
            gun.clear = vehicle.level().clip(new ClipContext(origin, aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle)).getType() == HitResult.Type.MISS;
        }
        return gun.clear;
    }

    /** Fires one round of the gun along its barrel, as a player's leaves, which was laid where the round has to go. */
    private static boolean launch(Context context, EntityNPCInterface gunner, WeaponMount mount, ItemStack ammo, Gun gun, Vec3 origin, WeaponMuzzle muzzle)
    {
        ProjectileShot shot = FlansProjectiles.prepare(gunner, ammo, ItemStack.EMPTY, mount.gun().orElseThrow(), null, false, new ProjectileParameters(gun.fired, true, 0F, 0F, 1F)).orElse(null);
        if (shot == null)
            return false;
        Vec3 direction = context.state().seatBarrelDirection(context.vehicle(), mount);
        if (shot.launch(origin, direction, false).isEmpty())
            return false;
        NpcMuzzleFlashes.fired(context.vehicle(), muzzle, direction, context.state().getHullYaw(), false);
        return true;
    }

    private static void effects(Context context, EntityNPCInterface gunner, IContentType weapon, WeaponMuzzle muzzle, Vec3 origin, float facing, boolean first)
    {
        NpcWeaponOptions options = context.options();
        if (first && options.enabled(Feature.FIRE_SOUNDS))
            FlansProjectiles.playShootSound(gunner, weapon, ItemStack.EMPTY, false);
        if (options.enabled(Feature.SHOOT_PARTICLES))
            FlansProjectiles.shootParticles(context.vehicle(), muzzle, origin, facing, NpcModels.scale(context.vehicle()));
    }
}
