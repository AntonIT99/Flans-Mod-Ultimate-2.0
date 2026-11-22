package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansModEntities;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.damage.AmmoRackCookOff;
import com.flansmodultimate.common.driveables.damage.VehicleLandingDamage;
import com.flansmodultimate.common.driveables.optics.FireControlSolution;
import com.flansmodultimate.common.driveables.physics.*;
import com.flansmodultimate.common.driveables.visual.VehicleVisual;
import com.flansmodultimate.common.physics.ModPhysics;
import com.flansmodultimate.api.equipment.WeaponBallistics;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.*;
import com.flansmodultimate.platform.PlatformSounds;
import com.flansmodultimate.platform.entity.SynchedDataDefinition;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Wheel-, track- and water-capable server vehicle simulation. */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Vehicle extends Driveable implements VehicleVisual
{
    @Getter
    protected float wheelYaw;
    @Getter
    protected float prevWheelYaw;
    @Getter
    protected float wheelAngle;
    @Getter
    protected float prevWheelAngle;
    @Getter
    protected float leftTrackProgress;
    @Getter
    protected float rightTrackProgress;
    @Getter
    private float trackTravelStep;
    @Getter
    private float leftTrackStep;
    @Getter
    private float rightTrackStep;
    private int throttleDecayDelay;
    private boolean fixedThrottle;
    /** Progressive throttle lever state. Transient, and tracked per side. */
    private final ThrottleLeverRamp throttleRamp = new ThrottleLeverRamp();
    /** Real-world gearbox direction state. Transient and server-side. */
    private final DriveDirectionInterlock drivetrain = new DriveDirectionInterlock();
    /** Hull yaw seen by the previous engine-sound tick, and the ticks left before a pivot turn counts as stopped. Client side. */
    private float engineSoundYaw = Float.NaN;
    private int pivotSoundTicks;
    /** Whether a burning ammunition rack has already killed the driver. Transient and server-side, as in the Labjac fork. */
    private boolean ammoRackDriverKilled;
    /** Degrees per tick the drawn barrel moves toward its fire-control elevation. */
    private static final float FIRE_CONTROL_LAYING_RATE = 1F;
    /** Elevation in degrees the fire control adds to the driver's main gun, which clients raise the drawn barrel by. */
    private static final EntityDataAccessor<Float> DATA_FIRE_CONTROL_ELEVATION = SynchedEntityData.defineId(Vehicle.class, EntityDataSerializers.FLOAT);

    /** Oxygen ticks a diving vehicle's crew has left, for its HUD and for predicting whether it is forced up. */
    private static final EntityDataAccessor<Float> DATA_OXYGEN = SynchedEntityData.defineId(Vehicle.class, EntityDataSerializers.FLOAT);
    private static final String NBT_OXYGEN = "Oxygen";
    /** Hull damage per tick below a diving vehicle's crush depth, as in the Labjac fork. */
    private static final float CRUSH_DAMAGE_PER_TICK = 10F;
    /** Most blocks of water above a diving hull that are counted for its depth. */
    private static final int MAX_DEPTH_PROBE_BLOCKS = 160;

    /** Range the driver last lased for the fire control, in blocks, or negative for none. Transient and server-side. */
    private double fireControlRange = -1D;
    /** The driver who lased {@link #fireControlRange}; a new driver has to lase again. */
    @Nullable
    private Entity fireControlRangedBy;
    /** Depth rate of a diving vehicle, from -1 (diving) to 1 (surfacing). Transient; the predicting client steps its own. */
    private float divingRate;
    /** Depth of the hull below the water surface measured this tick, or NaN out of water, and the tick it was measured. */
    private double diveDepth = Double.NaN;
    private int diveDepthTick = -1;
    /** Fire-control elevation the barrel is drawn with this tick and the last one. Client side. */
    private float clientFireControlElevation;
    private float prevClientFireControlElevation;

    public Vehicle(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Vehicle(Level level, VehicleType type, double x, double y, double z, float yaw, @Nullable Player placer, ItemStack sourceStack)
    {
        super(FlansModEntities.vehicleEntity.get(), level, type, x, y, z, yaw, placer, sourceStack);
    }

    @Nullable
    public VehicleType getVehicleType()
    {
        return getConfigType() instanceof VehicleType type ? type : null;
    }

    /**
     * A tracked vehicle rotating on the spot drives its tracks with a closed throttle, so it keeps
     * the driving loop. The hull yaw is compared against the previous sound tick, which covers both
     * the predicted driver and interpolated observers, and a short grace period stops the loops
     * flickering between ticks where the yaw update lands late.
     */
    @Override
    protected boolean isEngineLoadedWithoutThrottle()
    {
        VehicleType type = getVehicleType();
        float yaw = getYaw();
        boolean rotating = !Float.isNaN(engineSoundYaw) && Math.abs(Mth.wrapDegrees(yaw - engineSoundYaw)) > 0.05F;
        engineSoundYaw = yaw;
        if (type == null || !type.isTank())
            return false;
        if (rotating)
            pivotSoundTicks = 4;
        else if (pivotSoundTicks > 0)
            --pivotSoundTicks;
        return pivotSoundTicks > 0;
    }

    @Override
    protected boolean stepsOnWheelContact()
    {
        return true;
    }

    /** Animate completed movement, including observer interpolation and local prediction. */
    @Override
    public void tick()
    {
        double previousX = getX();
        double previousZ = getZ();
        float previousYaw = getYaw();
        super.tick();
        VehicleType type = getVehicleType();
        if (type == null || isRemoved())
            return;
        float previousLeftPhase = leftTrackProgress;
        float previousRightPhase = rightTrackProgress;
        advanceAnimations(type, getX() - previousX, getZ() - previousZ, Mth.wrapDegrees(getYaw() - previousYaw));
        tickWalkerStompSounds(type, previousLeftPhase, previousRightPhase);
        tickAmmoRackCookOff(type);
        if (level().isClientSide)
            tickClientFireControlElevation();
        else
        {
            if (tickCount % 5 == 0)
                updateFireControlElevation();
            tickDivingSupplies(type);
        }
    }

    @Override
    protected void defineEntityData(SynchedDataDefinition data)
    {
        super.defineEntityData(data);
        data.define(DATA_FIRE_CONTROL_ELEVATION, 0F);
        data.define(DATA_OXYGEN, -1F);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_OXYGEN, Tag.TAG_ANY_NUMERIC))
            entityData.set(DATA_OXYGEN, tag.getFloat(NBT_OXYGEN));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        if (entityData.get(DATA_OXYGEN) >= 0F)
            tag.putFloat(NBT_OXYGEN, entityData.get(DATA_OXYGEN));
    }

    /** @return the oxygen ticks a diving vehicle's crew has left: a fresh one has its full supply */
    public float getOxygen()
    {
        VehicleType type = getVehicleType();
        float oxygen = entityData.get(DATA_OXYGEN);
        return oxygen >= 0F || type == null ? Math.max(0F, oxygen) : type.getMaxOxygen();
    }

    /** @return how deep the hull is below the water surface, in blocks, or NaN out of water. Measured once per tick. */
    public double getDiveDepth()
    {
        if (diveDepthTick != tickCount)
        {
            diveDepthTick = tickCount;
            diveDepth = measureDiveDepth();
        }
        return diveDepth;
    }

    private double measureDiveDepth()
    {
        int baseY = Mth.floor(getY());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(Mth.floor(getX()), baseY, Mth.floor(getZ()));
        if (!level().isLoaded(cursor) || !level().getFluidState(cursor).is(FluidTags.WATER))
            return Double.NaN;
        double surface = Double.NaN;
        for (int offset = 0; offset <= MAX_DEPTH_PROBE_BLOCKS; offset++)
        {
            cursor.setY(baseY + offset);
            if (!level().isLoaded(cursor))
                break;
            FluidState fluid = level().getFluidState(cursor);
            if (!fluid.is(FluidTags.WATER))
                break;
            surface = cursor.getY() + fluid.getHeight(level(), cursor);
        }
        return Double.isFinite(surface) ? Math.max(0D, surface - getY()) : Double.NaN;
    }

    /** Steps the driver's depth rate of a diving vehicle from this tick's controls. Server and predicting client. */
    private void stepDivingRate(VehicleType type)
    {
        if (!type.isCanDive())
        {
            divingRate = 0F;
            return;
        }
        int mask = getInputMask();
        boolean driven = getControllingEntity() instanceof Player;
        divingRate = SubmarineDiving.stepRate(divingRate, driven && DriveableInput.isDown(mask, DriveableInput.DESCEND), driven && DriveableInput.isDown(mask, DriveableInput.SURFACE),
            DriveableInput.isDown(mask, DriveableInput.BRAKE | DriveableInput.ASCEND), getOxygen() > 0.1F);
    }

    /**
     * Moves a diving vehicle at its depth rate instead of letting it float, once it is submerged or diving.
     *
     * @return the velocity, or null to leave a surfaced or dry vehicle to the ordinary physics
     */
    @Nullable
    private Vec3 applyDiving(Vec3 velocity, VehicleType type)
    {
        if (!type.isCanDive())
            return null;
        double depth = getDiveDepth();
        if (!Double.isFinite(depth) || divingRate >= 0F && depth <= SubmarineDiving.SUBMERGED_DEPTH)
            return null;
        return new Vec3(velocity.x, SubmarineDiving.verticalVelocity(divingRate, type.getDiveSpeed(), type.getSurfaceSpeed()), velocity.z);
    }

    /** Uses up and refills a diving vehicle's oxygen and crushes a hull below its crush depth. Server side. */
    private void tickDivingSupplies(VehicleType type)
    {
        if (!type.isCanDive() || destroyed || isWreck())
            return;
        double depth = getDiveDepth();
        float oxygen = SubmarineDiving.stepOxygen(getOxygen(), Double.isFinite(depth) ? depth : 0D, type.getMaxOxygen(), type.isUnlimitedOxygen());
        if (Float.floatToIntBits(entityData.get(DATA_OXYGEN)) != Float.floatToIntBits(oxygen))
            entityData.set(DATA_OXYGEN, oxygen);
        DriveablePart core = driveableData == null ? null : driveableData.getPart(EnumDriveablePart.CORE);
        if (core != null && Double.isFinite(depth) && depth > type.getMaxDepth())
            core.damage(CRUSH_DAMAGE_PER_TICK, false);
    }

    /**
     * Takes the range the driver lased for the fire control, or clears it with a negative or non-finite one. Ranges beyond
     * the fire control's reach are not kept. Server side.
     */
    public void setFireControlRange(Entity driver, double range)
    {
        VehicleType type = getVehicleType();
        boolean valid = type != null && type.isFireControlAdjustsBarrel() && Double.isFinite(range) && range > 0D && range <= type.getFireControlMaxRange();
        fireControlRange = valid ? range : -1D;
        fireControlRangedBy = valid ? driver : null;
    }

    /**
     * Raises the driver's gun and shell shots to the ballistic solution for the lased range, within the fire control's
     * largest correction and the driver seat's pitch limits, as the Labjac fork's adjusting-barrel fire control. Guided
     * rounds, bombs and mines fly as aimed.
     */
    @Override
    protected Vec3 applyFireControl(Vec3 direction, FireableGun fireable, BulletType bullet, @Nullable LivingEntity attacker, ItemStack ammo, EnumWeaponType weapon)
    {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float correction = fireControlCorrection(direction, fireable, bullet, attacker, ammo, weapon);
        if (correction == 0F || horizontal < 1.0E-6D)
            return direction;
        double elevation = Math.atan2(direction.y, horizontal) + Math.toRadians(correction);
        return new Vec3(direction.x / horizontal * Math.cos(elevation), Math.sin(elevation), direction.z / horizontal * Math.cos(elevation)).scale(direction.length());
    }

    /** @return the elevation in degrees the fire control adds to a shot fired along a direction, or 0 for none. Server side. */
    private float fireControlCorrection(Vec3 direction, FireableGun fireable, BulletType bullet, @Nullable LivingEntity attacker, ItemStack ammo, EnumWeaponType weapon)
    {
        VehicleType type = getVehicleType();
        if (type == null || !type.isFireControlAdjustsBarrel() || fireControlRange <= 0D || weapon != EnumWeaponType.SHELL && weapon != EnumWeaponType.GUN || isGuided(bullet))
            return 0F;
        Entity controller = getControllingEntity();
        if (controller == null || controller != fireControlRangedBy)
            return 0F;
        FiredShot shot = new FiredShot(fireable, bullet, this, attacker, ShootableItem.getRoundsFired(ammo));
        float speed = shot.getMuzzleVelocity(false);
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (speed <= 0F || horizontal < 1.0E-6D)
            return 0F;
        double gravity = ModPhysics.gravity(ShootableType.FALL_SPEED_COEFFICIENT * bullet.getFallSpeed(), level());
        double quadraticDrag = shot.getQuadraticDrag(level());
        double drag = quadraticDrag > 0D ? 1D : ModPhysics.dragRetention(bullet.getDragInAir(), level());
        float lineElevation = (float) Math.toDegrees(Math.atan2(direction.y, horizontal));
        float maximum = type.getFireControlMaxCorrection();
        float correction = FireControlSolution.elevationCorrection(fireControlRange, lineElevation, new WeaponBallistics(speed, gravity, drag, false, quadraticDrag), maximum);
        correction = Mth.clamp(correction * type.getFireControlDropScale(), -maximum, maximum);
        // The gun cannot be raised past the elevation its seat allows; aim pitch is positive downwards
        Seat driver = getDriverSeat();
        SeatInfo info = driver == null ? null : driver.getSeatInfo();
        if (info != null)
        {
            float aimPitch = getTurretPitch();
            correction = aimPitch - info.clampPitch(aimPitch - correction);
        }
        return correction;
    }

    /** Keeps the correction of the driver's main gun in step with its lased range, for clients to raise the barrel by. Server side. */
    private void updateFireControlElevation()
    {
        float elevation = 0F;
        AmmoSelection selection = fireControlRange > 0D && configType != null && !destroyed && !isWreck() ? selectDriverWeaponAmmo(false) : null;
        Vec3 direction = selection == null ? null : getDriverWeaponDirection(false);
        if (direction != null && selection.stack().getItem() instanceof ShootableItem item && item.getConfigType() instanceof BulletType bullet)
            elevation = fireControlCorrection(direction, resolveFireableGun(selection, false), bullet, getControllingEntity() instanceof LivingEntity living ? living : null, selection.stack(),
                configType.weaponType(false));
        if (Float.floatToIntBits(entityData.get(DATA_FIRE_CONTROL_ELEVATION)) != Float.floatToIntBits(elevation))
            entityData.set(DATA_FIRE_CONTROL_ELEVATION, elevation);
    }

    /** Moves the drawn barrel toward the synced fire-control elevation at the rate a gun is laid. Client side. */
    private void tickClientFireControlElevation()
    {
        prevClientFireControlElevation = clientFireControlElevation;
        float target = entityData.get(DATA_FIRE_CONTROL_ELEVATION);
        clientFireControlElevation += Mth.clamp(target - clientFireControlElevation, -FIRE_CONTROL_LAYING_RATE, FIRE_CONTROL_LAYING_RATE);
    }

    @Override
    public float getFireControlElevation(float partialTick)
    {
        return Mth.lerp(partialTick, prevClientFireControlElevation, clientFireControlElevation);
    }

    private static boolean isGuided(BulletType bullet)
    {
        return bullet.isLockOnToPlanes() || bullet.isLockOnToVehicles() || bullet.isLockOnToMechas() || bullet.isLockOnToPlayers() || bullet.isLockOnToLivings()
            || bullet.isEnableSACLOS() || bullet.isManualGuidance() || bullet.isLaserGuidance();
    }

    /** A destroyed ammunition rack kills the driver once, then burns its core or turret down with small blasts. Server side. */
    private void tickAmmoRackCookOff(VehicleType type)
    {
        if (level().isClientSide || destroyed || isWreck() || tickCount % AmmoRackCookOff.PULSE_TICKS != 0)
            return;
        EnumDriveablePart burning = AmmoRackCookOff.burningPart(driveableData == null ? null : driveableData.getParts());
        if (burning == null)
            return;
        DriveablePart part = driveableData.getPart(burning);
        killDriverForAmmoRack();
        part.damage(AmmoRackCookOff.damagePerPulse(part.getMaxHealth(), type.getWeakspotCookTime()), false);
        if (level() instanceof ServerLevel serverLevel)
        {
            Vec3 centre = getPartWorldCentre(burning);
            serverLevel.sendParticles(ParticleTypes.LAVA, centre.x, centre.y + 0.5D, centre.z, 6, 0.4D, 0.3D, 0.4D, 0.1D);
            serverLevel.sendParticles(ParticleTypes.FLAME, centre.x, centre.y + 0.5D, centre.z, 8, 0.3D, 0.6D, 0.3D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y + 1D, centre.z, 4, 0.3D, 0.5D, 0.3D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y + 0.5D, centre.z, 1, 0.2D, 0.2D, 0.2D, 0D);
            level().playSound(null, centre.x, centre.y, centre.z, PlatformSounds.genericExplode(), SoundSource.BLOCKS, 1.5F, 0.9F + random.nextFloat() * 0.3F);
        }
    }

    private void killDriverForAmmoRack()
    {
        if (ammoRackDriverKilled)
            return;
        Seat driver = getDriverSeat();
        if (driver == null)
            return;
        for (Entity passenger : List.copyOf(driver.getPassengers()))
        {
            if (!(passenger instanceof LivingEntity living))
                continue;
            living.stopRiding();
            DamageSource source = lastAtkEntity instanceof Player attacker ? level().damageSources().playerAttack(attacker)
                : lastAtkEntity instanceof LivingEntity attacker ? level().damageSources().mobAttack(attacker) : level().damageSources().generic();
            living.hurt(source, Float.MAX_VALUE);
            ammoRackDriverKilled = true;
        }
    }

    @Override
    protected void tickDriveable()
    {
        VehicleType type = getVehicleType();
        if (type == null)
            return;
        // Let the root collision body clear the same ledges that the wheel
        // probes can reach. Legacy vehicles always used a one-block entity
        // step; using the configured value preserves that behaviour without
        // injecting an upward suspension impulse.
        updateThrottleAndSteering(type);
        applyEpicShipThrottleLimits();
        stepDivingRate(type);

        boolean pushed = type.getPushSpeedKmh() > 0F;
        float traction = traction();
        boolean tracked = type.isTank() && !pushed;
        boolean leftTrackIntact = isPartIntact(EnumDriveablePart.LEFT_TRACK);
        boolean rightTrackIntact = isPartIntact(EnumDriveablePart.RIGHT_TRACK);
        boolean singleTrackDrive = tracked && leftTrackIntact != rightTrackIntact;
        boolean noTrackDrive = tracked && !leftTrackIntact && !rightTrackIntact;
        if (tracked && (!leftTrackIntact || !rightTrackIntact))
            traction = 0F;
        if (noTrackDrive)
        {
            setThrottle(approach(getThrottle(), 0F, 0.04F));
            prevWheelYaw = wheelYaw;
            wheelYaw = 0F;
        }

        float throttleLimit = DriveableControlPhysics.damagedThrottleLimit(getThrottleDamageNerf());
        float effectiveThrottle = Mth.clamp(getThrottle(), -throttleLimit, throttleLimit);
        boolean braking = DriveableInput.isDown(getInputMask(), DriveableInput.BRAKE | DriveableInput.ASCEND);
        if (braking || (pushed ? getControllingEntity() == null : !isEngineActive()))
            effectiveThrottle = 0F;
        ResolvedVehiclePhysics physics = type.getResolvedPhysics();
        boolean derivedPhysics = !pushed && !ModCommonConfig.forceLegacyVehiclePhysics() && physics.hasGroundPropulsion();
        double speedScale = ModCommonConfig.realisticSpeedScale(physics.category());
        float normalizedThrottle = DriveableControlPhysics.normalizedThrottle(effectiveThrottle, type.getMaxNegativeThrottle());
        double targetSpeed;
        if (pushed)
        {
            targetSpeed = normalizedThrottle * VehiclePhysicsUnits.kmhToBlocksPerTick(type.getPushSpeedKmh()) * traction;
        }
        else if (derivedPhysics)
        {
            // The real-world profile owns terminal speed outright. Throttle is
            // reduced to the driver demand fraction, so MaxThrottle and the
            // legacy 0.26 / 0.32 speed factors no longer scale it and cannot
            // double-apply on top of the derived value.
            double terminal = normalizedThrottle >= 0F ? physics.maxSpeedBlocksPerTick(speedScale) : reverseTerminalSpeed(type, physics, speedScale);
            targetSpeed = normalizedThrottle * terminal * traction;
        }
        else
        {
            float propulsion = DriveableControlPhysics.directionalPropulsion(effectiveThrottle, type.getMaxThrottle(), type.getMaxNegativeThrottle(), type.getMaxThrottleInWater(), isInWater());
            targetSpeed = propulsion * getEngineSpeed() * (tracked ? 0.26D : 0.32D) * traction;
            // An authored reverse speed caps legacy propulsion rather than
            // scaling it, so MaxNegativeThrottle is never applied twice.
            if (!ModCommonConfig.forceLegacyVehiclePhysics() && physics.hasReverseSpeedOverride() && targetSpeed < 0D)
                targetSpeed = Math.max(targetSpeed, -physics.reverseSpeedBlocksPerTick(speedScale));
        }
        // The ground under the wheels: last tick's averaged wheel sample, firm while airborne, afloat or switched off.
        SurfaceTraction.Sample surface = ModCommonConfig.surfaceTractionEnabled() && !isInWater() ? wheelSurface : SurfaceTraction.Sample.FIRM;
        targetSpeed *= surface.speedFactor();
        // A complete real-world ground profile derives a forgiving uphill response
        // from power-to-weight and drive layout; legacy propulsion stays unchanged.
        // Downhill its driver holds it back on the engine instead, so that it does not take a steep descent at road speed.
        if (derivedPhysics)
        {
            targetSpeed *= GroundSlopePhysics.propulsionFactor(getPitch(), Math.signum(normalizedThrottle), physics.powerToWeightKwPerKg(), physics.driveType(), surface.grip());
            targetSpeed *= GroundSlopePhysics.descentFactor(getPitch(), Math.signum(normalizedThrottle));
        }
        boolean steeringHeld = isPartIntact(EnumDriveablePart.STEERING) && axis(getInputMask(), DriveableInput.RIGHT, DriveableInput.LEFT) != 0F;
        float turnControl = singleTrackDrive ? DriveableControlPhysics.singleTrackTurnControl(effectiveThrottle, wheelYaw, steeringHeld, leftTrackIntact, rightTrackIntact) : wheelYaw;
        float steeringModifier = turnControl > 0F ? type.getTurnLeftModifier() : type.getTurnRightModifier();

        float directionalThrottle;
        if (effectiveThrottle > 0F)
        {
            if (isInWater())
                directionalThrottle = type.getMaxThrottleInWater();
            else
                directionalThrottle = type.getMaxThrottle();
        }
        else
        {
            directionalThrottle = type.getMaxNegativeThrottle();
        }

        Vec3 steeringForward = localDirectionToWorld(LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D)));
        double forwardLength = Math.hypot(steeringForward.x, steeringForward.z);
        Vec3 steeringVelocity = getDeltaMovement();
        double signedSpeed = forwardLength > 1.0E-8D ? (steeringVelocity.x * steeringForward.x + steeringVelocity.z * steeringForward.z) / forwardLength : 0D;
        double trackedScale = isEngineActive() ? 0.04D * Math.max(0F, directionalThrottle) * getEngineSpeed() : 0D;
        double velocityScale = tracked ? trackedScale : DriveableControlPhysics.wheeledSteeringVelocityScale(signedSpeed);
        double steeringScale = 0.1D * Math.max(0F, steeringModifier);
        float yawDelta = (float) Math.toDegrees(turnControl * steeringScale * velocityScale);
        if (type.usesRealTurnRate(ModCommonConfig.forceLegacyVehiclePhysics(), pushed, tracked))
        {
            double referenceSpeed = tracked ? 0D : VehiclePhysicsUnits.kmhToBlocksPerTick(type.getRealWorldSpec().maxSpeedKmh()) * speedScale;
            float realTurnControl = singleTrackDrive ? DriveableControlPhysics.realSingleTrackTurnControl(effectiveThrottle, wheelYaw, steeringHeld, leftTrackIntact, rightTrackIntact) : wheelYaw;
            yawDelta = DriveableControlPhysics.realSteeringYawDelta(type.getRealTurnRateDegPerSec(), realTurnControl, tracked, isEngineActive(), signedSpeed, referenceSpeed);
        }
        // Low surface grip costs steering authority gently: a vehicle on ice understeers but still answers.
        yawDelta *= SurfaceTraction.steeringFactor(surface.grip());
        if ((braking && tracked) || !isPartIntact(EnumDriveablePart.STEERING))
            yawDelta = 0F;
        // Only wheel contact keeps the terrain pose: a body resting on a ledge with its wheels over a drop has nothing
        // that aligns it, and would otherwise hold its last angle indefinitely. A wheelless type keeps its placed pose.
        boolean supported = hasWheelContact() || onGround() && type.getWheelPositions().isEmpty();

        float pitch = supported ? getPitch() : approach(getPitch(), 0F, 0.8F);
        float roll;
        if (type.isCanRoll())
        {
            if (supported)
                roll = getRoll();
            else
                roll = approach(getRoll(), 0F, 1F);
        }
        else
        {
            roll = 0F;
        }

        boolean navalPose = type.isEpicShip() && isFloatingOnWater();
        setOrientation(getYaw() + yawDelta, navalPose ? getPitch() : pitch, navalPose ? getRoll() : roll);

        Vec3 legacyForward = LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D));
        Vec3 transformedForward = localDirectionToWorld(legacyForward);
        Vec3 forward = new Vec3(transformedForward.x, 0D, transformedForward.z);
        if (forward.lengthSqr() > 1.0E-8D)
            forward = forward.normalize();
        Vec3 current = getDeltaMovement();

        double grip;
        if (isInWater())
        {
            grip = 0.08D;
        }
        else
        {
            // Lateral slip decays at the grip the surface leaves, so a vehicle on ice slides where it was going.
            if (onGround() || hasWheelContact())
                grip = 0.22D * surface.grip();
            else
                grip = 0.035D;
        }
        Vec3 velocity;
        if (derivedPhysics)
        {
            velocity = derivedGroundVelocity(physics, current, forward, targetSpeed, traction, grip, braking, speedScale, yawDelta, normalizedThrottle, surface.grip());
        }
        else
        {
            Vec3 desired = forward.scale(targetSpeed);
            velocity = new Vec3(Mth.lerp(grip, current.x, desired.x), current.y, Mth.lerp(grip, current.z, desired.z));
        }

        velocity = applyVehicleVerticalPhysics(velocity, type);
        double descent = velocity.y;
        velocity = applyWheelContactPhysics(velocity, !isFloatingOnWater(), verticalGravity(type));
        double horizontalDrag = GroundPropulsionPhysics.postIntegrationHorizontalDrag(derivedPhysics || pushed, type.getDrag());
        velocity = velocity.multiply(ModPhysics.dragRetention(horizontalDrag, level()), 1D, ModPhysics.dragRetention(horizontalDrag, level()));

        if (!ModCommonConfig.forceLegacyVehiclePhysics())
            velocity = enforceSpeedCap(velocity, ModCommonConfig.maxVehicleSpeedKmh());

        velocity = applyGroundFriction(current, velocity, effectiveThrottle, braking, tracked, pushed, derivedPhysics && drivetrain.isShifting() ? Math.abs(normalizedThrottle) : 0F, surface.grip());
        moveWithCollisions(velocity);

        handleLandingImpact(type, descent);

        harvestConfiguredBlocks();
        if (isEngineActive() && !pushed)
            consumeFuel(DriveableControlPhysics.vehicleFuelLoad(effectiveThrottle, type.getWheelPositions().size()));
    }

    /**
     * Applies the legacy drop response to a hull that has just met the ground.
     *
     * <p>
     * Legacy Flan's ran this out of {@code fall()} as a flat hitpoint figure,
     * which bit only on the small cores period packs authored.
     * {@link VehicleLandingDamage} reproduces the same outcome for those hulls
     * as a share of each part's own health, so a drop stays equally
     * consequential for a pack authoring thousands of hitpoints or deriving them
     * from mass through {@code UseRealisticVehicleHealth}.
     *
     * @param descentSpeed
     *            the downward speed this tick's physics asked for, before the
     *            suspension cancelled it; negative downwards
     */
    private void handleLandingImpact(@NotNull VehicleType type, double descentSpeed)
    {
        // Settling onto its suspension on the tick it is placed is not a landing,
        // and water breaks a fall rather than stopping it dead.
        if (level().isClientSide || tickCount <= 20 || !verticalCollision || isInWater())
            return;

        VehicleLandingDamage.Impact impact = VehicleLandingDamage.evaluate(-descentSpeed, type.getFallDamageFactor());
        if (!impact.damaging())
            return;

        // The running gear is what is meant to meet the ground, so it takes its
        // share on top of the hull's rather than instead of it.
        DriveablePosition firstWheel = type.getWheelPosition(0);
        if (firstWheel != null && firstWheel.getPart() != EnumDriveablePart.CORE)
            damageLandingPart(firstWheel.getPart(), impact.runningGearHealthFraction());
        damageLandingPart(EnumDriveablePart.CORE, impact.coreHealthFraction());
    }

    private void damageLandingPart(@NotNull EnumDriveablePart partType, float healthFraction)
    {
        DriveablePart part = driveableData == null ? null : driveableData.getPart(partType);
        if (part == null || part.isDestroyed() || healthFraction <= 0F || part.getMaxHealth() <= 0F)
            return;
        damagePart(partType, part.getMaxHealth() * healthFraction, level().damageSources().fall());
    }

    // Forge's getStepHeight() reads this, and vanilla and other mods still call it directly.
    @Override
    @SuppressWarnings("deprecation")
    public float maxUpStep()
    {
        VehicleType type = getVehicleType();
        return type == null ? super.maxUpStep() : Mth.clamp(type.getWheelStepHeight(), 0F, 2.5F);
    }

    @Override
    protected void tickClientDriveable()
    {
        VehicleType type = getVehicleType();
        if (type != null)
        {
            float steeringInput = isPartIntact(EnumDriveablePart.STEERING) ? axis(getInputMask(), DriveableInput.RIGHT, DriveableInput.LEFT) : 0F;
            prevWheelYaw = wheelYaw;
            wheelYaw = DriveableControlPhysics.dampedControl(wheelYaw, steeringInput, 1F);
        }
    }

    /** The predicted step steers the wheels; the tick then animates its completed movement. */
    @Override
    public boolean supportsClientPrediction()
    {
        return true;
    }

    @Override
    protected void handleRisingInputs(@NotNull Seat seat, @NotNull Player player, int rising)
    {
        boolean deploySmoke = DriveableInput.isDown(rising, DriveableInput.FLARE);
        boolean smokeWasReady = !isVarFlare() && !isCountermeasureReloading();
        super.handleRisingInputs(seat, player, rising);

        VehicleType type = getVehicleType();
        if (deploySmoke && smokeWasReady && isVarFlare() && type != null && !type.getSmokers().isEmpty())
            dischargeSmoke();
    }

    /**
     * Vehicles use the legacy smoke dispensers as their countermeasures. The
     * shared timer and flags still provide cooldown and lock-on behaviour, but
     * the generic flare particle must not be emitted as well.
     */
    @Override
    protected void updateFlares()
    {
        if (ticksFlareUsing <= 0)
        {
            setFlag(FLAG_FLARE, false);
            setFlag(FLAG_COUNTERMEASURE_RELOADING, flareDelay > 0);
            return;
        }
        --ticksFlareUsing;
        setFlag(FLAG_FLARE, true);
        setFlag(FLAG_COUNTERMEASURE_RELOADING, flareDelay > 0);
    }

    private void updateThrottleAndSteering(VehicleType type)
    {
        int input = DriveableControlPhysics.vehicleThrottleInput(getInputMask());
        float throttle = getThrottle();
        boolean canControl = getControllingEntity() != null && (isEngineActive() || type.getPushSpeedKmh() > 0F);
        boolean braking = DriveableInput.isDown(input, DriveableInput.BRAKE | DriveableInput.ASCEND);
        boolean pedalInput = DriveableInput.isDown(input, DriveableInput.FORWARD | DriveableInput.BACKWARD);
        fixedThrottle = DriveableControlPhysics.fixedVehicleThrottle(fixedThrottle, canControl, braking, input);
        // Advanced every tick, including the ones where nothing is held, so the
        // ramp is counting an uninterrupted hold and nothing else.
        int leverDirection;
        if (canControl && braking && throttle != 0F)
        {
            leverDirection = (throttle > 0F ? -1 : 1);
        }
        else
        {
            if (canControl && !pedalInput)
                leverDirection = ThrottleLeverRamp.direction(input, DriveableInput.THROTTLE_INCREASE, DriveableInput.THROTTLE_DECREASE);
            else
                leverDirection = 0;
        }

        float leverMultiplier = throttleRamp.advance(leverDirection, ThrottleLeverRamp.VEHICLE_MAX_STEP_MULTIPLIER);
        if (canControl)
        {
            float damageMultiplier = DriveableControlPhysics.damagedAccelerationMultiplier(getThrottleDamageNerf());
            // Precedence: a complete real-world profile beats the legacy
            // UseRealisticAcceleration experiment, which beats the fixed legacy rate.
            // Under the profile this is only how fast the pedal travels,
            // how fast the vehicle actually accelerates comes from the power
            // model, not from a per-pack number.
            float acceleration;
            if (!ModCommonConfig.forceLegacyVehiclePhysics() && type.getResolvedPhysics().hasGroundPropulsion())
                acceleration = VehiclePhysicsConstants.REAL_THROTTLE_RAMP_PER_TICK;
            else if (type.isUseRealisticAcceleration())
                acceleration = Math.max(0.0005F, getEnginePower() / Math.max(1F, type.getMass()));
            else
                acceleration = 0.01F;
            acceleration *= damageMultiplier;
            if (DriveableInput.isDown(input, DriveableInput.FORWARD))
            {
                throttle += acceleration * (throttle < 0F ? type.getBrakingModifier() : 1F);
                throttleDecayDelay = 10;
            }
            if (DriveableInput.isDown(input, DriveableInput.BACKWARD))
            {
                throttle -= acceleration * (throttle > 0F ? type.getBrakingModifier() : 1F);
                throttleDecayDelay = 10;
            }
            // Unlike the pedals the lever retains the selected demand after
            // release, so it travels on its own fine step rather than the
            // pedal rate, and holding it moves it progressively faster. The
            // ramp restarts as the lever passes through zero, so drive to
            // reverse is never swept.
            float leverStep = ThrottleLeverRamp.VEHICLE_LEVER_BASE_STEP * damageMultiplier * leverMultiplier;
            if (braking)
                throttle = DriveableControlPhysics.brakedThrottle(throttle, leverStep * Math.max(0F, type.getBrakingModifier()));
            else if (leverDirection > 0)
                throttle += leverStep * (throttle < 0F ? type.getBrakingModifier() : 1F);
            else if (leverDirection < 0)
                throttle -= leverStep * (throttle > 0F ? type.getBrakingModifier() : 1F);
        }
        if (!braking)
        {
            if (type.isTank() && Math.abs(throttle) < 0.3F && Math.abs(axis(input, DriveableInput.RIGHT, DriveableInput.LEFT)) > 0F)
                throttle += Math.max(0F, type.getClutchBrake());
            else if (throttleDecayDelay > 0)
                --throttleDecayDelay;
            else if (!fixedThrottle)
                throttle = approach(throttle, 0F, type.getThrottleDecay() * (float) ModCommonConfig.vehicleThrottleDecayMultiplier());
        }
        float damageLimit = DriveableControlPhysics.damagedThrottleLimit(getThrottleDamageNerf());
        if (Math.abs(throttle) > damageLimit)
            throttle = (Math.copySign(damageLimit, throttle) + throttle * 2F) / 3F;
        // Whatever moved the lever through zero — the driver, the brake, or the
        // idle decay — the progressive step starts again from the base rate.
        throttleRamp.resetOnZeroCrossing(getThrottle(), throttle);
        setThrottle(throttle);

        float steeringInput = isPartIntact(EnumDriveablePart.STEERING) ? axis(input, DriveableInput.RIGHT, DriveableInput.LEFT) : 0F;
        prevWheelYaw = wheelYaw;
        wheelYaw = DriveableControlPhysics.dampedControl(wheelYaw, steeringInput, 1F);
    }

    /**
     * Longitudinal motion under the real-world profile.
     *
     * <p>
     * The power model owns speed along the forward axis: available
     * acceleration falls as the vehicle speeds up and reaches zero exactly at the
     * authored top speed, so the vehicle approaches it without snapping and
     * without overshooting at 20 Hz. Lateral slip keeps decaying at the existing
     * grip constant, so terrain feel and drift are unchanged.
     *
     * @param demand
     *            the normalized driver demand, whose magnitude is how hard the
     *            driver is braking while the gearbox sits in neutral
     */
    private Vec3 derivedGroundVelocity(ResolvedVehiclePhysics physics, Vec3 current, Vec3 forward, double targetSpeed, float traction, double grip, boolean braking, double speedScale, float yawDelta,
        float demand, float surfaceGrip)
    {
        Vec3 horizontal = new Vec3(current.x, 0D, current.z);
        double forwardSpeed = horizontal.dot(forward);
        Vec3 lateral = horizontal.subtract(forward.scale(forwardSpeed));

        double terminal = physics.maxSpeedBlocksPerTick(speedScale);
        double power = physics.effectivePowerWatts();
        // Demand against the direction still being travelled finds the gearbox
        // in neutral: the vehicle sheds its momentum before the opposite gear
        // engages, and that gear's clutch then takes up gradually.
        double transmission = drivetrain.advance(targetSpeed, VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(forwardSpeed));
        // Asking for the other direction is also asking to stop: the neutral
        // gearbox coasts, and the driver's own demand works the brake as far as
        // the control has been moved.
        double brakeFraction = braking ? 1D : 0D;
        if (drivetrain.isShifting())
        {
            targetSpeed = 0D;
            brakeFraction = Math.max(brakeFraction, Float.isFinite(demand) ? Math.abs(demand) : 0D);
        }
        // The surface limits what both the drive and the brakes can put down: launching and stopping lengthen on ice.
        double surface = Mth.clamp(surfaceGrip, 0F, 1F);
        double tractionFactor = physics.driveType().tractionFactor() * (isInWater() ? 0.35D : 1D) * Math.max(0F, traction) * surface;
        double acceleration = GroundPropulsionPhysics.accelerationBlocksPerTickSquared(forwardSpeed, power, physics.massKg(), terminal, tractionFactor, ModCommonConfig.dragFactor(level()))
            * transmission;
        double deceleration = GroundPropulsionPhysics.decelerationBlocksPerTickSquared(forwardSpeed, power, physics.massKg(), terminal, brakeFraction * surface, ModCommonConfig.dragFactor(level()));
        double newForwardSpeed = GroundPropulsionPhysics.approach(forwardSpeed, targetSpeed, acceleration, deceleration);
        newForwardSpeed = GroundPropulsionPhysics.applyTurningLoss(newForwardSpeed, yawDelta);

        Vec3 driven = forward.scale(newForwardSpeed).add(lateral.scale(1D - grip));
        return new Vec3(driven.x, current.y, driven.z);
    }

    /**
     * Reverse terminal speed under the real-world profile.
     *
     * <p>
     * An authored RealMaxReverseSpeedKmh wins. Otherwise the definition's own
     * forward-to-reverse throttle ratio is carried over, so a pack that always
     * reversed at half speed still does. The MaxNegativeThrottle zero gate lives
     * upstream in normalizedThrottle, so a vehicle that could never reverse still
     * cannot.
     */
    private static double reverseTerminalSpeed(VehicleType type, ResolvedVehiclePhysics physics, double speedScale)
    {
        if (physics.hasReverseSpeedOverride())
            return physics.reverseSpeedBlocksPerTick(speedScale);
        float forwardPower = Math.max(1.0E-4F, type.getMaxThrottle());
        float reversePower = Math.max(0F, type.getMaxNegativeThrottle());
        return physics.maxSpeedBlocksPerTick(speedScale) * Math.min(1D, reversePower / forwardPower);
    }

    /**
     * Minimum slowing of a grounded vehicle without drive demand. Nobody at the
     * controls, the engine off or the brake held means locked wheels or tracks;
     * a driver simply off the pedals meets rolling resistance, which tracks have
     * far more of than tyres. Asking for the opposite direction while the gearbox
     * is in neutral is that same rolling resistance plus the brake the driver is
     * standing on, so a full reversal of demand stops like the brake control.
     */
    // The friction mode needs the tick's whole drive state; a parameter record would only move the list.
    @SuppressWarnings("java:S107")
    private Vec3 applyGroundFriction(Vec3 before, Vec3 after, float effectiveThrottle, boolean braking, boolean tracked, boolean pushed, float neutralBrakeDemand, float surfaceGrip)
    {
        double deceleration;
        if (getControllingEntity() == null || !isEngineActive() && !pushed || braking)
            deceleration = VehiclePhysicsConstants.PARKED_GROUND_FRICTION_DECELERATION_MS2;
        else if (neutralBrakeDemand > 0F || Math.abs(effectiveThrottle) < 1.0E-3F && !DriveableInput.isDown(getInputMask(), DriveableInput.FORWARD | DriveableInput.BACKWARD))
            deceleration = Math.max(tracked ? VehiclePhysicsConstants.TRACKED_IDLE_DECELERATION_MS2 : VehiclePhysicsConstants.WHEELED_IDLE_DECELERATION_MS2,
                Math.min(1F, neutralBrakeDemand) * VehiclePhysicsConstants.PARKED_GROUND_FRICTION_DECELERATION_MS2);
        else
            return after;
        // Locked wheels and rolling resistance both work through the surface: a vehicle braking on ice keeps sliding.
        return applyMinimumGroundDeceleration(before, after, deceleration * Mth.clamp(surfaceGrip, 0F, 1F));
    }

    /** Per-tick downward velocity gravity contributes, shared with the suspension. */
    private double verticalGravity(VehicleType type)
    {
        return isFloatingOnWater() ? 0D : ModPhysics.gravity(Math.max(0.005D, Math.min(0.08D, type.getGravity() * 0.08D)), level());
    }

    private Vec3 applyVehicleVerticalPhysics(Vec3 velocity, VehicleType type)
    {
        Vec3 diving = applyDiving(velocity, type);
        if (diving != null)
            return diving;
        if (isFloatingOnWater())
        {
            if (!type.isEpicShip())
                setOrientation(getYaw(), approach(getPitch(), 0F, 1.5F), approach(getRoll(), 0F, 1.5F));
            return applyGravityAndBuoyancy(velocity, 0D);
        }
        velocity = applyGravityAndBuoyancy(velocity, verticalGravity(type));
        if (velocity.y < -type.getMaxFallSpeed())
            velocity = new Vec3(velocity.x, -type.getMaxFallSpeed(), velocity.z);
        return velocity;
    }

    private void advanceAnimations(VehicleType type, double displacementX, double displacementZ, float yawDelta)
    {
        prevWheelAngle = wheelAngle;
        Vec3 forward = localDirectionToWorld(LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D)));
        if (type.isRotateWheels() || type.isTank())
        {
            wheelAngle = Mth.wrapDegrees(wheelAngle + WheelAnimationPhysics.angularStepDegrees(displacementX, displacementZ, forward.x, forward.z));
        }
        trackTravelStep = TrackAnimationPhysics.travelStep(displacementX, displacementZ, forward.x, forward.z);
        if (type.isTank())
        {
            VehicleGeometry geometry = type.getResolvedPhysics().geometry();
            Float width = geometry.trackWidthM() != null ? geometry.trackWidthM() : geometry.widthM();
            TrackAnimationPhysics.Steps steps = TrackAnimationPhysics.steps(trackTravelStep, yawDelta, width == null ? 1D : width, isPartIntact(EnumDriveablePart.LEFT_TRACK),
                isPartIntact(EnumDriveablePart.RIGHT_TRACK));
            leftTrackStep = steps.left() * TrackAnimationPhysics.linkLoopScale(type.getLeftTrackPoints(), type.getTrackLinkLength(), type.getModelScale());
            rightTrackStep = steps.right() * TrackAnimationPhysics.linkLoopScale(type.getRightTrackPoints(), type.getTrackLinkLength(), type.getModelScale());
        }
        else
        {
            // Non-tank walkers retain their authored throttle-based leg timing.
            leftTrackStep = getThrottle() * 0.075F - wheelYaw * 0.0025F;
            rightTrackStep = getThrottle() * 0.075F + wheelYaw * 0.0025F;
        }
        leftTrackProgress += leftTrackStep;
        rightTrackProgress += rightTrackStep;
        leftTrackProgress -= Mth.floor(leftTrackProgress);
        rightTrackProgress -= Mth.floor(rightTrackProgress);
    }

    private void tickWalkerStompSounds(VehicleType type, float previousLeftPhase, float previousRightPhase)
    {
        if (level().isClientSide || !isEngineActive() || Math.abs(getThrottle()) <= 0.01F)
            return;
        playWalkerStomp(type.getStompSoundFrontRight(), crossedLegZero(previousRightPhase, rightTrackProgress, 0F));
        playWalkerStomp(type.getStompSoundBackRight(), crossedLegZero(previousRightPhase, rightTrackProgress, 0.75F));
        playWalkerStomp(type.getStompSoundFrontLeft(), crossedLegZero(previousLeftPhase, leftTrackProgress, 0.5F));
        playWalkerStomp(type.getStompSoundBackLeft(), crossedLegZero(previousLeftPhase, leftTrackProgress, 0.25F));
    }

    private void playWalkerStomp(String sound, boolean crossed)
    {
        if (crossed && StringUtils.isNotBlank(sound))
            PacketPlaySound.sendSoundPacket(this, ModCommonConfig.get().vehicleSoundRange(), sound, false);
    }

    private static boolean crossedLegZero(float previousPhase, float currentPhase, float phaseOffset)
    {
        float previous = Mth.sin((previousPhase + phaseOffset) * Mth.TWO_PI);
        float current = Mth.sin((currentPhase + phaseOffset) * Mth.TWO_PI);
        return previous != 0F && current != 0F && Math.signum(previous) != Math.signum(current);
    }

    private float traction()
    {
        VehicleType type = getVehicleType();
        if (type == null || type.getWheelPositions().isEmpty())
            return 1F;
        // An explicit DriveType chooses which wheels drive; without one the
        // legacy FourWheelDrive flag still decides, so inference alone can never
        // change an existing pack's traction.
        ResolvedVehiclePhysics physics = type.getResolvedPhysics();
        boolean allWheelsDriven = !ModCommonConfig.forceLegacyVehiclePhysics() && physics.driveTypeExplicit() ? physics.driveType().drivesAllWheels() : type.isFourWheelDrive();
        int configured = 0;
        int intact = 0;
        int wheelIndex = 0;
        for (DriveablePosition wheel : type.getWheelPositions())
        {
            int currentIndex = wheelIndex++;
            if (wheel == null)
                continue;
            if (!allWheelsDriven && type.getWheelPositions().size() >= 4 && currentIndex >= 2)
                continue;
            ++configured;
            if (isPartIntact(wheel.getPart()))
                ++intact;
        }
        return configured == 0 ? 1F : Mth.clamp((float) intact / configured, 0F, 1F);
    }

    public void dischargeSmoke()
    {
        VehicleType type = getVehicleType();
        if (type == null)
            return;
        for (VehicleType.SmokePoint smoker : type.getSmokers())
        {
            if (smoker == null || !isPartIntact(smoker.part()))
                continue;
            Vec3 localOrigin = LegacyDriveableCoordinates.toLocal(smoker.position());
            Vec3 localDirection = LegacyDriveableCoordinates.toLocal(smoker.direction());
            Vec3 origin = localToWorld(localOrigin.x, localOrigin.y, localOrigin.z);
            Vec3 direction = localDirectionToWorld(localDirection);
            int detonation = Mth.clamp(smoker.detonationTime(), 1, 20 * 60);
            if (detonation == 20)
                PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.FM_SMOKER, origin.x, origin.y, origin.z, direction.x, direction.y, direction.z), origin, 150D, level().dimension());
            else
                PacketHandler.sendToAllAround(new PacketSmokeShell(origin, direction, detonation), origin, 150D, level().dimension());
        }
    }

    @Override
    protected boolean canFireWeaponBank(boolean secondary)
    {
        VehicleType type = getVehicleType();
        // As in 1.7.10, the door only matters to vehicles marked ShootWithOpenDoor, whose weapons
        // are armed by opening it; every other vehicle fires whatever the state of its door
        return super.canFireWeaponBank(secondary) && (type == null || type.doorAllowsFiring(isDoorOpen()));
    }

    @Override
    protected boolean shouldSquashEntities()
    {
        VehicleType type = getVehicleType();
        return type != null && type.isSquashMobs();
    }

    private static float axis(int mask, int positive, int negative)
    {
        return (DriveableInput.isDown(mask, positive) ? 1F : 0F) - (DriveableInput.isDown(mask, negative) ? 1F : 0F);
    }

    private static float approach(float value, float target, float amount)
    {
        return value < target ? Math.min(target, value + amount) : Math.max(target, value - amount);
    }
}
