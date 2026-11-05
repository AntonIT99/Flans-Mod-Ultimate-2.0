/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.*;
import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.MechaEquipmentSupport;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.raytracing.hits.DriveableHit;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.platform.item.ItemStackData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.UnaryOperator;

/** Read-only AA and driveable property inspection for addons, without creating a live vehicle. */
@ApiStatus.Experimental
public final class FlansEntityTypes
{
    /** @return authored mecha stomp cadence, or empty for another definition or a silent mecha; either side */
    public static Optional<MechaGait> getMechaGait(IContentType type)
    {
        if (!(type instanceof MechaType mecha) || mecha.getStompSound() == null || mecha.getStompSound().isBlank())
            return Optional.empty();
        return Optional.of(new MechaGait(mecha.getStompSound(), Math.max(1, mecha.getStompSoundLength()), mecha.getLegAnimSpeed() > 0F ? mecha.getLegAnimSpeed() : 0.12F, mecha.getStompRangeLower(),
            mecha.getStompRangeUpper()));
    }

    /**
     * Applies a mecha definition's landing block damage for an addon-controlled entity on the server thread.
     * Respects driveable breaking rules, mob griefing, unbreakable blocks, block entities and protection events.
     *
     * @param entity
     *            live model entity at the landing position
     * @param type
     *            selected mecha definition
     * @param force
     *            whether a carried addon enables landing block damage
     * @param descent
     *            pre-landing vertical velocity in blocks per tick, negative downward
     */
    public static void applyMechaLanding(Entity entity, IContentType type, boolean force, double descent)
    {
        if (type instanceof MechaType mecha)
            MechaEquipmentSupport.landing(entity, mecha, force, descent);
    }

    /**
     * Tests whether a definition hides all occupants at the supplied door state.
     * @param type loaded content definition
     * @param doorsOpen whether its doors are open
     * @return whether occupants, including equipment, should be hidden; false for non-driveables
     * May be called on either side after content loading.
     */
    public static boolean hidesOccupants(IContentType type, boolean doorsOpen)
    {
        return type instanceof DriveableType driveable && driveable.hidesOccupants(doorsOpen);
    }

    /**
     * Tests unconditional, door-dependent and per-seat occupant hiding after content loading, on either side.
     * @param type loaded content definition
     * @param doorsOpen current door state
     * @param seatIndex seat number from the definition, zero for the driver
     * @return whether that seat's entire occupant should be hidden; false for non-driveables
     */
    public static boolean hidesOccupants(IContentType type, boolean doorsOpen, int seatIndex)
    {
        return type instanceof DriveableType driveable && driveable.hidesOccupants(doorsOpen, seatIndex);
    }

    private FlansEntityTypes()
    {}

    /**
     * Samples block collision shapes beneath a model's authored wheels and advances its pitch and bank with the normal
     * driveable suspension smoothing and angle limits. Call once per server tick on the level thread while grounded;
     * this reads the world without moving the entity. Missing support retains the previous angle. Unsupported definitions
     * or definitions without wheels return level. Yaw and vertical movement remain with the caller's movement controller.
     *
     * @param entity
     *            entity wearing the model, used for collision context
     * @param type
     *            selected content definition
     * @param modelToWorld
     *            transforms static preview points (blocks, Y up, forward -Z, right +X) to world positions,
     *            including the current attitude, its lift, model resting height and any body offset
     * @param scale
     *            additional uniform entity scale, excluding the definition's ModelScale; must be positive and finite
     * @param current
     *            current nose-down pitch and right-down bank, in degrees, and lift in blocks
     * @return smoothed ground attitude, respecting the definition's wheel spring strength and roll permission, lifted so the
     *         wheels that bear the load rest on the terrain
     */
    public static TerrainAttitude sampleTerrainAttitude(Entity entity, IContentType type, UnaryOperator<Vec3> modelToWorld, double scale, TerrainAttitude current)
    {
        if (!Double.isFinite(scale) || scale <= 0D)
            return TerrainAttitude.LEVEL;
        return com.flansmodultimate.common.driveables.physics.ModelTerrainAttitude.sample(entity, type, modelToWorld, scale, current);
    }

