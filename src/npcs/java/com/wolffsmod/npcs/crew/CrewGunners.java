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
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The seat guns of a vehicle NPC that its crew man. A gun fires at the vehicle's target once a crew member sits behind it,
 * the seat gun has turned onto the target within its limits, the target is within the vehicle's firing range and nothing
 * solid stands in the way. It selects compatible ammunition from the vehicle, then the gunner, then the gun's default, without using any up, and
 * waits out the gun's reload after each item of ammunition, as a crewed driveable's seat gun does. The crew member is the
 * shooter, so its hits are credited to it. Server thread; one instance per vehicle NPC.
 */
public final class CrewGunners
{
    /** Ticks between checks whether the line of fire of a gun is clear. */
    private static final int SIGHT_INTERVAL = 5;
    /** Degrees a wanted aim may lie beyond the seat's limits and still count as within the gun's arc. */
    private static final float ARC_SLACK = 1F;
    /** Most shots one gun fires in a tick, however short its cadence. */
    private static final int MAX_SHOTS_PER_TICK = 8;
    /** Pause, in ticks, of a gun whose definition states none. */
    private static final double MIN_SHOT_DELAY = 1D;
    /** Ticks between looks at how a gun's round flies. */
    private static final int BALLISTICS_INTERVAL = 20;

    private final Map<Integer, Gun> guns = new HashMap<>();

    /** What the vehicle's guns fire at this tick, and with what. */
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
        {
            Optional<EntityNPCInterface> gunner = mount.driver() || mount.gun().isEmpty() ? Optional.empty() : NpcCrew.occupant(vehicle, mount.seat());
            gunner.ifPresent(crew -> fire(context, crew, mount));
        }
    }

    /** Whether the vehicle has a target its manned guns may fire at at all. */
    private static boolean engages(EntityNPCInterface vehicle, NpcWeaponOptions options, @Nullable LivingEntity target, NpcVehicleState state)
    {
        boolean allowed = options.enabled(Feature.MANNED_GUNS_FIRE) && options.enabled(Feature.PASSENGER_GUNS) && !options.enabled(Feature.TURRET_LOCKED);
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
        Vec3 aimPoint = NpcVehicleState.aimPoint(context.target());
        ReloadCycle cycle = FlansEntityTypes.getSeatReloadCycle(definition, mount.seat(), ammo).orElse(null);
        if (ammo.isEmpty() || cycle == null || !lay(context, gunner, mount, gun, ammo))
            return;
        for (int shot = 0; shot < MAX_SHOTS_PER_TICK && gun.cooldown < 1D; shot++)
        {
            WeaponMuzzle muzzle = FlansProjectiles.getSeatMuzzle(definition, mount.seat(), gun.sequence, state.turretAim()).orElse(null);
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
     * Lays the gun where its rounds have to go, even while it reloads, so that it is on target when it may fire again.
     *
     * @return whether it may fire now: it is ready, the target is within the vehicle's firing range and the gun points there
     */
    private static boolean lay(Context context, EntityNPCInterface gunner, WeaponMount mount, Gun gun, ItemStack ammo)
    {
        EntityNPCInterface vehicle = context.vehicle();
        NpcVehicleState state = context.state();
        Optional<WeaponMuzzle> muzzle = FlansProjectiles.getSeatMuzzle(context.definition(), mount.seat(), gun.sequence, state.turretAim());
        if (muzzle.isEmpty())
            return false;
        Vec3 lead = lead(context, gunner, mount.gun().orElseThrow(), ammo, gun, NpcModels.world(vehicle, context.model(), muzzle.get().offset(), state.getHullYaw()));
        state.leadSeat(mount.seat(), lead);
        return gun.cooldown < 1D && context.target().distanceTo(vehicle) <= vehicle.stats.ranged.getRange() && aligned(context, mount, lead);
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
    private static Vec3 lead(Context context, EntityNPCInterface gunner, IContentType weapon, ItemStack ammo, Gun gun, Vec3 origin)
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
        Vec3 toTarget = NpcVehicleState.aimPoint(context.target()).subtract(origin);
        if (Double.isNaN(gun.speed))
            return toTarget.normalize();
        boolean arc = context.options().enabled(Feature.BALLISTIC_AIM);
        return context.state().lead(toTarget, context.target(), context.options(), gun.speed, arc ? gun.gravity : 0D, arc ? gun.drag : 1D, false);
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