    /**
     * Scales an outside velocity change using the same mass, configuration and dead zone as real driveables and AA guns.
     * Unsupported definitions and legacy knockback keep the input. Read on the level thread after content loading;
     * apply the result on the server only.
     *
     * @param type
     *            selected driveable or AA-gun definition
     * @param impulse
     *            outside velocity change in blocks per tick, excluding propulsion and gravity
     * @return the velocity change to retain
     */
    public static Vec3 scaleExternalImpulse(IContentType type, Vec3 impulse)
    {
        if (com.flansmodultimate.config.ModCommonConfig.forceLegacyVehicleKnockback())
            return impulse;
        double mass;
        if (type instanceof com.flansmodultimate.common.types.DriveableType driveable)
            mass = driveable.getImpulseMass().massKg();
        else if (type instanceof com.flansmodultimate.common.types.AAGunType gun)
            mass = gun.getImpulseMass().massKg();
        else
            return impulse;
        return com.flansmodultimate.common.driveables.physics.VehicleImpulsePhysics.scaleExternalImpulse(impulse, mass, com.flansmodultimate.config.ModCommonConfig.vehicleKnockbackReferenceMassKg());
    }

    /** @return a real driveable's turret yaw relative to its hull, in degrees, or empty for another entity; level thread, either side */
    public static OptionalDouble getTurretYaw(Entity entity)
    {
        return entity instanceof Driveable driveable ? OptionalDouble.of(driveable.getTurretYaw()) : OptionalDouble.empty();
    }

    /**
     * Inspects the first intact driveable part on a world-space line segment without applying damage. Either side, on the entity's level thread.
     *
     * @param vehicle
     *            real driveable whose hitboxes are inspected
     * @param origin
     *            start of the line in blocks
     * @param end
     *            end of the line in blocks; stop at a rider's near surface to test whether armour shields that rider
     * @return nominal protection on the first struck face, or empty for no intersection, unknown armour or another entity
     */
    public static Optional<PartArmor> getArmorAlongRay(Entity vehicle, Vec3 origin, Vec3 end)
    {
        if (!(vehicle instanceof Driveable driveable) || driveable.getConfigType() == null)
            return Optional.empty();
        return driveable.attackFromBullet(origin, end.subtract(origin)).stream().filter(DriveableHit.class::isInstance).map(DriveableHit.class::cast).findFirst()
            .flatMap(hit -> driveable.getConfigType().getPartArmor(hit.getPart().getShortName(), hit.getLocalOutwardNormal()));
    }

    /**
     * Creates impact damage attributed to a vehicle or an entity wearing a Flan damage model.
     * The source keeps the vehicle identity for kill messages and credits its controller. Server thread only.
     *
     * @param vehicle
     *            live vehicle or model wearer
     * @param controller
     *            responsible driver, or null for an unmanned vehicle
     * @return damage source to pass to the victim's hurt method
     */
    public static DamageSource collisionDamageSource(Entity vehicle, @Nullable Entity controller)
    {
        return com.flansmodultimate.common.FlanDamageSources.vehicleImpact(vehicle, controller);
    }

    /**
     * Reads ground-vehicle steering and reverse limits without creating an entity. Either side after content loading.
     * Values reflect the current force-legacy and speed-scale configuration. Legacy steering coefficients and pedal
     * response are not absolute movement limits and are deliberately not exposed as such.
     *
     * @param type
     *            selected definition
     * @return available handling, with empty fields for unsupported or unspecified properties
     */
    public static GroundHandling getGroundHandling(IContentType type)
    {
        return EntityTypePropertySupport.groundHandling(type);
    }

    /**
     * Reads how a ground vehicle speeds up, tops out and slows down without creating an entity, so that something
     * moving in its place can match a player-driven vehicle of that definition. Either side after content loading.
     *
     * @param type
     *            selected definition
     * @return the vehicle's longitudinal response, or empty for anything but a ground vehicle with a usable top speed
     */
    public static Optional<GroundPropulsion> getGroundPropulsion(IContentType type)
    {
        return GroundPropulsionProfiles.of(type);
    }

    /**
     * Reads how a vehicle turns and reverses in water, where a legacy wheeled hull steers on a circle its turn speeds set.
     * A ship filed as a plane only reports her astern share. Either side after content loading.
     *
     * @param type
     *            selected definition
     * @return available handling, with empty fields for unsupported or unspecified properties
     */
    public static GroundHandling getWaterHandling(IContentType type)
    {
        return EntityTypePropertySupport.groundHandling(type, true);
    }

    /**
     * Reads how a vehicle speeds up, tops out and slows down in water, as {@link #getGroundPropulsion} does on land: at
     * {@code MaxThrottleInWater}, with the grip and traction it keeps in water. A ship filed as a plane is read from her
     * marine profile, as she sails under it. Either side after content loading.
     *
     * @param type
     *            selected definition
     * @return the vehicle's longitudinal response in water, or empty for anything but a vehicle or a
     *         ship with a usable top speed
     */
    public static Optional<GroundPropulsion> getWaterPropulsion(IContentType type)
    {
        return GroundPropulsionProfiles.of(type, true);
    }

    /**
     * Reads how a mecha walks when a player drives it, without creating an entity. Either side after content loading.
     *
     * @param type
     *            selected definition
     * @return the mecha's walking speed and authored jump, or empty for anything but a mecha
     */
    public static Optional<MechaMobility> getMechaMobility(IContentType type)
    {
        return DriveableMobilityProfiles.mecha(type);
    }

    /**
     * Reads the highest step a driveable climbs without jumping, as its entity does: a vehicle's wheel step height, never
     * below one block for one placeable on land, or a mecha's {@code StepHeight}. Either side after content loading.
     *
     * @param type
     *            selected definition
     * @return the step height in blocks, or empty for anything but a vehicle or mecha
     */
    public static OptionalDouble getStepHeight(IContentType type)
    {
        return DriveableMobilityProfiles.stepHeight(type);
    }

    /**
     * Reads how a plane or helicopter flies when a player flies it, without creating an entity, so that something flying
     * in its place can match it. Either side after content loading. Aircraft that fly freely in six degrees are not
     * covered.
     *
     * @param type
     *            selected definition
     * @return the aircraft's flight performance, or empty for any other definition
     */
    public static Optional<FlightPerformance> getFlightPerformance(IContentType type)
    {
        return DriveableMobilityProfiles.flight(type);
    }

    /**
     * Inspects movement-engine audio independently of living-entity stat inheritance. Either side after content loading.
     * Mecha stomps retain their footstep cadence and are not engine loops.
     *
     * @param type
     *            selected driveable definition
     * @return engine sound, effective range and clip duration, or empty for missing audio, mechas and unsupported types
     */
    public static Optional<EngineSound> getEngineSound(IContentType type)
    {
        return EntityTypePropertySupport.engineSound(type);
    }

    /**
     * Inspects a vehicle/plane's idle engine loop, using its idle, start, then movement audio fallback. Either side after content loading.
     *
     * @param type
     *            selected definition
     * @return audio with effective engine range and measured or authored repeat duration, or empty for unsupported/missing audio
     */
    public static Optional<EngineSound> getEngineIdleSound(IContentType type)
    {
        return EntityTypePropertySupport.engineIdleSound(type);
    }

    /**
     * Evaluates the authored engine pitch curve using normalized movement instead of driveable throttle. Either side after content loading.
     *
     * @param type
     *            selected vehicle/plane definition, or an unsupported type to use the generic vehicle curve
     * @param movement
     *            fraction of normal movement speed, clamped to 0–1; reverse uses its positive magnitude
     * @return pitch multiplier clamped to the sound engine's 0.5–2 range
     */
    public static float getEnginePitch(IContentType type, float movement)
    {
        return EntityTypePropertySupport.enginePitch(type, movement);
    }

    /**
     * Read on either logical side after content loading; this does not mutate gameplay state.
     *
     * @param type
     *            selected AA gun, mecha, plane or vehicle definition
     * @param secondary
     *            whether to inspect the secondary weapon bank
     * @return available type properties, or empty for an unsupported definition
     */
    public static Optional<EntityTypeProperties> getProperties(IContentType type, boolean secondary)
    {
        return getProperties(type, secondary, true);
    }

    /**
     * Reads the same type properties with explicit control of mounted gun resolution, on either side after content loading.
     *
     * @param type
     *            selected AA gun, mecha, plane or vehicle definition
     * @param secondary
     *            whether to inspect the secondary weapon bank
     * @param mountedWeapons
     *            whether authored gun mounts supply weapon defaults; false uses the platform itself
     * @return available type properties, or empty for an unsupported definition
     */
    public static Optional<EntityTypeProperties> getProperties(IContentType type, boolean secondary, boolean mountedWeapons)
    {
        return EntityTypePropertySupport.read(type, secondary, mountedWeapons);
    }

    /**
     * Reads how the definition alternates between firing and reloading with the given ammunition, by the rules a placed
     * AA gun or a driveable bank follows. Either side after content loading; does not mutate gameplay state.
     *
     * @param type
     *            selected AA gun, mecha, plane or vehicle definition
     * @param secondary
     *            whether to inspect the secondary weapon bank; ignored for AA guns
     * @param ammunition
     *            ammunition being fired, whose rounds per item and reload time multiplier apply; empty counts one round
     * @return the reload cycle, or empty for an unsupported definition or a bank without a weapon
     */
    public static Optional<ReloadCycle> getReloadCycle(IContentType type, boolean secondary, ItemStack ammunition)
    {
        return EntityTypePropertySupport.reloadCycle(type, secondary, ammunition);
    }

    /**
     * Describes the firing and reloading rhythm of a seat's own gun with this ammunition, as a crewed driveable keeps it:
     * the gun's cadence, and a reload after each item of ammunition, never shorter than the pause between shots. Either
     * side after content loading; does not mutate gameplay state.
     *
     * @param type
     *            vehicle, plane or mecha definition
     * @param seat
     *            seat index, as {@link WeaponMount#seat()}
     * @param ammunition
     *            ammunition being fired, whose rounds per item set the volleys; empty counts one round
     * @return the reload cycle, or empty for another definition or a seat without a gun
     */
    public static Optional<ReloadCycle> getSeatReloadCycle(IContentType type, int seat, ItemStack ammunition)
    {
        return EntityTypePropertySupport.seatReloadCycle(type, seat, ammunition);
    }

    /**
     * The damage a vehicle or mecha whose definition declares {@code SquashMobs} deals each living entity it runs into, as its
     * own entity deals it. Either side after content loading.
     *
     * @param type
     *            vehicle or mecha definition
     * @param horizontalSpeed
     *            how fast it moves across the ground, in blocks per tick
     * @return the damage in half hearts, or empty for a definition that squashes nothing or a speed too slow to squash
     */
    public static OptionalDouble getSquashDamage(IContentType type, double horizontalSpeed)
    {
        return EntityTypePropertySupport.squashDamage(type, horizontalSpeed);
    }

    /**
     * Lists what a definition can aim: every seat of a driveable, the driver first, or the single gun of an AA gun.
     * Either side after content loading.
     *
     * @param type
     *            selected AA gun, mecha, plane or vehicle definition
     * @return immutable mounts ordered by seat index; empty for an unsupported definition
     */
    public static List<WeaponMount> getMounts(IContentType type)
    {
        return EntityTypePropertySupport.mounts(type);
    }

    /** @return the definition of a vehicle, plane or mecha entity, or empty for any other entity; either side */
    public static Optional<IDriveableType> getDriveableType(Entity entity)
    {
        return entity instanceof Driveable driveable ? Optional.ofNullable(driveable.getConfigType()) : Optional.empty();
    }

    /**
     * @return the share of its hit points a driveable has left over all of its parts, from 0 to 1, or empty for any other
     *         entity; server thread
     */
    public static OptionalDouble getHealthShare(Entity entity)
    {
        return entity instanceof Driveable driveable ? driveable.getHealthShare() : OptionalDouble.empty();
    }

    /** @return whether a driveable was destroyed and left as a wreck, which nobody can board; false for any other entity; either side */
    public static boolean isWreck(Entity entity)
    {
        return entity instanceof Driveable driveable && driveable.isWreck();
    }

    /**
     * @return the player who owns a driveable: the one who placed it, or who first locked it with a key; empty for one
     *         nobody owns and for any other entity. Server side.
     */
    public static Optional<UUID> getOwner(Entity entity)
    {
        return entity instanceof Driveable driveable ? Optional.ofNullable(driveable.getOwnerId()) : Optional.empty();
    }

    /** @return whether a driveable is locked with a key, which only its owner and the key's holder may then use; server side */
    public static boolean isLocked(Entity entity)
    {
        return entity instanceof Driveable driveable && driveable.isLocked();
    }

    /**
     * @return the share of a driveable's fuel tank still filled, from 0 to 1; empty for one without a tank, while
     *         driveables need no fuel, and for any other entity. Server side.
     */
    public static OptionalDouble getFuelShare(Entity entity)
    {
        return entity instanceof Driveable driveable ? driveable.getFuelShare() : OptionalDouble.empty();
    }

    /** @return which way a vehicle, plane or mecha points now, or empty for any other entity; server side */
    public static Optional<DriveableAttitude> getAttitude(Entity entity)
    {
        return entity instanceof Driveable driveable ? Optional.of(new DriveableAttitude(driveable.getFrontVector(), driveable.getRoofVector())) : Optional.empty();
    }

    /**
     * @return where a driveable's throttle lever stands, from -1 in full reverse to 1 at full throttle, or empty for any
     *         other entity. A rotorcraft's lever is its collective. Server side.
     */
    public static OptionalDouble getThrottle(Entity entity)
    {
        return entity instanceof Driveable driveable ? OptionalDouble.of(driveable.getThrottle()) : OptionalDouble.empty();
    }

    /** @return how a plane flies at the moment, or empty for any other entity; either side */
    public static Optional<AircraftMode> getAircraftMode(Entity entity)
    {
        if (!(entity instanceof Plane plane))
            return Optional.empty();
        return Optional.of(switch (plane.getPlaneMode())
        {
            case HELI, VTOL -> AircraftMode.ROTORCRAFT;
            case SIXDOF -> AircraftMode.SIX_DOF;
            case PLANE -> AircraftMode.FIXED_WING;
        });
    }

    /** @return whether a driveable rests on the ground, its wheels included; false for any other entity; server side */
    public static boolean isGrounded(Entity entity)
    {
        return entity instanceof Driveable driveable && driveable.isSupportedByGround();
    }

    /**
     * Repairs a driveable as a repair tool does, for someone who is no player: the damaged part nearest a point gets hit
     * points back. Server thread.
     *
     * @param from
     *            where the repair is made from, such as the repairer's eyes
     * @param amount
     *            hit points given back
     * @return whether a part was repaired; false for another entity, a driveable without damage or a destroyed one
     */
    public static boolean repair(Entity entity, Vec3 from, int amount)
    {
        if (entity.level().isClientSide)
            return false;
        return entity instanceof Driveable driveable
            ? driveable.repairNearestPart(from, amount)
            : entity instanceof com.flansmodultimate.api.parts.IFlanRepairTarget target && target.flansmodultimateRepair(from, amount);
    }

    /**
     * Places a vehicle, plane or mecha of a definition in the level, as placing its item would, with every part worn down
     * to a share of its hit points. It has no fuel and no ammunition. Server thread.
     *
     * @param type
     *            a vehicle, plane or mecha definition
     * @param position
     *            where the driveable's origin goes, in world coordinates
     * @param yaw
     *            the heading it faces, in degrees as Minecraft measures yaw
     * @param health
     *            the share of its hit points every part keeps, from above 0 to 1; a part is never destroyed by it
     * @return the driveable, or empty for another definition, on a client, or where it does not fit
     */
    public static Optional<Entity> spawnDriveable(ServerLevel level, IContentType type, Vec3 position, float yaw, float health)
    {
        return spawnDriveable(level, type, position, yaw, health, 0);
    }

    /**
     * Spawns an inert, non-repairable driveable wreck, without a death explosion or item drops.
     * Uses the server's driveable wreck lifetime and respects the definition's wreck opt-out.
     * Call on the server level thread. Position is in world blocks and yaw in degrees.
     *
     * @param level destination server level
     * @param type driveable definition
     * @param position world position in blocks
     * @param yaw heading in degrees
     * @param paintjob definition paintjob index
     * @return the wreck, or empty if disabled, unsupported or placement failed
     */
    public static Optional<Entity> spawnDriveableWreck(ServerLevel level, IContentType type, Vec3 position, float yaw, int paintjob)
    {
        if (com.flansmodultimate.config.ModCommonConfig.driveableWreckLifetimeTicks() <= 0)
            return Optional.empty();
        Optional<Entity> spawned = spawnDriveable(level, type, position, yaw, 1F, paintjob);
        if (spawned.isEmpty())
            return Optional.empty();
        Driveable driveable = (Driveable) spawned.get();
        if (driveable.makeReplacementWreck())
            return spawned;
        driveable.discard();
        return Optional.empty();
    }

    /**
     * As {@link #spawnDriveable(ServerLevel, IContentType, Vec3, float, float)}, painted with one of the definition's paintjobs.
     *
     * @param paintjob
     *            the paintjob id, 0 for the default one
     */
    public static Optional<Entity> spawnDriveable(ServerLevel level, IContentType type, Vec3 position, float yaw, float health, int paintjob)
    {
        if (!(type.getItem() instanceof DriveableItem<?, ?> item))
            return Optional.empty();
        ItemStack source = new ItemStack(item);
        if (paintjob != 0)
            ItemStackData.update(source, tag -> tag.putInt(IPaintableItem.NBT_PAINTJOB_ID, paintjob));
        float driveableYaw = LegacyDriveableCoordinates.driveableYawFromRenderedForward(yaw, type instanceof PlaneType);
        Driveable driveable = item.spawnDriveable(level, position.x, position.y, position.z, driveableYaw, null, source);
        if (driveable == null)
            return Optional.empty();
        driveable.wearDown(health);
        return Optional.of(driveable);
    }
}
