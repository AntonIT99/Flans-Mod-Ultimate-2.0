package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.IControllable;
import com.flansmodultimate.common.FlanExplosion;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.DriveableCollisionHelper;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveableExplosion;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.PilotGun;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.driveables.ShootPoint;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.item.ToolItem;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.DriveableHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.event.GunFiredEvent;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketDriveableDamage;
import com.flansmodultimate.network.client.PacketDriveableRenderState;
import com.flansmodultimate.network.client.PacketParticle;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-authoritative common runtime for planes, vehicles and mechas.
 *
 * <p>The client sends only a compact intent mask and constrained seat aim. All
 * transforms, fuel, inventory, weapon delays and damage are owned by the
 * server and replicated through normal entity data/position tracking.</p>
 */
public abstract class Driveable extends Entity implements IEntityAdditionalSpawnData, IFlanEntity<DriveableType>, IControllable
{
    public static final String NBT_TYPE = "driveable_type";
    public static final String NBT_YAW = "driveable_yaw";
    public static final String NBT_PITCH = "driveable_pitch";
    public static final String NBT_ROLL = "driveable_roll";
    public static final String NBT_THROTTLE = "driveable_throttle";
    public static final String NBT_TURRET_YAW = "turret_yaw";
    public static final String NBT_TURRET_PITCH = "turret_pitch";
    public static final String NBT_FLAGS = "driveable_flags";
    public static final String NBT_MODE = "driveable_mode";
    public static final String NBT_OWNER = "driveable_owner";
    public static final String NBT_LOCKED = "driveable_locked";
    public static final String NBT_ENGINE_START_TICKS = "engine_start_ticks";
    public static final String NBT_PRIMARY_SHOOT_DELAY = "primary_shoot_delay";
    public static final String NBT_SECONDARY_SHOOT_DELAY = "secondary_shoot_delay";
    public static final String NBT_RECOIL_TICKS = "recoil_ticks";
    public static final String NBT_RECOIL_DURATION = "recoil_duration";
    public static final String NBT_IT1_STAGE = "it1_stage";
    public static final String NBT_IT1_RELOAD_DELAY = "it1_reload_delay";
    public static final String NBT_IT1_CAN_FIRE = "it1_can_fire";
    public static final String NBT_IT1_RELOADING = "it1_reloading";
    public static final String NBT_IT1_DOOR_ANGLE = "it1_door_angle";
    public static final String NBT_IT1_ARM_ANGLE = "it1_arm_angle";
    public static final String NBT_IT1_RAIL_ANGLE = "it1_rail_angle";
    public static final String NBT_SOURCE_STACK = "source_stack";
    public static final String NBT_KEY_ID = "key";

    protected static final int FLAG_GEAR = 1;
    protected static final int FLAG_DOOR = 1 << 1;
    protected static final int FLAG_WING = 1 << 2;
    protected static final int FLAG_FLARE = 1 << 3;
    protected static final int FLAG_ENGINE = 1 << 4;
    protected static final int FLAG_IT1_CAN_FIRE = 1 << 5;
    protected static final int FLAG_IT1_RELOADING = 1 << 6;

    protected static final EntityDataAccessor<String> DATA_DRIVEABLE_TYPE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_ROLL = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_THROTTLE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_TURRET_YAW = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_TURRET_PITCH = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_FLIGHT_ROLL = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Boolean> DATA_MOUSE_CONTROL = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Float> DATA_RECOIL_PROGRESS = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_IT1_DOOR_ANGLE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_PREV_IT1_DOOR_ANGLE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_IT1_ARM_ANGLE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_PREV_IT1_ARM_ANGLE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_IT1_RAIL_ANGLE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_PREV_IT1_RAIL_ANGLE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> DATA_INPUT_MASK = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_FLAGS = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> DATA_FUEL = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> DATA_LOCK_TARGET = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.INT);

    private static final int INPUT_TIMEOUT_TICKS = 12;
    private static final int CHILD_REPAIR_INTERVAL = 20;
    private static final int RELOAD_SOUND_TICK_UNSET = 15_214_541;
    private static final double MAX_SPAWN_COORDINATE = 29_999_984D;

    @Nullable
    protected DriveableType configType;
    @Nullable
    private DriveableCollisionHelper collisionHelper;
    @Getter @Nullable
    protected DriveableData driveableData;
    protected String shortname = StringUtils.EMPTY;

    @Getter
    protected Seat[] seats = new Seat[0];
    @Getter
    protected Wheel[] wheels = new Wheel[0];
    @Getter
    protected final RotatedAxes axes = new RotatedAxes();

    @Getter protected float prevYaw;
    @Getter protected float prevPitch;
    @Getter protected float prevRoll;
    @Getter protected float prevTurretYaw;
    @Getter protected float prevTurretPitch;

    protected int localInputMask;
    protected int previousInputMask;
    protected int inputTimeout;
    protected int primaryShootDelay;
    protected int secondaryShootDelay;
    protected int primaryShootPointIndex;
    protected int secondaryShootPointIndex;
    protected int primaryBurstRemaining;
    protected int secondaryBurstRemaining;
    protected int primaryHeldTicks;
    protected int secondaryHeldTicks;
    protected int[] passengerShootDelay = new int[0];
    protected int[] passengerBurstRemaining = new int[0];
    protected int[] passengerHeldTicks = new int[0];
    protected int weaponInventoryFingerprint;
    protected boolean weaponInventoryFingerprintInitialized;
    protected int renderInventoryFingerprint;
    protected boolean renderInventoryFingerprintInitialized;
    protected int flareDelay;
    @Getter protected int ticksFlareUsing;
    protected int ticksSinceUsed;
    protected int markerTicks;
    protected int proxyCheckTicker;
    protected int engineSoundTimer;
    protected int idleSoundTimer;
    protected int reverseSoundTimer;
    protected int engineStartTicks;
    protected int recoilTicksRemaining;
    protected int recoilDuration;
    protected int it1Stage = 8;
    protected int it1ReloadDelay;
    protected int lockOnSoundDelay;
    protected int underWaterCheckTick = Integer.MIN_VALUE;
    protected boolean underWaterCached;
    protected boolean wasEngineActive;
    protected boolean engineRequested;
    protected boolean wasEngineRequested;
    protected boolean placementEffectsPending;
    protected boolean destroyed;
    protected boolean suppressDrops;
    protected boolean isShowedPosition;
    protected boolean locked;
    protected ItemStack sourceStack = ItemStack.EMPTY;
    protected final EnumSet<EnumDriveablePart> destroyedParts = EnumSet.noneOf(EnumDriveablePart.class);
    protected final Map<UUID, Entity> ridersHiddenByDriveable = new HashMap<>();
    @Nullable protected Entity lockOnTarget;
    private final float[] syncedPartHealth = new float[EnumDriveablePart.values().length];
    private final int[] syncedPartFireTicks = new int[EnumDriveablePart.values().length];
    private final byte[] syncedPartFlags = new byte[EnumDriveablePart.values().length];
    private boolean partSyncInitialized;

    @Getter @Nullable
    protected UUID ownerId;
    @Setter @Nullable
    protected Entity lastAtkEntity;

    protected Driveable(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    protected Driveable(EntityType<?> entityType, Level level, InfoType infoType)
    {
        this(entityType, level);
        if (infoType instanceof DriveableType type)
            initialize(type, ItemStack.EMPTY);
    }

    protected Driveable(EntityType<?> entityType, Level level, DriveableType type, double x, double y, double z,
                        float yaw, @Nullable Player placer, @Nullable ItemStack stack)
    {
        this(entityType, level);
        initialize(type, stack == null ? ItemStack.EMPTY : stack);
        setPos(x, y, z);
        setOrientation(yaw, 0F, 0F);
        if (placer != null)
            ownerId = placer.getUUID();
    }

    protected final void initialize(@NotNull DriveableType type, @NotNull ItemStack stack)
    {
        configType = type;
        collisionHelper = new DriveableCollisionHelper(type.getCollisionProfile());
        getPersistentData().putBoolean("CanMountEntity", type.isCanMountEntity());
        engineStartTicks = Math.max(0, type.getEngineStartTime());
        placementEffectsPending = !level().isClientSide;
        recoilTicksRemaining = 0;
        recoilDuration = 0;
        entityData.set(DATA_RECOIL_PROGRESS, 0F);
        it1Stage = 8;
        it1ReloadDelay = 0;
        setIT1Angles(0F, 0F, 0F, true);
        setFlag(FLAG_IT1_CAN_FIRE, type.isIT1());
        setFlag(FLAG_IT1_RELOADING, false);
        setShortName(type.getShortName());
        sourceStack = stack.copy();
        sourceStack.setCount(sourceStack.isEmpty() ? 0 : 1);
        driveableData = stack.isEmpty() ? new DriveableData(type) : DriveableData.fromStack(type, stack);
        if (!sourceStack.isEmpty())
            driveableData.removeSerializedState(sourceStack.getTag());
        weaponInventoryFingerprint = weaponInventoryFingerprint();
        weaponInventoryFingerprintInitialized = true;
        renderInventoryFingerprint = renderInventoryFingerprint();
        renderInventoryFingerprintInitialized = true;
        driveableData.setInventoryChanged(false);
        resizeProxyArrays();
        destroyedParts.clear();
        for (DriveablePart part : driveableData.getParts().values())
        {
            if (part.isDestroyed())
                destroyedParts.add(part.getType());
        }
        partSyncInitialized = false;
        setFuel(driveableData.getFuelInTank());
        refreshDimensions();
    }

    private void resizeProxyArrays()
    {
        if (configType == null)
            return;
        int seatCount = configType.getSeats().size();
        int wheelCount = configType.getWheelPositions().size();
        if (seats.length != seatCount)
            seats = Arrays.copyOf(seats, seatCount);
        if (wheels.length != wheelCount)
            wheels = Arrays.copyOf(wheels, wheelCount);
        if (passengerShootDelay.length != seatCount)
        {
            passengerShootDelay = Arrays.copyOf(passengerShootDelay, seatCount);
            passengerBurstRemaining = Arrays.copyOf(passengerBurstRemaining, seatCount);
            passengerHeldTicks = Arrays.copyOf(passengerHeldTicks, seatCount);
        }
    }

    @Override
    @Nullable
    public DriveableType getConfigType()
    {
        if (configType == null && InfoType.getInfoType(getShortName()) instanceof DriveableType type)
            initialize(type, ItemStack.EMPTY);
        return configType;
    }

    @Override
    public String getShortName()
    {
        String synced = entityData.get(DATA_DRIVEABLE_TYPE);
        return StringUtils.isBlank(synced) ? shortname : synced;
    }

    public void setShortName(@Nullable String value)
    {
        shortname = StringUtils.defaultString(value).trim();
        if (shortname.length() > 256)
            shortname = shortname.substring(0, 256);
        entityData.set(DATA_DRIVEABLE_TYPE, shortname);
    }

    public float getYaw() { return entityData.get(DATA_YAW); }
    public float getPitch() { return entityData.get(DATA_PITCH); }
    public float getRoll() { return entityData.get(DATA_ROLL); }
    public float getThrottle() { return entityData.get(DATA_THROTTLE); }
    public float getTurretYaw() { return entityData.get(DATA_TURRET_YAW); }
    public float getTurretPitch() { return entityData.get(DATA_TURRET_PITCH); }
    public float getFlightPitchControl() { return entityData.get(DATA_FLIGHT_PITCH); }
    public float getFlightRollControl() { return entityData.get(DATA_FLIGHT_ROLL); }
    public boolean isMouseControlEnabled() { return entityData.get(DATA_MOUSE_CONTROL); }
    public float getRecoilProgress() { return entityData.get(DATA_RECOIL_PROGRESS); }
    public float getIT1DoorAngle() { return entityData.get(DATA_IT1_DOOR_ANGLE); }
    public float getPrevIT1DoorAngle() { return entityData.get(DATA_PREV_IT1_DOOR_ANGLE); }
    public float getIT1ArmAngle() { return entityData.get(DATA_IT1_ARM_ANGLE); }
    public float getPrevIT1ArmAngle() { return entityData.get(DATA_PREV_IT1_ARM_ANGLE); }
    public float getIT1RailAngle() { return entityData.get(DATA_IT1_RAIL_ANGLE); }
    public float getPrevIT1RailAngle() { return entityData.get(DATA_PREV_IT1_RAIL_ANGLE); }
    public boolean isCanFireIT1() { return getFlag(FLAG_IT1_CAN_FIRE); }
    public boolean isReloadingDrakon() { return getFlag(FLAG_IT1_RELOADING); }
    public int getInputMask() { return entityData.get(DATA_INPUT_MASK); }
    public int getDriveableMode() { return entityData.get(DATA_MODE); }
    public float getFuel() { return entityData.get(DATA_FUEL); }

    protected void setYaw(float yaw)
    {
        if (!Float.isFinite(yaw))
            return;
        yaw = Mth.wrapDegrees(yaw);
        entityData.set(DATA_YAW, yaw);
        setYRot(yaw);
        axes.setAngles(yaw, getPitch(), getRoll());
    }

    protected void setPitch(float pitch)
    {
        if (!Float.isFinite(pitch))
            return;
        pitch = Mth.clamp(pitch, -89.9F, 89.9F);
        entityData.set(DATA_PITCH, pitch);
        setXRot(pitch);
        axes.setAngles(getYaw(), pitch, getRoll());
    }

    protected void setRoll(float roll)
    {
        if (!Float.isFinite(roll))
            return;
        roll = Mth.wrapDegrees(roll);
        entityData.set(DATA_ROLL, roll);
        axes.setAngles(getYaw(), getPitch(), roll);
    }

    public void setOrientation(float yaw, float pitch, float roll)
    {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || !Float.isFinite(roll))
            return;
        entityData.set(DATA_YAW, Mth.wrapDegrees(yaw));
        entityData.set(DATA_PITCH, Mth.clamp(pitch, -89.9F, 89.9F));
        entityData.set(DATA_ROLL, Mth.wrapDegrees(roll));
        setYRot(getYaw());
        setXRot(getPitch());
        axes.setAngles(getYaw(), getPitch(), getRoll());
    }

    protected void setThrottle(float throttle)
    {
        float maximum = configType == null ? 1F : Math.max(0F, configType.getMaxThrottle());
        float reverse = configType == null ? 1F : Math.max(0F, configType.getMaxNegativeThrottle());
        entityData.set(DATA_THROTTLE, Mth.clamp(Float.isFinite(throttle) ? throttle : 0F, -reverse, maximum));
    }

    protected void setTurretAim(float yaw, float pitch)
    {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch))
            return;
        entityData.set(DATA_TURRET_YAW, Mth.wrapDegrees(yaw));
        entityData.set(DATA_TURRET_PITCH, Mth.clamp(pitch, -89.9F, 89.9F));
    }

    protected void setFlightControls(float pitch, float roll, boolean mouseControl)
    {
        entityData.set(DATA_FLIGHT_PITCH, Mth.clamp(Float.isFinite(pitch) ? pitch : 0F, -1F, 1F));
        entityData.set(DATA_FLIGHT_ROLL, Mth.clamp(Float.isFinite(roll) ? roll : 0F, -1F, 1F));
        entityData.set(DATA_MOUSE_CONTROL, mouseControl && this instanceof Plane);
    }

    protected void setInputMask(int mask)
    {
        entityData.set(DATA_INPUT_MASK, DriveableInput.sanitize(mask));
    }

    protected void setDriveableMode(int mode)
    {
        entityData.set(DATA_MODE, Math.max(0, mode));
    }

    protected void setFuel(float fuel)
    {
        float tank = configType == null ? Math.max(0F, fuel) : configType.getFuelTankSize();
        float clamped = tank < 0F ? Math.max(0F, fuel) : Mth.clamp(fuel, 0F, Math.max(0F, tank));
        entityData.set(DATA_FUEL, clamped);
        if (driveableData != null)
            driveableData.setFuelInTank(clamped);
    }

    protected boolean getFlag(int flag)
    {
        return (entityData.get(DATA_FLAGS) & flag) != 0;
    }

    protected void setFlag(int flag, boolean value)
    {
        int flags = entityData.get(DATA_FLAGS);
        entityData.set(DATA_FLAGS, value ? flags | flag : flags & ~flag);
    }

    public boolean isGearDeployed() { return getFlag(FLAG_GEAR); }
    public boolean isDoorOpen() { return getFlag(FLAG_DOOR); }
    public boolean isWingFolded() { return getFlag(FLAG_WING); }
    public boolean isVarFlare() { return getFlag(FLAG_FLARE); }
    public boolean isEngineActive() { return getFlag(FLAG_ENGINE); }
    public void setGearDeployed(boolean value) { setFlag(FLAG_GEAR, value); }
    public void setDoorOpen(boolean value) { setFlag(FLAG_DOOR, value); }
    public void setWingFolded(boolean value) { setFlag(FLAG_WING, value); }

    public void setEntityMarker(int ticks)
    {
        markerTicks = Math.max(markerTicks, Math.max(0, ticks));
        isShowedPosition = markerTicks > 0;
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(DATA_DRIVEABLE_TYPE, StringUtils.EMPTY);
        entityData.define(DATA_YAW, 0F);
        entityData.define(DATA_PITCH, 0F);
        entityData.define(DATA_ROLL, 0F);
        entityData.define(DATA_THROTTLE, 0F);
        entityData.define(DATA_TURRET_YAW, 0F);
        entityData.define(DATA_TURRET_PITCH, 0F);
        entityData.define(DATA_FLIGHT_PITCH, 0F);
        entityData.define(DATA_FLIGHT_ROLL, 0F);
        entityData.define(DATA_MOUSE_CONTROL, false);
        entityData.define(DATA_RECOIL_PROGRESS, 0F);
        entityData.define(DATA_IT1_DOOR_ANGLE, 0F);
        entityData.define(DATA_PREV_IT1_DOOR_ANGLE, 0F);
        entityData.define(DATA_IT1_ARM_ANGLE, 0F);
        entityData.define(DATA_PREV_IT1_ARM_ANGLE, 0F);
        entityData.define(DATA_IT1_RAIL_ANGLE, 0F);
        entityData.define(DATA_PREV_IT1_RAIL_ANGLE, 0F);
        entityData.define(DATA_INPUT_MASK, 0);
        entityData.define(DATA_FLAGS, FLAG_GEAR);
        entityData.define(DATA_MODE, 0);
        entityData.define(DATA_FUEL, 0F);
        entityData.define(DATA_LOCK_TARGET, -1);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer)
    {
        CompoundTag state = new CompoundTag();
        writeRuntimeState(state);
        if (driveableData != null)
            driveableData.saveRenderState(state);
        buffer.writeUtf(getShortName(), 256);
        buffer.writeNbt(state);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer)
    {
        try
        {
            setShortName(buffer.readUtf(256));
            CompoundTag state = buffer.readNbt();
            if (state == null)
                state = new CompoundTag();
            readAdditionalSaveData(state);
        }
        catch (RuntimeException exception)
        {
            FlansMod.log.warn("Invalid driveable spawn data; discarding entity", exception);
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        String typeName = tag.contains(NBT_TYPE, Tag.TAG_STRING) ? tag.getString(NBT_TYPE) : tag.getString("Type");
        setShortName(typeName);
        if (!(InfoType.getInfoType(typeName) instanceof DriveableType type))
        {
            FlansMod.log.warn("Unknown driveable type {}, discarding entity", typeName);
            discard();
            return;
        }

        ItemStack savedSource = tag.contains(NBT_SOURCE_STACK, Tag.TAG_COMPOUND)
            ? ItemStack.of(tag.getCompound(NBT_SOURCE_STACK)) : ItemStack.EMPTY;
        initialize(type, savedSource);
        driveableData = new DriveableData(type, tag);
        weaponInventoryFingerprint = weaponInventoryFingerprint();
        weaponInventoryFingerprintInitialized = true;
        renderInventoryFingerprint = renderInventoryFingerprint();
        renderInventoryFingerprintInitialized = true;
        driveableData.setInventoryChanged(false);
        setFuel(driveableData.getFuelInTank());
        setOrientation(tag.getFloat(NBT_YAW), tag.getFloat(NBT_PITCH), tag.getFloat(NBT_ROLL));
        setThrottle(tag.getFloat(NBT_THROTTLE));
        setTurretAim(tag.getFloat(NBT_TURRET_YAW), tag.getFloat(NBT_TURRET_PITCH));
        entityData.set(DATA_FLAGS, tag.contains(NBT_FLAGS) ? tag.getInt(NBT_FLAGS) : FLAG_GEAR);
        setDriveableMode(tag.getInt(NBT_MODE));
        if (tag.hasUUID(NBT_OWNER))
            ownerId = tag.getUUID(NBT_OWNER);
        locked = tag.getBoolean(NBT_LOCKED);
        if (tag.contains(NBT_ENGINE_START_TICKS, Tag.TAG_INT))
            engineStartTicks = Math.max(0, tag.getInt(NBT_ENGINE_START_TICKS));
        primaryShootDelay = tag.contains(NBT_PRIMARY_SHOOT_DELAY, Tag.TAG_INT)
            ? Math.max(0, tag.getInt(NBT_PRIMARY_SHOOT_DELAY)) : 0;
        secondaryShootDelay = tag.contains(NBT_SECONDARY_SHOOT_DELAY, Tag.TAG_INT)
            ? Math.max(0, tag.getInt(NBT_SECONDARY_SHOOT_DELAY)) : 0;
        recoilTicksRemaining = tag.contains(NBT_RECOIL_TICKS, Tag.TAG_INT)
            ? Math.max(0, tag.getInt(NBT_RECOIL_TICKS)) : 0;
        recoilDuration = tag.contains(NBT_RECOIL_DURATION, Tag.TAG_INT)
            ? Math.max(recoilTicksRemaining, tag.getInt(NBT_RECOIL_DURATION)) : recoilTicksRemaining;
        entityData.set(DATA_RECOIL_PROGRESS, recoilDuration <= 0 ? 0F
            : Mth.clamp(1F - (float) recoilTicksRemaining / recoilDuration, 0F, 1F));
        it1Stage = tag.contains(NBT_IT1_STAGE, Tag.TAG_INT)
            ? Mth.clamp(tag.getInt(NBT_IT1_STAGE), 1, 8) : 8;
        it1ReloadDelay = tag.contains(NBT_IT1_RELOAD_DELAY, Tag.TAG_INT)
            ? Math.max(0, tag.getInt(NBT_IT1_RELOAD_DELAY)) : 0;
        setIT1Angles(tag.getFloat(NBT_IT1_DOOR_ANGLE), tag.getFloat(NBT_IT1_ARM_ANGLE),
            tag.getFloat(NBT_IT1_RAIL_ANGLE), true);
        setFlag(FLAG_IT1_CAN_FIRE, type.isIT1() && (!tag.contains(NBT_IT1_CAN_FIRE) || tag.getBoolean(NBT_IT1_CAN_FIRE)));
        setFlag(FLAG_IT1_RELOADING, type.isIT1() && tag.getBoolean(NBT_IT1_RELOADING));
        // Loading an existing entity (including client spawn data) must not replay placement effects.
        placementEffectsPending = false;
        resizeProxyArrays();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        DriveableType type = getConfigType();
        if (type == null || driveableData == null)
            return;
        writeRuntimeState(tag);
        if (!sourceStack.isEmpty())
        {
            CompoundTag sourceTag = new CompoundTag();
            sourceStack.save(sourceTag);
            tag.put(NBT_SOURCE_STACK, sourceTag);
        }
        driveableData.save(tag);
    }

    private void writeRuntimeState(@NotNull CompoundTag tag)
    {
        tag.putString(NBT_TYPE, getShortName());
        tag.putString("Type", getShortName());
        tag.putFloat(NBT_YAW, getYaw());
        tag.putFloat(NBT_PITCH, getPitch());
        tag.putFloat(NBT_ROLL, getRoll());
        tag.putFloat(NBT_THROTTLE, getThrottle());
        tag.putFloat(NBT_TURRET_YAW, getTurretYaw());
        tag.putFloat(NBT_TURRET_PITCH, getTurretPitch());
        tag.putInt(NBT_FLAGS, entityData.get(DATA_FLAGS));
        tag.putInt(NBT_MODE, getDriveableMode());
        if (ownerId != null)
            tag.putUUID(NBT_OWNER, ownerId);
        tag.putBoolean(NBT_LOCKED, locked);
        tag.putInt(NBT_ENGINE_START_TICKS, Math.max(0, engineStartTicks));
        tag.putInt(NBT_PRIMARY_SHOOT_DELAY, Math.max(0, primaryShootDelay));
        tag.putInt(NBT_SECONDARY_SHOOT_DELAY, Math.max(0, secondaryShootDelay));
        tag.putInt(NBT_RECOIL_TICKS, Math.max(0, recoilTicksRemaining));
        tag.putInt(NBT_RECOIL_DURATION, Math.max(0, recoilDuration));
        tag.putInt(NBT_IT1_STAGE, Mth.clamp(it1Stage, 1, 8));
        tag.putInt(NBT_IT1_RELOAD_DELAY, Math.max(0, it1ReloadDelay));
        tag.putBoolean(NBT_IT1_CAN_FIRE, isCanFireIT1());
        tag.putBoolean(NBT_IT1_RELOADING, isReloadingDrakon());
        tag.putFloat(NBT_IT1_DOOR_ANGLE, getIT1DoorAngle());
        tag.putFloat(NBT_IT1_ARM_ANGLE, getIT1ArmAngle());
        tag.putFloat(NBT_IT1_RAIL_ANGLE, getIT1RailAngle());
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);
        if (DATA_DRIVEABLE_TYPE.equals(key) && configType == null)
            getConfigType();
        if (DATA_YAW.equals(key) || DATA_PITCH.equals(key) || DATA_ROLL.equals(key))
            axes.setAngles(getYaw(), getPitch(), getRoll());
        if (DATA_FUEL.equals(key) && driveableData != null)
            driveableData.setFuelInTank(getFuel());
    }

    @Override
    public void tick()
    {
        super.tick();
        prevYaw = getYaw();
        prevPitch = getPitch();
        prevRoll = getRoll();
        prevTurretYaw = getTurretYaw();
        prevTurretPitch = getTurretPitch();

        DriveableType type = getConfigType();
        if (type == null || driveableData == null)
        {
            if (!level().isClientSide)
                discard();
            return;
        }

        axes.setAngles(getYaw(), getPitch(), getRoll());
        if (markerTicks > 0)
        {
            --markerTicks;
            isShowedPosition = markerTicks > 0;
        }

        if (level().isClientSide)
        {
            tickClientDriveable();
            return;
        }

        if (++proxyCheckTicker >= CHILD_REPAIR_INTERVAL || tickCount <= 1)
        {
            proxyCheckTicker = 0;
            ensureProxyEntities();
        }

        updatePartState();
        if (destroyed || isRemoved())
            return;

        if (++inputTimeout > INPUT_TIMEOUT_TICKS
            && (getInputMask() != 0 || getFlightPitchControl() != 0F || getFlightRollControl() != 0F))
        {
            previousInputMask = getInputMask();
            setInputMask(0);
            setFlightControls(0F, 0F, isMouseControlEnabled());
        }

        int previousPrimaryShootDelay = primaryShootDelay;
        if (primaryShootDelay > 0)
            --primaryShootDelay;
        if (secondaryShootDelay > 0)
            --secondaryShootDelay;
        tickTimedWeaponSounds(previousPrimaryShootDelay);
        applyPlacementEffects();
        if (flareDelay > 0)
            --flareDelay;
        updateFlares();
        refuelFromInventory();
        updateRiderVisibility();
        updateEngineState();
        updateLockOnTargeting();
        tickDriveable();
        if (collisionHelper != null)
            collisionHelper.tick(this);
        tickWeapons();
        tickWeaponAnimations();
        tickSounds();
        previousInputMask = getInputMask();
        for (Seat seat : seats)
        {
            if (seat != null)
                seat.finishInputTick();
        }
        emitConfiguredParticles();
        updateProxyPositions();
        syncChangedPartState();
        syncRenderInventoryState();
        updateLifetime();
    }

    /** Subclass server physics tick. */
    protected abstract void tickDriveable();

    /** Lightweight visual state update; world simulation remains server-owned. */
    protected void tickClientDriveable() {}

    private void tickTimedWeaponSounds(int previousPrimaryShootDelay)
    {
        if (configType == null || configType.getReloadSoundTick() == RELOAD_SOUND_TICK_UNSET
            || previousPrimaryShootDelay <= primaryShootDelay
            || primaryShootDelay != configType.getReloadSoundTick()
            || StringUtils.isBlank(configType.getShootReloadSound()))
            return;
        PacketPlaySound.sendSoundPacket(this, ModCommonConfig.get().soundRange(), configType.getShootReloadSound(), false);
    }

    private void applyPlacementEffects()
    {
        if (!placementEffectsPending || configType == null)
            return;
        placementEffectsPending = false;
        primaryShootDelay = Math.max(primaryShootDelay, Math.max(0, configType.getPlaceTimePrimary()));
        secondaryShootDelay = Math.max(secondaryShootDelay, Math.max(0, configType.getPlaceTimeSecondary()));

        String primarySound = configType.getPlaceSoundPrimary();
        String secondarySound = configType.getPlaceSoundSecondary();
        if (StringUtils.isNotBlank(primarySound))
            PacketPlaySound.sendSoundPacket(this, ModCommonConfig.get().soundRange(), primarySound, false);
        if (StringUtils.isNotBlank(secondarySound) && !secondarySound.equals(primarySound))
            PacketPlaySound.sendSoundPacket(this, ModCommonConfig.get().soundRange(), secondarySound, false);
    }

    protected void updateEngineState()
    {
        if (configType == null)
            return;
        boolean flooded = isUnderWater() && !configType.isWorksUnderWater();
        if (flooded)
            setThrottle(0F);
        if (engineStartTicks > 0)
            --engineStartTicks;
        boolean occupied = getControllingEntity() != null;
        boolean ready = occupied && !flooded && hasFuelForEngine() && engineStartTicks <= 0;
        engineRequested = ready && Math.abs(getThrottle()) > 0.001F;
        setFlag(FLAG_ENGINE, ready);
    }

    protected void tickWeapons()
    {
        if (configType == null || driveableData == null || level().isClientSide)
            return;

        handleInventoryReloadState();
        boolean primaryDown = DriveableInput.isDown(getInputMask(), DriveableInput.PRIMARY_FIRE);
        boolean secondaryDown = DriveableInput.isDown(getInputMask(), DriveableInput.SECONDARY_FIRE);
        primaryHeldTicks = primaryDown ? primaryHeldTicks + 1 : 0;
        secondaryHeldTicks = secondaryDown ? secondaryHeldTicks + 1 : 0;

        EnumFireMode primaryMode = configType.getModePrimary();
        EnumFireMode secondaryMode = configType.getModeSecondary();
        boolean primaryRising = primaryDown && !DriveableInput.isDown(previousInputMask, DriveableInput.PRIMARY_FIRE);
        boolean secondaryRising = secondaryDown && !DriveableInput.isDown(previousInputMask, DriveableInput.SECONDARY_FIRE);
        if (primaryMode == EnumFireMode.BURST && primaryRising)
            primaryBurstRemaining = 3;
        if (secondaryMode == EnumFireMode.BURST && secondaryRising)
            secondaryBurstRemaining = 3;

        if (primaryShootDelay <= 0 && shouldFire(primaryMode, primaryDown, primaryRising, primaryHeldTicks, primaryBurstRemaining))
        {
            if (fireWeaponBank(false))
            {
                primaryShootDelay = Math.max(1, Mth.ceil(getConfiguredShootDelay(false)));
                if (primaryMode == EnumFireMode.BURST && primaryBurstRemaining > 0)
                    --primaryBurstRemaining;
            }
        }
        if (secondaryShootDelay <= 0 && shouldFire(secondaryMode, secondaryDown, secondaryRising, secondaryHeldTicks, secondaryBurstRemaining))
        {
            if (fireWeaponBank(true))
            {
                secondaryShootDelay = Math.max(1, Mth.ceil(getConfiguredShootDelay(true)));
                if (secondaryMode == EnumFireMode.BURST && secondaryBurstRemaining > 0)
                    --secondaryBurstRemaining;
            }
        }

        tickPassengerGuns();
    }

    protected float getConfiguredShootDelay(boolean secondary)
    {
        float shared = configType == null ? -1F : configType.shootDelay(secondary);
        if (shared >= 0F)
            return shared;
        if (configType instanceof PlaneType plane)
            return secondary ? plane.getPlaneBombDelay() : plane.getPlaneShootDelay();
        if (configType instanceof VehicleType vehicle)
            return secondary ? vehicle.getVehicleShellDelay() : vehicle.getVehicleShootDelay();
        return 1F;
    }

    private static boolean shouldFire(EnumFireMode mode, boolean held, boolean rising, int heldTicks, int burstRemaining)
    {
        return switch (mode)
        {
            case SEMIAUTO -> rising;
            case BURST -> burstRemaining > 0;
            case MINIGUN -> held && heldTicks >= 10;
            case FULLAUTO -> held;
        };
    }

    protected boolean canFireWeaponBank(boolean secondary)
    {
        if (configType == null || !configType.isWorksUnderWater() && isUnderWater())
            return false;
        return !configType.isIT1() || configType.weaponType(secondary) != EnumWeaponType.MISSILE || isCanFireIT1();
    }

    protected void tickWeaponAnimations()
    {
        tickRecoilAnimation();
        if (configType != null && configType.isIT1())
            tickIT1Reload();
    }

    private void beginRecoil()
    {
        if (configType == null || configType.getRecoilTime() <= 0F)
            return;
        recoilDuration = Math.max(1, Mth.ceil(configType.getRecoilTime()));
        recoilTicksRemaining = recoilDuration;
        entityData.set(DATA_RECOIL_PROGRESS, 0F);
    }

    private void tickRecoilAnimation()
    {
        if (recoilTicksRemaining > 0 && recoilDuration > 0)
        {
            int elapsed = recoilDuration - recoilTicksRemaining + 1;
            entityData.set(DATA_RECOIL_PROGRESS, Mth.clamp((float) elapsed / recoilDuration, 0F, 1F));
            --recoilTicksRemaining;
        }
        else if (getRecoilProgress() != 0F)
        {
            recoilTicksRemaining = 0;
            recoilDuration = 0;
            entityData.set(DATA_RECOIL_PROGRESS, 0F);
        }
    }

    private void beginIT1Reload()
    {
        it1Stage = 1;
        it1ReloadDelay = 0;
        setFlag(FLAG_IT1_CAN_FIRE, false);
        setFlag(FLAG_IT1_RELOADING, false);
    }

    private void tickIT1Reload()
    {
        if (driveableData == null)
            return;

        float door = getIT1DoorAngle();
        float arm = getIT1ArmAngle();
        float rail = getIT1RailAngle();
        if (it1ReloadDelay > 0)
        {
            --it1ReloadDelay;
            setFlag(FLAG_IT1_RELOADING, true);
            setIT1Angles(door, arm, rail, false);
            return;
        }

        switch (Mth.clamp(it1Stage, 1, 8))
        {
            case 1 -> {
                door = Mth.approach(door, 0F, 5F);
                arm = Mth.approach(arm, 0F, 3F);
                rail = Mth.approach(rail, -10F, 5F);
                if (rail == -10F)
                    it1Stage = 2;
            }
            case 2 -> {
                door = Mth.approach(door, -90F, 5F);
                arm = Mth.approach(arm, 0F, 3F);
                rail = Mth.approach(rail, -10F, 1F);
                if (door == -90F)
                    it1Stage = 3;
            }
            case 3 -> {
                door = Mth.approach(door, -90F, 5F);
                arm = Mth.approach(arm, 179F, 3F);
                rail = Mth.approach(rail, -10F, 1F);
                if (arm == 179F)
                    it1Stage = 4;
            }
            case 4 -> {
                door = Mth.approach(door, 0F, 10F);
                arm = Mth.approach(arm, 180F, 3F);
                rail = Mth.approach(rail, -10F, 1F);
                if (door == 0F && hasLoadedIT1Missile())
                {
                    it1Stage = 5;
                    it1ReloadDelay = 60;
                    door = Mth.approach(door, -90F, 10F);
                    setFlag(FLAG_IT1_RELOADING, true);
                }
            }
            case 5 -> {
                door = Mth.approach(door, -90F, 10F);
                arm = Mth.approach(arm, 180F, 3F);
                rail = Mth.approach(rail, -10F, 1F);
                setFlag(FLAG_IT1_RELOADING, true);
                if (door == -90F)
                    it1Stage = 6;
            }
            case 6 -> {
                door = Mth.approach(door, -90F, 5F);
                arm = Mth.approach(arm, 0F, 3F);
                rail = Mth.approach(rail, -10F, 1F);
                if (arm == 0F)
                    it1Stage = 7;
            }
            case 7 -> {
                door = Mth.approach(door, 0F, 10F);
                arm = Mth.approach(arm, 0F, 3F);
                rail = Mth.approach(rail, 0F, 1F);
                if (rail == 0F && door == 0F)
                {
                    it1Stage = 8;
                    setFlag(FLAG_IT1_CAN_FIRE, true);
                    setFlag(FLAG_IT1_RELOADING, false);
                }
            }
            case 8 -> {
                Seat driver = getSeat(0);
                SeatInfo info = configType.getSeat(0);
                float speed = info == null ? 2F : Math.max(0.1F, Math.abs(info.getAimingSpeed().y));
                rail = Mth.approach(rail, driver == null ? -getTurretPitch() : -driver.getAimPitch(), speed);
                if (!hasLoadedIT1Missile())
                    beginIT1Reload();
            }
        }
        setIT1Angles(door, arm, rail, false);
    }

    private boolean hasLoadedIT1Missile()
    {
        for (int slot = 0; slot < driveableData.getNumMissileSlots(); slot++)
        {
            if (validAmmo(driveableData.getMissile(slot), EnumWeaponType.MISSILE))
                return true;
        }
        return false;
    }

    private void setIT1Angles(float door, float arm, float rail, boolean snapPrevious)
    {
        float safeDoor = Float.isFinite(door) ? door : 0F;
        float safeArm = Float.isFinite(arm) ? arm : 0F;
        float safeRail = Float.isFinite(rail) ? rail : 0F;
        entityData.set(DATA_PREV_IT1_DOOR_ANGLE, snapPrevious ? safeDoor : getIT1DoorAngle());
        entityData.set(DATA_PREV_IT1_ARM_ANGLE, snapPrevious ? safeArm : getIT1ArmAngle());
        entityData.set(DATA_PREV_IT1_RAIL_ANGLE, snapPrevious ? safeRail : getIT1RailAngle());
        entityData.set(DATA_IT1_DOOR_ANGLE, safeDoor);
        entityData.set(DATA_IT1_ARM_ANGLE, safeArm);
        entityData.set(DATA_IT1_RAIL_ANGLE, safeRail);
    }

    public boolean isUnderWater()
    {
        if (configType == null)
            return isInWater();
        if (underWaterCheckTick == tickCount)
            return underWaterCached;

        // Legacy MaxDepth tests the driveable box shifted upwards. Sampling
        // fluid states directly preserves that behavior without allocating a
        // stream or forcing unloaded chunks to load.
        AABB probe = getBoundingBox().move(0D, Mth.clamp(configType.getMaxDepth(), 0, 64), 0D);
        int minX = Mth.floor(probe.minX);
        int minY = Mth.floor(probe.minY);
        int minZ = Mth.floor(probe.minZ);
        int maxX = Mth.floor(probe.maxX - 1.0E-7D);
        int maxY = Mth.floor(probe.maxY - 1.0E-7D);
        int maxZ = Mth.floor(probe.maxZ - 1.0E-7D);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean liquid = false;
        outer:
        for (int blockY = minY; blockY <= maxY; blockY++)
        {
            for (int blockX = minX; blockX <= maxX; blockX++)
            {
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++)
                {
                    cursor.set(blockX, blockY, blockZ);
                    if (level().hasChunkAt(cursor) && !level().getFluidState(cursor).isEmpty())
                    {
                        liquid = true;
                        break outer;
                    }
                }
            }
        }
        underWaterCheckTick = tickCount;
        underWaterCached = liquid;
        return liquid;
    }

    /** Compatibility flag used by carrier / vehicle-seat integrations. */
    public boolean canMountEntity()
    {
        return configType != null && configType.isCanMountEntity();
    }

    protected void handleInventoryReloadState()
    {
        if (!driveableData.isInventoryChanged())
            return;
        int fingerprint = weaponInventoryFingerprint();
        boolean ammunitionChanged = !weaponInventoryFingerprintInitialized || fingerprint != weaponInventoryFingerprint;
        weaponInventoryFingerprint = fingerprint;
        weaponInventoryFingerprintInitialized = true;
        driveableData.setInventoryChanged(false);
        if (!ammunitionChanged)
            return;
        primaryShootDelay = Math.max(primaryShootDelay, Math.max(0, configType.getReloadTimePrimary()));
        secondaryShootDelay = Math.max(secondaryShootDelay, Math.max(0, configType.getReloadTimeSecondary()));
        String sound = StringUtils.firstNonBlank(configType.getShootReloadSound(), configType.getReloadSoundPrimary(), configType.getReloadSoundSecondary());
        if (StringUtils.isNotBlank(sound))
            PacketPlaySound.sendSoundPacket(this, 96D, sound, false);
    }

    protected int weaponInventoryFingerprint()
    {
        if (driveableData == null)
            return 0;
        int result = 1;
        int end = Math.min(driveableData.getCargoInventoryStart(), driveableData.getContainerSize());
        for (int slot = 0; slot < end; slot++)
        {
            ItemStack stack = driveableData.getItem(slot);
            result = 31 * result + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
            result = 31 * result + stack.getCount();
            result = 31 * result + stack.getDamageValue();
            result = 31 * result + (stack.hasTag() ? stack.getTag().hashCode() : 0);
        }
        return result;
    }

    protected int renderInventoryFingerprint()
    {
        if (driveableData == null)
            return 0;
        int result = 31 + driveableData.getPaintjobID();
        for (int index = 0; index < driveableData.getRenderSlotCount(); index++)
        {
            ItemStack stack = driveableData.getItem(driveableData.getRenderSlotIndex(index));
            result = 31 * result + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
            result = 31 * result + stack.getCount();
            result = 31 * result + stack.getDamageValue();
            result = 31 * result + (stack.hasTag() ? stack.getTag().hashCode() : 0);
        }
        return result;
    }

    private void syncRenderInventoryState()
    {
        int fingerprint = renderInventoryFingerprint();
        if (renderInventoryFingerprintInitialized && fingerprint == renderInventoryFingerprint)
            return;
        renderInventoryFingerprint = fingerprint;
        renderInventoryFingerprintInitialized = true;
        PacketHandler.sendToTracking(new PacketDriveableRenderState(this), this);
    }

    public void applyRenderInventoryNetworkState(int paintjobId, int[] slots, ItemStack[] stacks)
    {
        if (!level().isClientSide || driveableData == null || slots == null || stacks == null
            || slots.length != stacks.length || slots.length > DriveableData.MAX_RENDER_SYNC_SLOTS)
            return;
        driveableData.setPaintjobID(paintjobId);
        for (int index = 0; index < slots.length; index++)
            driveableData.applyRenderSlot(slots[index], stacks[index]);
        renderInventoryFingerprint = renderInventoryFingerprint();
        renderInventoryFingerprintInitialized = true;
    }

    protected void acknowledgeInternalWeaponInventoryChange()
    {
        weaponInventoryFingerprint = weaponInventoryFingerprint();
        weaponInventoryFingerprintInitialized = true;
        driveableData.setInventoryChanged(false);
    }

    protected boolean fireWeaponBank(boolean secondary)
    {
        if (!canFireWeaponBank(secondary) || getControllingEntity() == null)
            return false;
        EnumWeaponType weapon = configType.weaponType(secondary);
        if (weapon == EnumWeaponType.NONE || !weaponEnabled(weapon))
            return false;

        List<ShootPoint> points = configType.shootPoints(secondary);
        if (points.isEmpty())
            return false;
        if (MinecraftForge.EVENT_BUS.post(new GunFiredEvent(this)))
            return false;
        List<ShootPoint> selected;
        if (configType.alternate(secondary))
        {
            int index = secondary ? secondaryShootPointIndex : primaryShootPointIndex;
            ShootPoint point = points.get(Math.floorMod(index, points.size()));
            selected = List.of(point);
            if (secondary)
                secondaryShootPointIndex = (index + 1) % points.size();
            else
                primaryShootPointIndex = (index + 1) % points.size();
        }
        else
            selected = points;

        boolean fired = false;
        List<ShootPoint> firedPoints = new ArrayList<>();
        for (ShootPoint point : selected)
        {
            if (point == null || !isPartIntact(point.getRootPos().getPart()))
                continue;
            boolean pointFired = fireFromPoint(point, weapon, secondary,
                getControllingEntity() instanceof LivingEntity living ? living : null);
            fired |= pointFired;
            if (pointFired)
                firedPoints.add(point);
        }
        if (fired)
        {
            playBankEffects(secondary, firedPoints);
            if (weapon == EnumWeaponType.SHELL)
                beginRecoil();
            if (configType.isIT1() && weapon == EnumWeaponType.MISSILE)
                beginIT1Reload();
        }
        return fired;
    }

    protected boolean weaponEnabled(EnumWeaponType weapon)
    {
        return switch (weapon)
        {
            case BOMB, MINE -> FlansMod.teamsManager.isBombsEnabled();
            case SHELL -> FlansMod.teamsManager.isShellsEnabled();
            case GUN -> FlansMod.teamsManager.isBulletsEnabled();
            default -> true;
        };
    }

    protected boolean fireFromPoint(ShootPoint point, EnumWeaponType weapon, boolean secondary, @Nullable LivingEntity attacker)
    {
        AmmoSelection selection = selectAmmo(point, weapon);
        if (selection == null || !ShootableItem.hasRoundsLeft(selection.stack()))
            return false;
        if (!(selection.stack().getItem() instanceof ShootableItem item) || !(item.getConfigType() instanceof BulletType bulletType))
            return false;

        FireableGun fireable;
        int bulletCount;
        if (selection.gunType() != null)
        {
            fireable = new FireableGun(selection.gunType(), selection.stack());
            if (configType.isRangingGun() && configType.getBulletSpeed() > 0F)
                fireable = new FireableGun(fireable.getType(), fireable.getDamage(), fireable.getSpread(),
                    configType.getBulletSpeed(), fireable.getSpreadPattern());
            bulletCount = Math.max(1, selection.gunType().getNumBullets(null, bulletType));
        }
        else
        {
            float multiplier = secondary ? configType.getDamageMultiplierSecondary() : configType.getDamageMultiplierPrimary();
            float speed = configType.getBulletSpeed() > 0F ? configType.getBulletSpeed() : bulletType.getBulletSpeed(true);
            fireable = new FireableGun(configType, Math.max(0F, multiplier), Math.max(0F, configType.getBulletSpread()),
                Math.max(0.01F, speed), EnumSpreadPattern.CIRCLE);
            bulletCount = Math.max(1, bulletType.getNumBullets());
        }
        if (selection.gunType() != null)
            fireable.multiplyDamage(secondary ? configType.getDamageMultiplierSecondary() : configType.getDamageMultiplierPrimary());

        Vec3 origin = getShootOrigin(point);
        Vec3 direction = getShootDirection(point, secondary);
        FiredShot shot = new FiredShot(fireable, bulletType, this, attacker, ShootableItem.getRoundsRemaining(selection.stack()));
        boolean creative = attacker instanceof Player player && player.getAbilities().instabuild;
        ShootingHelper.fireGun(level(), shot, bulletCount, origin, direction, () -> {
            if (!creative)
                consumeAmmo(selection);
        });
        return true;
    }

    @Nullable
    protected AmmoSelection selectAmmo(ShootPoint point, EnumWeaponType weapon)
    {
        if (point.getRootPos() instanceof PilotGun pilotGun)
        {
            int slot = configType.getPilotGuns().indexOf(pilotGun);
            if (slot < 0 || slot >= driveableData.getNumAmmoSlots())
                return null;
            ItemStack stack = driveableData.getAmmo(slot);
            if (!validAmmo(stack, weapon))
                return null;
            return new AmmoSelection(AmmoBank.AMMO, slot, stack, pilotGun.getType());
        }

        if (weapon == EnumWeaponType.GUN)
        {
            for (int slot = 0; slot < configType.getPilotGuns().size(); slot++)
            {
                ItemStack stack = driveableData.getAmmo(slot);
                if (validAmmo(stack, weapon))
                    return new AmmoSelection(AmmoBank.AMMO, slot, stack, configType.getPilotGuns().get(slot).getType());
            }
            return null;
        }

        boolean bombBank = weapon == EnumWeaponType.BOMB || weapon == EnumWeaponType.MINE;
        int size = bombBank ? driveableData.getNumBombSlots() : driveableData.getNumMissileSlots();
        for (int slot = 0; slot < size; slot++)
        {
            ItemStack stack = bombBank ? driveableData.getBomb(slot) : driveableData.getMissile(slot);
            if (validAmmo(stack, weapon))
                return new AmmoSelection(bombBank ? AmmoBank.BOMB : AmmoBank.MISSILE, slot, stack, null);
        }
        return null;
    }

    protected boolean validAmmo(ItemStack stack, EnumWeaponType requested)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ShootableItem item) || !(item.getConfigType() instanceof BulletType bulletType))
            return false;
        return ShootableItem.hasRoundsLeft(stack) && configType.isValidAmmo(bulletType)
            && (bulletType.getWeaponType() == requested || requested == EnumWeaponType.GUN && bulletType.getWeaponType() == EnumWeaponType.NONE);
    }

    protected void consumeAmmo(AmmoSelection selection)
    {
        ItemStack stack = selection.stack();
        if (!ShootableItem.consumeRound(stack))
            return;
        if (!ShootableItem.hasRoundsLeft(stack))
            stack = ItemStack.EMPTY;
        switch (selection.bank())
        {
            case AMMO -> driveableData.setAmmo(selection.slot(), stack);
            case BOMB -> driveableData.setBomb(selection.slot(), stack);
            case MISSILE -> driveableData.setMissile(selection.slot(), stack);
        }
        acknowledgeInternalWeaponInventoryChange();
    }

    protected Vec3 getShootOrigin(ShootPoint point)
    {
        Vec3 root = new Vec3(point.getRootPos().getPosition().x, point.getRootPos().getPosition().y, point.getRootPos().getPosition().z);
        Vec3 offset = new Vec3(point.getOffPos().x, point.getOffPos().y, point.getOffPos().z);
        EnumDriveablePart part = point.getRootPos().getPart();
        if (!isTurretMountedPart(part))
            return localToWorld(root.x, root.y, root.z).add(localDirectionToWorld(offset));

        boolean separateMuzzleOffset = offset.lengthSqr() > 1.0E-8D;
        float rootPitch = part == EnumDriveablePart.BARREL || !separateMuzzleOffset ? getTurretPitch() : 0F;
        Vec3 origin = turretPointToWorld(root, getTurretYaw(), rootPitch);
        Vec3 muzzleOffset = rotateTurretLocalDirection(offset, getTurretYaw(), getTurretPitch());
        return origin.add(localDirectionToWorld(muzzleOffset));
    }

    protected Vec3 getShootDirection(ShootPoint point, boolean secondary)
    {
        boolean fixed = secondary ? configType.isFixedSecondaryFire() : configType.isFixedPrimaryFire();
        com.flansmod.common.vector.Vector3f fixedAngle = secondary ? configType.getSecondaryFireAngle() : configType.getPrimaryFireAngle();
        EnumDriveablePart part = point.getRootPos().getPart();
        if (fixed)
        {
            Vec3 localDirection = new Vec3(fixedAngle.x, fixedAngle.y, fixedAngle.z);
            if (localDirection.lengthSqr() < 1.0E-8D)
                localDirection = new Vec3(1D, 0D, 0D);
            if (isTurretMountedPart(part))
            {
                float pitch = part == EnumDriveablePart.BARREL ? getTurretPitch() : 0F;
                localDirection = rotateTurretLocalDirection(localDirection, getTurretYaw(), pitch);
            }
            return localDirectionToWorld(localDirection).normalize();
        }
        if (isTurretMountedPart(part))
            return aimedDirection(getTurretYaw(), getTurretPitch());
        return getForwardVector();
    }

    protected static boolean isTurretMountedPart(@Nullable EnumDriveablePart part)
    {
        return part == EnumDriveablePart.TURRET || part == EnumDriveablePart.BARREL
            || part != null && part.name().startsWith("TURRET_");
    }

    /** Rotate a vector expressed in forward / up / right driveable coordinates. */
    protected static Vec3 rotateTurretLocalDirection(@NotNull Vec3 vector, float yaw, float pitch)
    {
        double yawRadians = yaw * Mth.DEG_TO_RAD;
        double pitchRadians = pitch * Mth.DEG_TO_RAD;
        double yawCos = Math.cos(yawRadians);
        double yawSin = Math.sin(yawRadians);
        double pitchCos = Math.cos(pitchRadians);
        double pitchSin = Math.sin(pitchRadians);

        double pitchX = vector.x * pitchCos + vector.y * pitchSin;
        double pitchY = -vector.x * pitchSin + vector.y * pitchCos;
        return new Vec3(pitchX * yawCos + vector.z * yawSin, pitchY,
            -pitchX * yawSin + vector.z * yawCos);
    }

    protected Vec3 turretPointToWorld(@NotNull Vec3 point, float yaw, float pitch)
    {
        if (configType == null)
            return localToWorld(point.x, point.y, point.z);
        com.flansmod.common.vector.Vector3f configuredOrigin = configType.getTurretOrigin();
        Vec3 pivot = new Vec3(configuredOrigin.x, configuredOrigin.y, configuredOrigin.z);
        Vec3 rotated = rotateTurretLocalDirection(point.subtract(pivot), yaw, pitch).add(pivot);
        com.flansmod.common.vector.Vector3f configuredOffset = configType.getTurretOriginOffset();
        Vec3 originOffset = rotateTurretLocalDirection(new Vec3(configuredOffset.x, configuredOffset.y, configuredOffset.z), yaw, 0F);
        Vec3 local = rotated.add(originOffset);
        return localToWorld(local.x, local.y, local.z);
    }

    protected Vec3 aimedDirection(float yaw, float pitch)
    {
        return localDirectionToWorld(rotateTurretLocalDirection(new Vec3(1D, 0D, 0D), yaw, pitch)).normalize();
    }

    protected void playBankEffects(boolean secondary, List<ShootPoint> firedPoints)
    {
        String sound = secondary ? configType.getShootSoundSecondary() : configType.getShootSoundPrimary();
        if (StringUtils.isNotBlank(sound))
            PacketPlaySound.sendSoundPacket(this, 128D, sound, true);
        List<DriveableType.ShootParticle> particles = secondary ? configType.getShootParticlesSecondary() : configType.getShootParticlesPrimary();
        for (ShootPoint point : firedPoints)
        {
            Vec3 origin = getShootOrigin(point);
            EnumDriveablePart part = point.getRootPos().getPart();
            for (DriveableType.ShootParticle particle : particles)
            {
                Vec3 localDirection = new Vec3(particle.x(), particle.y(), particle.z());
                if (isTurretMountedPart(part))
                    localDirection = rotateTurretLocalDirection(localDirection, getTurretYaw(), getTurretPitch());
                Vec3 direction = localDirectionToWorld(localDirection);
                PacketHandler.sendToAllAround(new PacketParticle(particle.name(), origin.x, origin.y, origin.z,
                    direction.x, direction.y, direction.z), origin, 128D, level().dimension());
            }
        }
    }

    protected void tickPassengerGuns()
    {
        for (int index = 0; index < seats.length; index++)
        {
            if (passengerShootDelay[index] > 0)
                --passengerShootDelay[index];
            Seat seat = seats[index];
            SeatInfo info = seat == null ? null : seat.getSeatInfo();
            GunType gun = info == null ? null : info.getGunType();
            if (seat == null || info == null || gun == null || seat.getRiddenByEntity() == null || !isPartIntact(info.getPart()))
                continue;

            boolean held = seat.isInputDown(DriveableInput.PRIMARY_FIRE);
            boolean rising = seat.isInputRising(DriveableInput.PRIMARY_FIRE);
            passengerHeldTicks[index] = held ? passengerHeldTicks[index] + 1 : 0;
            EnumFireMode mode = gun.getFireMode(null);
            if (mode == EnumFireMode.BURST && rising)
                passengerBurstRemaining[index] = Math.max(1, gun.getNumBurstRounds());
            if (passengerShootDelay[index] > 0 || !shouldFire(mode, held, rising, passengerHeldTicks[index], passengerBurstRemaining[index]))
                continue;

            int ammoSlot = configType.getPilotGuns().size() + Math.max(0, info.getGunnerID());
            ItemStack ammo = driveableData.getAmmo(ammoSlot);
            if (!validAmmo(ammo, EnumWeaponType.GUN) || !(ammo.getItem() instanceof ShootableItem shootable)
                || !(shootable.getConfigType() instanceof BulletType bulletType))
                continue;
            if (MinecraftForge.EVENT_BUS.post(new GunFiredEvent(this)))
                continue;

            FireableGun fireable = new FireableGun(gun, ammo);
            LivingEntity attacker = seat.getRiddenByEntity() instanceof LivingEntity living ? living : null;
            Vec3 gunOrigin = new Vec3(info.getGunOrigin().x, info.getGunOrigin().y, info.getGunOrigin().z);
            Vec3 origin = isTurretMountedPart(info.getPart())
                ? turretPointToWorld(gunOrigin, seat.getAimYaw(), info.getPart() == EnumDriveablePart.BARREL ? seat.getAimPitch() : 0F)
                : localToWorld(gunOrigin.x, gunOrigin.y, gunOrigin.z);
            Vec3 direction = aimedDirection(seat.getAimYaw(), seat.getAimPitch());
            FiredShot shot = new FiredShot(fireable, bulletType, this, attacker, ShootableItem.getRoundsRemaining(ammo));
            boolean creative = attacker instanceof Player player && player.getAbilities().instabuild;
            ShootingHelper.fireGun(level(), shot, Math.max(1, gun.getNumBullets(null, bulletType)), origin, direction, () -> {
                if (!creative)
                {
                    ShootableItem.consumeRound(ammo);
                    driveableData.setAmmo(ammoSlot, ShootableItem.hasRoundsLeft(ammo) ? ammo : ItemStack.EMPTY);
                    acknowledgeInternalWeaponInventoryChange();
                }
            });
            passengerShootDelay[index] = Math.max(1, Mth.ceil(gun.getShootDelay(null)));
            if (mode == EnumFireMode.BURST && passengerBurstRemaining[index] > 0)
                --passengerBurstRemaining[index];
            String sound = gun.getShootSound(null, !ShootableItem.hasRoundsLeft(ammo));
            if (StringUtils.isNotBlank(sound))
                PacketPlaySound.sendSoundPacket(this, gun.getGunSoundRange(), sound, true);
        }
    }

    protected enum AmmoBank { AMMO, BOMB, MISSILE }
    protected record AmmoSelection(AmmoBank bank, int slot, ItemStack stack, @Nullable GunType gunType) {}

    protected void updateLifetime()
    {
        if (getControllingEntity() != null)
            ticksSinceUsed = 0;
        else
            ++ticksSinceUsed;
        int lifeSeconds = getLifetimeSeconds();
        if (lifeSeconds > 0 && ticksSinceUsed > lifeSeconds * 20)
        {
            suppressDrops = true;
            discard();
        }
    }

    protected int getLifetimeSeconds()
    {
        if (this instanceof Plane)
            return FlansMod.teamsManager.getPlaneLife();
        if (this instanceof Mecha)
            return FlansMod.teamsManager.getMechaLife();
        return FlansMod.teamsManager.getVehicleLife();
    }

    public void markUsed()
    {
        ticksSinceUsed = 0;
    }

    protected void updatePartState()
    {
        if (driveableData == null)
            return;
        for (DriveablePart part : driveableData.getParts().values())
        {
            boolean wasDestroyed = part.isDestroyed();
            part.tick();
            if (!wasDestroyed && part.isDestroyed() || part.isDestroyed() && !destroyedParts.contains(part.getType()))
                onPartDestroyed(part.getType());
            if (part.isOnFire() && tickCount % 4 == 0)
            {
                CollisionBox box = part.getBox();
                Vec3 position = box == null ? position() : localToWorld(box.getCentre().x, box.getCentre().y, box.getCentre().z);
                PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.FM_FLAME, position.x, position.y, position.z, 0D, 0.02D, 0D),
                    position, 96D, level().dimension());
            }
        }
    }

    protected void syncChangedPartState()
    {
        if (level().isClientSide || driveableData == null || isRemoved())
            return;
        List<DriveablePart> changed = new ArrayList<>();
        for (DriveablePart part : driveableData.getParts().values())
        {
            int ordinal = part.getType().ordinal();
            byte flags = (byte) ((part.isOnFire() ? 1 : 0) | (part.isDead() ? 2 : 0));
            if (!partSyncInitialized || Float.floatToIntBits(syncedPartHealth[ordinal]) != Float.floatToIntBits(part.getHealth())
                || syncedPartFireTicks[ordinal] != part.getFireTime() || syncedPartFlags[ordinal] != flags)
            {
                changed.add(part);
                syncedPartHealth[ordinal] = part.getHealth();
                syncedPartFireTicks[ordinal] = part.getFireTime();
                syncedPartFlags[ordinal] = flags;
            }
        }
        partSyncInitialized = true;
        if (!changed.isEmpty())
            PacketHandler.sendToAllAround(new PacketDriveableDamage(getId(), changed), position(), 192D, level().dimension());
    }

    /** Applies a validated server snapshot without running destructive gameplay effects on the client. */
    public void applyPartNetworkState(int[] ordinals, float[] health, int[] fireTicks, byte[] flags)
    {
        if (!level().isClientSide || driveableData == null || ordinals == null || health == null || fireTicks == null || flags == null)
            return;
        int count = Math.min(Math.min(ordinals.length, health.length), Math.min(fireTicks.length, flags.length));
        EnumDriveablePart[] values = EnumDriveablePart.values();
        for (int index = 0; index < count; index++)
        {
            int ordinal = ordinals[index];
            if (ordinal < 0 || ordinal >= values.length || !Float.isFinite(health[index]))
                continue;
            DriveablePart part = driveableData.getPart(values[ordinal]);
            if (part == null)
                continue;
            part.applyNetworkState(health[index], Math.max(0, fireTicks[index]), (flags[index] & 1) != 0, (flags[index] & 2) != 0);
            if (part.isDestroyed())
                destroyedParts.add(part.getType());
            else
                destroyedParts.remove(part.getType());
        }
    }

    protected void tickSounds()
    {
        if (configType == null)
            return;
        boolean occupied = getControllingEntity() != null;
        boolean ready = isEngineActive() && occupied;
        boolean active = ready && engineRequested;
        if (engineSoundTimer > 0)
            --engineSoundTimer;
        if (idleSoundTimer > 0)
            --idleSoundTimer;
        if (reverseSoundTimer > 0)
            --reverseSoundTimer;

        if (engineRequested && !wasEngineRequested && StringUtils.isNotBlank(configType.getStartSound()))
        {
            PacketPlaySound.sendSoundPacket(this, Math.max(1, configType.getStartSoundRange()), configType.getStartSound(), false);
            engineSoundTimer = Math.max(engineSoundTimer, Math.max(1, configType.getStartSoundLength()));
        }
        if (active && engineSoundTimer <= 0 && StringUtils.isNotBlank(configType.getEngineSound()))
        {
            PacketPlaySound.sendSoundPacket(this, Math.max(1, configType.getEngineSoundRange()), configType.getEngineSound(), true);
            engineSoundTimer = Math.max(1, configType.getEngineSoundLength());
        }
        if (ready && !engineRequested && engineSoundTimer <= 0 && idleSoundTimer <= 0 && StringUtils.isNotBlank(configType.getIdleSound()))
        {
            PacketPlaySound.sendSoundPacket(this, Math.max(1, configType.getEngineSoundRange()), configType.getIdleSound(), false);
            idleSoundTimer = Math.max(1, configType.getIdleSoundLength());
        }
        if (ready && getThrottle() < -0.05F && reverseSoundTimer <= 0 && StringUtils.isNotBlank(configType.getBackSound()))
        {
            PacketPlaySound.sendSoundPacket(this, Math.max(1, configType.getBackSoundRange()), configType.getBackSound(), false);
            reverseSoundTimer = Math.max(1, configType.getBackSoundLength());
        }
        wasEngineActive = active;
        wasEngineRequested = engineRequested;
    }

    protected void updateRiderVisibility()
    {
        if (configType == null)
            return;
        List<Entity> currentRiders = new ArrayList<>(getPassengers());
        for (Seat seat : seats)
        {
            if (seat != null)
                currentRiders.addAll(seat.getPassengers());
        }
        if (configType.isSetPlayerInvisible())
        {
            for (Entity rider : currentRiders)
            {
                if (!rider.isInvisible())
                {
                    ridersHiddenByDriveable.put(rider.getUUID(), rider);
                    rider.setInvisible(true);
                }
            }
        }
        ridersHiddenByDriveable.entrySet().removeIf(entry -> {
            Entity rider = entry.getValue();
            if (configType.isSetPlayerInvisible() && currentRiders.contains(rider) && rider.isAlive())
                return false;
            if (!rider.isRemoved())
                rider.setInvisible(false);
            return true;
        });
    }

    protected void restoreRiderVisibility()
    {
        for (Entity rider : ridersHiddenByDriveable.values())
        {
            if (!rider.isRemoved())
                rider.setInvisible(false);
        }
        ridersHiddenByDriveable.clear();
    }

    public int getLockOnTargetId()
    {
        return entityData.get(DATA_LOCK_TARGET);
    }

    @Nullable
    public Entity getLockOnTarget()
    {
        if (lockOnTarget != null && lockOnTarget.isAlive() && lockOnTarget.getId() == getLockOnTargetId())
            return lockOnTarget;
        lockOnTarget = level().getEntity(getLockOnTargetId());
        return lockOnTarget != null && lockOnTarget.isAlive() ? lockOnTarget : null;
    }

    protected void updateLockOnTargeting()
    {
        if (configType == null || !hasLockOnCapability() || !(getControllingEntity() instanceof LivingEntity controller))
        {
            clearLockOnTarget();
            return;
        }
        if (lockOnSoundDelay > 0)
            --lockOnSoundDelay;
        if (tickCount % 5 != 0)
        {
            if (!isValidLockOnTarget(lockOnTarget, controller, false))
                clearLockOnTarget();
            return;
        }

        double range = Mth.clamp(configType.getMaxRangeLockOn(), 1, 512);
        Vec3 origin = getSeatWorldPosition(0);
        Vec3 look = getDriverAimDirection();
        double minimumDot = Math.cos(Math.toRadians(Mth.clamp(configType.getCanLockOnAngle(), 0, 180)));
        Entity best = null;
        double bestScore = -Double.MAX_VALUE;
        for (Entity candidate : level().getEntities(this, getBoundingBox().inflate(range),
            entity -> entity.isAlive() && !isPartOfThis(entity) && matchesLockOnCategory(entity)))
        {
            if (!isValidLockOnTarget(candidate, controller, true))
                continue;
            Vec3 toTarget = targetCentre(candidate).subtract(origin);
            double distanceSquared = toTarget.lengthSqr();
            if (distanceSquared < 1.0E-6D || distanceSquared > range * range)
                continue;
            double dot = look.dot(toTarget.normalize());
            if (dot < minimumDot)
                continue;
            double score = dot * 4D - Math.sqrt(distanceSquared) / range;
            if (score > bestScore)
            {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null && !hasLineOfSight(origin, targetCentre(best), controller))
            best = null;

        if (best != lockOnTarget)
            lockOnSoundDelay = 0;
        lockOnTarget = best;
        entityData.set(DATA_LOCK_TARGET, best == null ? -1 : best.getId());
        if (best != null && lockOnSoundDelay <= 0)
        {
            if (StringUtils.isNotBlank(configType.getLockOnSound()))
                PacketPlaySound.sendSoundPacket(controller, 10D, configType.getLockOnSound(), false);
            if (best instanceof Driveable target && target.getConfigType() != null
                && StringUtils.isNotBlank(target.getConfigType().getLockingOnSound()))
                PacketPlaySound.sendSoundPacket(target, Math.max(1, target.getConfigType().getLockedOnSoundRange()),
                    target.getConfigType().getLockingOnSound(), false);
            lockOnSoundDelay = Math.max(1, configType.getLockOnSoundTime());
        }
    }

    protected boolean hasLockOnCapability()
    {
        return configType != null && (configType.isLockOnToPlanes() || configType.isLockOnToVehicles()
            || configType.isLockOnToMechas() || configType.isLockOnToPlayers() || configType.isLockOnToLivings());
    }

    protected boolean matchesLockOnCategory(Entity entity)
    {
        return configType != null && (configType.isLockOnToMechas() && entity instanceof Mecha
            || configType.isLockOnToVehicles() && (entity instanceof Vehicle || ModUtils.isVehicleLike(entity))
            || configType.isLockOnToPlanes() && (entity instanceof Plane || ModUtils.isPlaneLike(entity))
            || configType.isLockOnToPlayers() && entity instanceof Player
            || configType.isLockOnToLivings() && entity instanceof LivingEntity);
    }

    protected boolean isValidLockOnTarget(@Nullable Entity target, LivingEntity controller, boolean checkRange)
    {
        if (target == null || !target.isAlive() || target.level() != level() || target == controller || !matchesLockOnCategory(target))
            return false;
        if (target instanceof Player player && player.isSpectator())
            return false;
        if (target instanceof Driveable driveable && driveable.isVarFlare())
            return false;
        double range = configType == null ? 1D : Mth.clamp(configType.getMaxRangeLockOn(), 1, 512);
        return !checkRange || target.distanceToSqr(this) <= range * range;
    }

    protected boolean hasLineOfSight(Vec3 origin, Vec3 target, Entity controller)
    {
        BlockHitResult hit = level().clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, controller));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(origin) + 1D >= target.distanceToSqr(origin);
    }

    protected Vec3 getDriverAimDirection()
    {
        Seat driver = getDriverSeat();
        SeatInfo info = driver == null ? null : driver.getSeatInfo();
        return aimedDirection(getTurretYaw(), getTurretPitch());
    }

    protected static Vec3 targetCentre(Entity target)
    {
        return target.position().add(0D, target.getBbHeight() * 0.5D, 0D);
    }

    protected void clearLockOnTarget()
    {
        lockOnTarget = null;
        if (entityData.get(DATA_LOCK_TARGET) != -1)
            entityData.set(DATA_LOCK_TARGET, -1);
    }

    protected void onPartDestroyed(EnumDriveablePart part)
    {
        if (!destroyedParts.add(part))
            return;
        dropDestroyedPartRecipe(part);
        if (configType != null)
        {
            DriveableExplosion explosion = configType.getPartDeathExplosions().get(part);
            if (explosion != null && explosion.explosionRadius() > 0F)
                createExplosion(explosion, getPartWorldCentre(part));
        }
        for (EnumDriveablePart child : part.getChildren())
        {
            DriveablePart childPart = driveableData == null ? null : driveableData.getPart(child);
            if (childPart != null && !childPart.isDestroyed())
            {
                childPart.damage(Math.max(1F, childPart.getMaxHealth()), false);
                onPartDestroyed(child);
            }
        }
        if (part == EnumDriveablePart.CORE)
            destroyDriveable();
    }

    protected void dropDestroyedPartRecipe(EnumDriveablePart partType)
    {
        if (suppressDrops || level().isClientSide || configType == null || driveableData == null)
            return;
        DriveablePart part = driveableData.getPart(partType);
        if (part == null)
            return;
        Vec3 dropPosition = getPartWorldCentre(partType);
        for (ItemStack required : configType.getItemsRequired(part, driveableData.getEngine()))
        {
            if (required.isEmpty())
                continue;
            ItemStack drop = required.copy();
            Entity item = new net.minecraft.world.entity.item.ItemEntity(level(), dropPosition.x, dropPosition.y, dropPosition.z, drop);
            item.setDeltaMovement(getDeltaMovement().scale(0.25D).add((random.nextDouble() - 0.5D) * 0.1D,
                random.nextDouble() * 0.1D, (random.nextDouble() - 0.5D) * 0.1D));
            level().addFreshEntity(item);
        }
    }

    protected void ensureProxyEntities()
    {
        if (level().isClientSide || configType == null || !isAlive())
            return;
        resizeProxyArrays();
        for (int index = 0; index < seats.length; index++)
        {
            Seat seat = seats[index];
            if (seat != null && seat.isAlive() && seat.level() == level() && seat.getParentId() == getId())
                continue;
            SeatInfo info = configType.getSeat(index);
            if (info == null)
                continue;
            seat = new Seat(level(), this, index, info);
            seats[index] = seat;
            level().addFreshEntity(seat);
        }
        for (int index = 0; index < wheels.length; index++)
        {
            Wheel wheel = wheels[index];
            if (configType.getWheelPosition(index) == null)
            {
                if (wheel != null && !wheel.isRemoved())
                    wheel.discard();
                wheels[index] = null;
                continue;
            }
            if (wheel != null && wheel.isAlive() && wheel.level() == level() && wheel.getParentId() == getId())
                continue;
            wheel = new Wheel(level(), this, index);
            wheels[index] = wheel;
            level().addFreshEntity(wheel);
        }
    }

    public void registerSeatProxy(@NotNull Seat seat)
    {
        int index = seat.getSeatIndex();
        if (index < 0)
            return;
        if (index >= seats.length)
            seats = Arrays.copyOf(seats, index + 1);
        Seat existing = seats[index];
        if (existing == null || existing.isRemoved() || existing == seat || existing.getId() > seat.getId())
            seats[index] = seat;
    }

    public void registerWheelProxy(@NotNull Wheel wheel)
    {
        int index = wheel.getWheelIndex();
        if (index < 0)
            return;
        if (index >= wheels.length)
            wheels = Arrays.copyOf(wheels, index + 1);
        Wheel existing = wheels[index];
        if (existing == null || existing.isRemoved() || existing == wheel || existing.getId() > wheel.getId())
            wheels[index] = wheel;
    }

    protected void updateProxyPositions()
    {
        for (Seat seat : seats)
        {
            if (seat != null && seat.isAlive())
            {
                Vec3 p = getSeatWorldPosition(seat.getSeatIndex());
                seat.setPos(p.x, p.y, p.z);
            }
        }
        for (Wheel wheel : wheels)
        {
            if (wheel != null && wheel.isAlive())
            {
                Vec3 p = getWheelWorldPosition(wheel.getWheelIndex());
                wheel.setPos(p.x, p.y, p.z);
            }
        }
    }

    public Vec3 getSeatWorldPosition(int index)
    {
        SeatInfo info = configType == null ? null : configType.getSeat(index);
        if (info == null)
            return position().add(0D, getBbHeight() * 0.5D, 0D);
        Vec3 localPosition = new Vec3(info.getPosition().x, info.getPosition().y, info.getPosition().z);
        Vec3 worldPosition = isTurretMountedPart(info.getPart())
            ? turretPointToWorld(localPosition, getTurretYaw(), info.getPart() == EnumDriveablePart.BARREL ? getTurretPitch() : 0F)
            : localToWorld(localPosition.x, localPosition.y, localPosition.z);

        Vec3 rotatedOffset = new Vec3(info.getRotatedOffset().x, info.getRotatedOffset().y, info.getRotatedOffset().z);
        if (rotatedOffset.lengthSqr() > 1.0E-8D)
        {
            float pitch = info.getPart() == EnumDriveablePart.BARREL ? getTurretPitch() : 0F;
            rotatedOffset = rotateTurretLocalDirection(rotatedOffset, getTurretYaw(), pitch);
            worldPosition = worldPosition.add(localDirectionToWorld(rotatedOffset));
        }
        return worldPosition;
    }

    public Vec3 getWheelWorldPosition(int index)
    {
        DriveablePosition wheel = configType == null ? null : configType.getWheelPosition(index);
        if (wheel == null)
            return position();
        return localToWorld(wheel.getPosition().x, wheel.getPosition().y, wheel.getPosition().z);
    }

    public Vec3 getSafeDismountPosition(@NotNull LivingEntity passenger, int seatIndex)
    {
        Vec3 right = getRightVector();
        for (double side : new double[] { 1.5D, -1.5D, 2.5D, -2.5D })
        {
            Vec3 candidate = position().add(right.scale(side)).add(0D, 0.25D, 0D);
            AABB moved = passenger.getBoundingBox().move(candidate.subtract(passenger.position()));
            if (level().noCollision(passenger, moved))
                return candidate;
        }
        return position().add(0D, getBbHeight() + 0.5D, 0D);
    }

    @Nullable
    public Seat getSeat(int index)
    {
        return index >= 0 && index < seats.length ? seats[index] : null;
    }

    @Nullable
    public Wheel getWheel(int index)
    {
        return index >= 0 && index < wheels.length ? wheels[index] : null;
    }

    @Override
    @Nullable
    public Seat getSeat(LivingEntity living)
    {
        if (living.getVehicle() instanceof Seat seat && seat.getDriveable() == this)
            return seat;
        for (Seat seat : seats)
        {
            if (seat != null && seat.hasPassenger(living))
                return seat;
        }
        return null;
    }

    public boolean isControlledBy(Player player)
    {
        Seat seat = getSeat(player);
        return seat != null && seat.isDriverSeat() && seat.getRiddenByEntity() == player;
    }

    public void acceptInput(@NotNull ServerPlayer player, int mask, float aimYaw, float aimPitch,
                            float flightPitch, float flightRoll, boolean mouseControl, int sequence)
    {
        if (!isAlive() || player.level() != level() || player.distanceToSqr(this) > 4096D)
            return;
        Seat seat = getSeat(player);
        if (seat == null || seat.getDriveable() != this || !seat.acceptInput(player, mask, aimYaw, aimPitch, sequence))
            return;

        inputTimeout = 0;
        markUsed();
        if (!seat.isDriverSeat())
            return;

        int sanitized = DriveableInput.sanitize(mask);
        previousInputMask = getInputMask();
        setInputMask(sanitized);
        setTurretAim(seat.getAimYaw(), seat.getAimPitch());
        setFlightControls(flightPitch, flightRoll, mouseControl);
        int rising = sanitized & ~previousInputMask;
        handleRisingInputs(seat, player, rising);
    }

    protected void handleRisingInputs(@NotNull Seat seat, @NotNull Player player, int rising)
    {
        if (DriveableInput.isDown(rising, DriveableInput.EXIT))
        {
            if (configType != null && StringUtils.isNotBlank(configType.getExitSound()))
                PacketPlaySound.sendSoundPacket(this, Math.max(1, configType.getEngineSoundRange()), configType.getExitSound(), false);
            player.stopRiding();
        }
        if (DriveableInput.isDown(rising, DriveableInput.MENU) && player instanceof ServerPlayer serverPlayer)
            openDriveableMenu(serverPlayer);
        if (DriveableInput.isDown(rising, DriveableInput.TOGGLE_GEAR))
            setGearDeployed(!isGearDeployed());
        if (DriveableInput.isDown(rising, DriveableInput.TOGGLE_DOOR))
            setDoorOpen(!isDoorOpen());
        if (DriveableInput.isDown(rising, DriveableInput.TOGGLE_MODE))
            toggleDriveableMode();
        if (DriveableInput.isDown(rising, DriveableInput.TRIM))
            setOrientation(getYaw(), 0F, 0F);
        if (DriveableInput.isDown(rising, DriveableInput.FLARE))
            deployFlare();
    }

    protected void toggleDriveableMode()
    {
        setDriveableMode(Math.floorMod(getDriveableMode() + 1, 2));
    }

    protected void deployFlare()
    {
        if (configType == null || !configType.isHasFlare() || flareDelay > 0 || ticksFlareUsing > 0)
            return;
        // TimeFlareUsing was specified in seconds by legacy content packs,
        // while FlareDelay is already expressed in ticks.
        ticksFlareUsing = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) configType.getTimeFlareUsing() * 20L));
        flareDelay = Math.max(ticksFlareUsing, configType.getFlareDelay());
        setFlag(FLAG_FLARE, true);
        if (StringUtils.isNotBlank(configType.getFlareSound()))
            PacketPlaySound.sendSoundPacket(this, 96D, configType.getFlareSound(), false);
    }

    protected void updateFlares()
    {
        if (ticksFlareUsing <= 0)
        {
            setFlag(FLAG_FLARE, false);
            return;
        }
        --ticksFlareUsing;
        setFlag(FLAG_FLARE, true);
        if (tickCount % 2 == 0)
        {
            Vec3 behind = position().subtract(getForwardVector().scale(Math.max(1D, getBbWidth())));
            PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.FM_FLARE, behind.x, behind.y, behind.z, 0D, -0.02D, 0D),
                behind, 128D, level().dimension());
        }
    }

    public boolean handleLegacyKey(@NotNull Seat seat, int key, @NotNull Player player)
    {
        if (seat.getRiddenByEntity() != player)
            return false;
        int input = DriveableInput.forLegacyKey(key);
        if (input == 0)
            return key == 10;
        if ((DriveableInput.EDGE_TRIGGERED_MASK & input) != 0 && seat.isDriverSeat())
            handleRisingInputs(seat, player, input);
        return true;
    }

    @Override
    public boolean pressKey(int key, Player player, boolean isOnEvent)
    {
        Seat seat = getSeat(player);
        return seat != null && seat.pressKey(key, player, isOnEvent);
    }

    @Override
    public boolean serverHandleKeyPress(int key, Player player)
    {
        Seat seat = getSeat(player);
        return seat != null && handleLegacyKey(seat, key, player);
    }

    @Override
    public void updateKeyHeldState(int key, boolean held)
    {
        int input = DriveableInput.forLegacyKey(key);
        if (input == 0)
            return;
        if (held)
            localInputMask |= input;
        else
            localInputMask &= ~input;
        setInputMask(localInputMask);
    }

    @Override
    public void onMouseMoved(double deltaX, double deltaY)
    {
        Seat driver = getDriverSeat();
        if (driver != null)
            driver.onMouseMoved(deltaX, deltaY);
    }

    @Nullable
    public Seat getDriverSeat()
    {
        for (Seat seat : seats)
        {
            if (seat != null && seat.isDriverSeat())
                return seat;
        }
        return seats.length == 0 ? null : seats[0];
    }

    @Override
    @Nullable
    public Entity getControllingEntity()
    {
        Seat driver = getDriverSeat();
        return driver == null ? null : driver.getRiddenByEntity();
    }

    @Override
    public boolean isDead()
    {
        return isRemoved() || destroyed;
    }

    @Override
    public float getPlayerRoll()
    {
        return getRoll();
    }

    @Override
    public float getPrevPlayerRoll()
    {
        return prevRoll;
    }

    @Override
    public float getCameraDistance()
    {
        return configType == null ? 4F : configType.getCameraDistance();
    }

    @Override
    @Nullable
    public LivingEntity getCamera()
    {
        return getControllingEntity() instanceof LivingEntity living ? living : null;
    }

    /** Used to stop self collision and friendly projectile hits. */
    public boolean isPartOfThis(@Nullable Entity entity)
    {
        if (entity == null)
            return false;
        if (entity == this)
            return true;
        if (hasPassenger(entity))
            return true;
        for (Seat seat : seats)
        {
            if (seat == entity || seat != null && seat.hasPassenger(entity))
                return true;
        }
        for (Wheel wheel : wheels)
        {
            if (wheel == entity)
                return true;
        }
        return false;
    }

    public boolean isPartIntact(@Nullable EnumDriveablePart part)
    {
        if (part == null || driveableData == null)
            return false;
        DriveablePart state = driveableData.getPart(part);
        return state != null && !state.isDestroyed();
    }

    /** Called by the shooting pipeline after a precise part ray hit. */
    public ShootingHelper.HitData bulletHit(BulletType bulletType, DriveableHit hit, ShootingHelper.HitData hitData)
    {
        if (bulletType == null || hit == null || driveableData == null)
            return hitData;
        DriveablePart part = driveableData.getPart(hit.getPart());
        if (part == null)
            return hitData;

        float previousPower = Math.max(0F, hitData.penetratingPower());
        float resistance = part.getPenetrationResistance();
        float remainingPower = Math.max(0F, previousPower - resistance);
        float penetrationRatio = previousPower <= 0F ? 0F : remainingPower / previousPower;
        float damage = bulletType.getDamage().getDamageAgainstEntity(this) * Mth.clamp(previousPower, 0.1F, 1F);
        if (!level().isClientSide)
        {
            part.damage(Math.max(0F, damage), bulletType.isSetEntitiesOnFire());
            if (part.isDestroyed())
                onPartDestroyed(part.getType());
        }
        return new ShootingHelper.HitData(remainingPower, penetrationRatio, false);
    }

    /** Precise ray trace against every configured local part box. */
    public List<BulletHit> attackFromBullet(Vec3 origin, Vec3 motion)
    {
        if (driveableData == null || motion.lengthSqr() < 1.0E-12D)
            return Collections.emptyList();
        Vec3 localOrigin = worldToLocal(origin);
        Vec3 localMotion = worldDirectionToLocal(motion);
        Vec3 localEnd = localOrigin.add(localMotion);
        double lengthSquared = localMotion.lengthSqr();
        List<BulletHit> hits = new ArrayList<>();
        for (DriveablePart part : driveableData.getParts().values())
        {
            CollisionBox box = part.getBox();
            if (box == null)
                continue;
            AABB aabb = box.asAabb();
            Optional<Vec3> intersection = aabb.contains(localOrigin) ? Optional.of(localOrigin) : aabb.clip(localOrigin, localEnd);
            if (intersection.isEmpty())
                continue;
            float fraction = (float) Mth.clamp(intersection.get().subtract(localOrigin).dot(localMotion) / lengthSquared, 0D, 1D);
            hits.add(new DriveableHit(this, part.getType(), fraction));
        }
        hits.sort(Comparator.naturalOrder());
        return hits;
    }

    public boolean damagePart(@Nullable EnumDriveablePart partType, float amount, @Nullable DamageSource source)
    {
        if (level().isClientSide || destroyed || driveableData == null || amount <= 0F)
            return false;
        EnumDriveablePart target = partType == null ? EnumDriveablePart.CORE : partType;
        DriveablePart part = driveableData.getPart(target);
        if (part == null)
            return false;
        if (source != null)
            lastAtkEntity = source.getEntity();
        boolean fire = source != null && source.is(DamageTypeTags.IS_FIRE);
        boolean newlyDestroyed = !part.isDestroyed() && part.damage(amount, fire);
        if (newlyDestroyed)
            onPartDestroyed(target);
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (isInvulnerableTo(source) || destroyed)
            return false;
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && getControllingEntity() == null && onGround()
            && canPlayerAccess(player)
            && (player.getAbilities().instabuild || FlansMod.teamsManager.isSurvivalCanBreakVehicles()))
        {
            if (!level().isClientSide)
                pickupAsItem(player);
            return true;
        }
        return damagePart(EnumDriveablePart.CORE, amount, source);
    }

    public Optional<EnumDriveablePart> findNearestPart(@NotNull Vec3 worldPoint)
    {
        if (driveableData == null)
            return Optional.empty();
        Vec3 local = worldToLocal(worldPoint);
        return driveableData.getParts().values().stream()
            .filter(part -> part.getBox() != null)
            .min(Comparator.comparingDouble(part -> distanceSquaredToBox(local, part.getBox().asAabb())))
            .map(DriveablePart::getType);
    }

    public boolean repairFromTool(@NotNull Player player, int amount)
    {
        if (level().isClientSide || amount <= 0 || driveableData == null || !canPlayerAccess(player))
            return false;
        Vec3 origin = player.getEyePosition();
        Vec3 motion = player.getLookAngle().scale(6D);
        EnumDriveablePart selected = attackFromBullet(origin, motion).stream()
            .filter(DriveableHit.class::isInstance)
            .map(DriveableHit.class::cast)
            .map(DriveableHit::getPart)
            .filter(this::canRepairPart)
            .findFirst()
            .orElseGet(() -> findNearestDamagedPart(origin).orElse(null));
        if (selected == null)
            return false;
        return repairPart(selected, amount);
    }

    private Optional<EnumDriveablePart> findNearestDamagedPart(Vec3 origin)
    {
        if (driveableData == null)
            return Optional.empty();
        return driveableData.getParts().values().stream()
            .filter(part -> canRepairPart(part.getType()))
            .min(Comparator.comparingDouble(part -> {
                CollisionBox box = part.getBox();
                Vec3 centre = box == null ? position() : localToWorld(box.getCentre().x, box.getCentre().y, box.getCentre().z);
                return centre.distanceToSqr(origin);
            })).map(DriveablePart::getType);
    }

    public boolean canRepairPart(@Nullable EnumDriveablePart part)
    {
        if (part == null || driveableData == null || destroyed)
            return false;
        DriveablePart state = driveableData.getPart(part);
        if (state == null || state.getMaxHealth() <= 0F || state.getHealth() >= state.getMaxHealth())
            return false;
        for (EnumDriveablePart parent : part.getParents())
        {
            if (!isPartIntact(parent))
                return false;
        }
        return true;
    }

    /** Repairs one part after validating its dependency chain. Inventory costs are owned by the caller. */
    public boolean repairPart(@Nullable EnumDriveablePart part, float amount)
    {
        if (level().isClientSide || amount <= 0F || !Float.isFinite(amount) || !canRepairPart(part))
            return false;
        DriveablePart state = driveableData.getPart(part);
        if (state == null || state.repair(amount) <= 0F)
            return false;
        state.extinguish();
        destroyedParts.remove(part);
        return true;
    }

    private static double distanceSquaredToBox(Vec3 point, AABB box)
    {
        double dx = Math.max(box.minX - point.x, Math.max(0D, point.x - box.maxX));
        double dy = Math.max(box.minY - point.y, Math.max(0D, point.y - box.maxY));
        double dz = Math.max(box.minZ - point.z, Math.max(0D, point.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    protected boolean hasFuelForEngine()
    {
        if (configType != null && !configType.isWorksUnderWater() && isUnderWater())
            return false;
        if (configType == null || configType.getFuelTankSize() < 0F || !FlansMod.teamsManager.isVehiclesNeedFuel())
            return true;
        Entity controller = getControllingEntity();
        return controller instanceof Player player && player.getAbilities().instabuild || getFuel() > 0F;
    }

    protected float getEngineSpeed()
    {
        PartType engine = driveableData == null ? null : driveableData.getEngine();
        return engine == null ? 1F : Math.max(0.05F, engine.getEngineSpeed());
    }

    protected float getEnginePower()
    {
        PartType engine = driveableData == null ? null : driveableData.getEngine();
        return engine == null ? 10F : Math.max(0F, engine.getEnginePower());
    }

    protected void consumeFuel(float load)
    {
        if (level().isClientSide || driveableData == null || configType == null || configType.getFuelTankSize() < 0F
            || !FlansMod.teamsManager.isVehiclesNeedFuel() || getControllingEntity() instanceof Player player && player.getAbilities().instabuild)
            return;
        PartType engine = driveableData.getEngine();
        float consumption = engine == null ? 1F : Math.max(0F, engine.getFuelConsumption());
        setFuel(getFuel() - consumption * Math.max(0F, load) / 20F);
    }

    protected void refuelFromInventory()
    {
        if (driveableData == null || configType == null || !FlansMod.teamsManager.isVehiclesNeedFuel()
            || configType.getFuelTankSize() <= 0F || getFuel() >= configType.getFuelTankSize())
            return;
        PartType engine = driveableData.getEngine();
        if (engine != null && engine.isUseRFPower())
        {
            refuelFromEnergyItems(engine);
            return;
        }
        ItemStack stack = driveableData.getFuelStack();
        if (!(stack.getItem() instanceof PartItem partItem) || partItem.getConfigType().getCategory() != PartType.Category.FUEL)
            return;
        int capacity = Math.max(0, partItem.getConfigType().getFuel());
        if (capacity <= 0)
            return;
        int stored = Math.max(0, capacity - stack.getDamageValue());
        if (stored <= 0)
        {
            stack.shrink(1);
            driveableData.setFuelStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
            return;
        }
        int transfer = Math.min(stored, Math.max(1, Mth.ceil(configType.getFuelTankSize() - getFuel())));
        setFuel(getFuel() + transfer);
        stack.setDamageValue(stack.getDamageValue() + transfer);
        if (stack.getDamageValue() >= capacity)
            stack.shrink(1);
        driveableData.setFuelStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
    }

    private void refuelFromEnergyItems(@NotNull PartType engine)
    {
        int drawRate = Math.max(1, engine.getRfDrawRate());
        float tankCapacity = Math.max(0F, configType.getFuelTankSize());
        for (int slot = 0; slot < driveableData.getContainerSize() && getFuel() < tankCapacity; slot++)
        {
            ItemStack stack = driveableData.getItem(slot);
            if (stack.isEmpty())
                continue;
            IEnergyStorage energy = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
            if (energy == null || !energy.canExtract())
                continue;

            double room = tankCapacity - getFuel();
            int roomLimitedDraw = (int) Math.min(drawRate, Math.ceil(room * drawRate / 2D));
            if (roomLimitedDraw <= 0)
                break;
            int extracted = Math.max(0, energy.extractEnergy(roomLimitedDraw, false));
            if (extracted <= 0)
                continue;
            setFuel((float) Math.min(tankCapacity, getFuel() + 2D * extracted / drawRate));
            driveableData.setChanged();
        }
    }

    protected void emitConfiguredParticles()
    {
        if (configType == null)
            return;
        configType.getEmitters().forEach(emitter -> {
            if (tickCount % emitter.getEmitRate() != 0 || !isPartIntact(emitter.getPart()))
                return;
            float throttle = getThrottle();
            if (throttle < emitter.getMinThrottle() || throttle > emitter.getMaxThrottle())
                return;

            DriveablePart partState = driveableData == null ? null : driveableData.getPart(emitter.getPart());
            if (partState == null || partState.getMaxHealth() <= 0F)
                return;
            float health = partState.getHealth() / partState.getMaxHealth();
            if (health < emitter.getMinHealth() || health > emitter.getMaxHealth())
                return;

            Vec3 localOrigin = new Vec3(
                emitter.getOrigin().x + (random.nextFloat() - 0.5F) * emitter.getExtents().x,
                emitter.getOrigin().y + (random.nextFloat() - 0.5F) * emitter.getExtents().y,
                emitter.getOrigin().z + (random.nextFloat() - 0.5F) * emitter.getExtents().z);
            Vec3 localVelocity = new Vec3(emitter.getVelocityVector().x,
                emitter.getVelocityVector().y, emitter.getVelocityVector().z);
            localOrigin = rotateLegacyEmitterVector(localOrigin);
            localVelocity = rotateLegacyEmitterVector(localVelocity);

            Vec3 origin;
            Vec3 direction;
            if (isTurretMountedPart(emitter.getPart()))
            {
                float pitch = emitter.getPart() == EnumDriveablePart.BARREL ? getTurretPitch() : 0F;
                origin = turretPointToWorld(localOrigin, getTurretYaw(), pitch);
                direction = localDirectionToWorld(rotateTurretLocalDirection(localVelocity, getTurretYaw(), pitch));
            }
            else
            {
                origin = localToWorld(localOrigin.x, localOrigin.y, localOrigin.z);
                direction = localDirectionToWorld(localVelocity);
            }
            PacketHandler.sendToAllAround(new PacketParticle(emitter.getParticleType(), origin.x, origin.y, origin.z,
                direction.x, direction.y, direction.z), origin, 128D, level().dimension());
        });
    }

    /** Convert legacy model X/Z emitter coordinates to the modern driveable basis. */
    protected static Vec3 rotateLegacyEmitterVector(@NotNull Vec3 vector)
    {
        return new Vec3(vector.z, vector.y, -vector.x);
    }

    public boolean bindOrCheckKey(@NotNull Player player, @NotNull ItemStack keyStack)
    {
        if (!(keyStack.getItem() instanceof ToolItem tool) || !tool.getConfigType().isKey())
            return false;
        String expected = getUUID().toString();
        String key = keyStack.getOrCreateTag().getString(NBT_KEY_ID);
        if (StringUtils.isBlank(key))
        {
            if (locked && ownerId != null && !ownerId.equals(player.getUUID()) && !player.getAbilities().instabuild)
                return false;
            keyStack.getOrCreateTag().putString(NBT_KEY_ID, expected);
            locked = true;
            if (ownerId == null)
                ownerId = player.getUUID();
            return true;
        }
        return expected.equals(key);
    }

    public boolean canPlayerAccess(@NotNull Player player)
    {
        if (!locked || player.getAbilities().instabuild || ownerId != null && ownerId.equals(player.getUUID()))
            return true;
        return bindOrCheckKey(player, player.getMainHandItem()) || bindOrCheckKey(player, player.getOffhandItem());
    }

    public boolean canPlayerAccessInventory(@NotNull Player player)
    {
        if (!player.isAlive() || player.distanceToSqr(this) > 64D || !canPlayerAccess(player))
            return false;
        if (configType instanceof PlaneType plane && !plane.isInvInflight()
            && (!onGround() || Math.abs(getThrottle()) >= 0.1F))
            return false;
        return true;
    }

    public Container getDriveableInventory()
    {
        return driveableData;
    }

    public boolean openDriveableMenu(@NotNull ServerPlayer player)
    {
        if (!canPlayerAccessInventory(player) || driveableData == null || configType == null)
            return false;
        NetworkHooks.openScreen(player,
            new SimpleMenuProvider((containerId, inventory, ignored) -> new DriveableInventoryMenu(containerId, inventory, this),
                Component.literal(configType.getName())),
            buffer -> buffer.writeVarInt(getId()));
        return true;
    }

    @Override
    @NotNull
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ToolItem tool && tool.getConfigType().isKey())
            return InteractionResult.sidedSuccess(level().isClientSide || bindOrCheckKey(player, held));
        if (!canPlayerAccess(player) || player.isSpectator())
            return InteractionResult.PASS;
        if (level().isClientSide)
            return InteractionResult.SUCCESS;

        Seat target = null;
        for (Seat seat : seats)
        {
            if (seat != null && seat.getFirstPassenger() == null && isPartIntact(seat.getSeatInfo() == null ? EnumDriveablePart.CORE : seat.getSeatInfo().getPart()))
            {
                target = seat;
                if (seat.isDriverSeat())
                    break;
            }
        }
        if (target == null)
            return InteractionResult.PASS;
        if (player.getVehicle() != null)
            player.stopRiding();
        return player.startRiding(target, true) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public boolean isPickable()
    {
        return isAlive();
    }

    @Override
    public ItemStack getPickedResult(HitResult target)
    {
        return createDropStack();
    }

    public ItemStack createDropStack()
    {
        if (configType == null || driveableData == null)
            return ItemStack.EMPTY;
        ItemStack result = sourceStack.isEmpty() ? ModUtils.getItemStack(configType).orElse(ItemStack.EMPTY) : sourceStack.copy();
        if (result.isEmpty())
            return ItemStack.EMPTY;
        result.setCount(1);
        return driveableData.copyToStack(result);
    }

    protected void pickupAsItem(Player player)
    {
        ItemStack stack = createDropStack();
        if (!stack.isEmpty())
        {
            if (!player.getInventory().add(stack))
                spawnAtLocation(stack, 0.25F);
        }
        suppressDrops = true;
        discard();
    }

    protected void destroyDriveable()
    {
        if (destroyed)
            return;
        destroyed = true;
        restoreRiderVisibility();
        if (configType != null && configType.isExplosionWhenDestroyed() && configType.getDeathExplosionRadius() > 0F)
        {
            createExplosion(new DriveableExplosion(configType.getDeathFireRadius(), configType.getDeathExplosionRadius(),
                configType.isDeathExplosionBreaksBlocks(), configType.getDeathExplosionDamageVsLiving(),
                configType.getDeathExplosionDamageVsPlayer(), configType.getDeathExplosionDamageVsPlane(),
                configType.getDeathExplosionDamageVsVehicle()), position());
        }
        for (Seat seat : seats)
        {
            if (seat == null)
                continue;
            for (Entity passenger : List.copyOf(seat.getPassengers()))
            {
                passenger.stopRiding();
                passenger.hurt(level().damageSources().generic(), Float.MAX_VALUE);
            }
        }
        for (Entity passenger : List.copyOf(getPassengers()))
        {
            passenger.stopRiding();
            passenger.hurt(level().damageSources().generic(), Float.MAX_VALUE);
        }
        if (!suppressDrops)
            dropContents();
        if (lastAtkEntity instanceof ServerPlayer attacker)
        {
            try
            {
                FlansMod.teamsManager.getStats(attacker).recordVehicleDestroyed();
            }
            catch (RuntimeException ignored)
            {
                // Teams data is optional outside a running server/round.
            }
        }
        discard();
    }

    protected Vec3 getPartWorldCentre(EnumDriveablePart partType)
    {
        DriveablePart part = driveableData == null ? null : driveableData.getPart(partType);
        CollisionBox box = part == null ? null : part.getBox();
        return box == null ? position() : localToWorld(box.getCentre().x, box.getCentre().y, box.getCentre().z);
    }

    protected void createExplosion(DriveableExplosion settings, Vec3 centre)
    {
        if (level().isClientSide || settings.explosionRadius() <= 0F)
            return;
        DamageStats blast = new DamageStats();
        blast.setDamage(settings.damageVsVehicle());
        blast.setDamageVsLiving(settings.damageVsLiving());
        blast.setDamageVsPlayer(settings.damageVsPlayer());
        blast.setDamageVsVehicles(settings.damageVsVehicle());
        blast.setDamageVsPlanes(settings.damageVsPlane());
        blast.setReadDamage(true);
        blast.setReadDamageVsLiving(true);
        blast.setReadDamageVsPlayer(true);
        blast.setReadDamageVsVehicles(true);
        blast.setReadDamageVsPlanes(true);
        blast.calculate();
        DamageStats fragments = new DamageStats();
        fragments.setDamage(0F);
        fragments.calculate();
        float power = configType == null ? 1F : Math.max(0F, configType.getDeathExplosionPower());
        FlanExplosion.Stats stats = new FlanExplosion.Stats(settings.explosionRadius(), power,
            settings.explosionRadius() * 1.5F, blast, settings.explosionRadius(), 0F, fragments);
        new FlanExplosion(level(), this, lastAtkEntity instanceof LivingEntity living ? living : null,
            centre.x, centre.y, centre.z, stats, settings.fireRadius() > 0F,
            settings.breaksBlocks() && FlansMod.teamsManager.isDriveablesBreakBlocks(), 8, 4, false);
    }

    protected void dropContents()
    {
        if (driveableData == null)
            return;
        for (ItemStack stack : driveableData.getInventory())
        {
            if (!stack.isEmpty())
                spawnAtLocation(stack.copy(), 0.5F);
        }
        driveableData.clearContent();
    }

    @Override
    public void remove(@NotNull RemovalReason reason)
    {
        restoreRiderVisibility();
        for (Seat seat : seats)
        {
            if (seat != null && !seat.isRemoved())
                seat.discard();
        }
        for (Wheel wheel : wheels)
        {
            if (wheel != null && !wheel.isRemoved())
                wheel.discard();
        }
        super.remove(reason);
    }

    public Vec3 getForwardVector()
    {
        return ModUtils.getDirectionFromPitchAndYaw(getPitch(), getYaw()).normalize();
    }

    public Vec3 getRightVector()
    {
        Vec3 horizontalRight = ModUtils.getDirectionFromPitchAndYaw(0F, getYaw() - 90F).normalize();
        Vec3 up = getForwardVector().cross(horizontalRight).normalize();
        double roll = getRoll() * Mth.DEG_TO_RAD;
        return horizontalRight.scale(Math.cos(roll)).add(up.scale(Math.sin(roll))).normalize();
    }

    public Vec3 getUpVector()
    {
        Vec3 forward = getForwardVector();
        Vec3 horizontalRight = ModUtils.getDirectionFromPitchAndYaw(0F, getYaw() - 90F).normalize();
        Vec3 up = forward.cross(horizontalRight).normalize();
        double roll = getRoll() * Mth.DEG_TO_RAD;
        return up.scale(Math.cos(roll)).subtract(horizontalRight.scale(Math.sin(roll))).normalize();
    }

    public Vec3 localToWorld(double x, double y, double z)
    {
        return position().add(localDirectionToWorld(new Vec3(x, y, z)));
    }

    public Vec3 localDirectionToWorld(@NotNull Vec3 local)
    {
        return getForwardVector().scale(local.x).add(getUpVector().scale(local.y)).add(getRightVector().scale(local.z));
    }

    public Vec3 worldToLocal(@NotNull Vec3 world)
    {
        return worldDirectionToLocal(world.subtract(position()));
    }

    public Vec3 worldDirectionToLocal(@NotNull Vec3 worldDirection)
    {
        return new Vec3(worldDirection.dot(getForwardVector()), worldDirection.dot(getUpVector()), worldDirection.dot(getRightVector()));
    }

    protected void moveWithCollisions(Vec3 velocity)
    {
        if (!Double.isFinite(velocity.x) || !Double.isFinite(velocity.y) || !Double.isFinite(velocity.z))
            velocity = Vec3.ZERO;
        double maximum = 8D;
        velocity = new Vec3(Mth.clamp(velocity.x, -maximum, maximum), Mth.clamp(velocity.y, -maximum, maximum), Mth.clamp(velocity.z, -maximum, maximum));
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        handleCollisionConsequences(velocity);
        if (horizontalCollision)
            setDeltaMovement(getDeltaMovement().multiply(0.2D, 1D, 0.2D));
        if (verticalCollision)
            setDeltaMovement(getDeltaMovement().multiply(1D, 0.2D, 1D));
    }

    protected Vec3 applyGravityAndBuoyancy(@NotNull Vec3 velocity, double gravity)
    {
        if (configType != null && configType.isFloatOnWater() && isInWater())
        {
            double lift = Mth.clamp(configType.getBuoyancy(), 0F, 0.25F);
            return velocity.add(0D, lift, 0D).multiply(0.92D, 0.8D, 0.92D);
        }
        return velocity.add(0D, -Math.max(0D, gravity), 0D);
    }

    /**
     * Samples configured wheel contact points and applies bounded spring and
     * step-up corrections before the root entity moves. This keeps wheels as
     * cheap visual/damage proxies while retaining terrain-aware suspension.
     */
    protected Vec3 applyWheelContactPhysics(@NotNull Vec3 velocity, boolean alignToTerrain)
    {
        if (configType == null || configType.getWheelPositions().isEmpty())
            return velocity;
        float spring = Mth.clamp(configType.getWheelSpringStrength(), 0F, 1F);
        float step = Mth.clamp(configType.getWheelStepHeight(), 0F, 2.5F);
        double verticalCorrection = 0D;
        double stepCorrection = 0D;
        int contacts = 0;
        double frontHeight = 0D, backHeight = 0D, leftHeight = 0D, rightHeight = 0D;
        int frontCount = 0, backCount = 0, leftCount = 0, rightCount = 0;
        double frontX = 0D, backX = 0D, leftZ = 0D, rightZ = 0D;

        for (int index = 0; index < configType.getWheelPositions().size(); index++)
        {
            DriveablePosition definition = configType.getWheelPosition(index);
            if (definition == null || !isPartIntact(definition.getPart()))
                continue;
            Vec3 wheel = getWheelWorldPosition(index).add(velocity.x, 0D, velocity.z);
            if (step > 0F)
            {
                BlockPos obstacle = BlockPos.containing(wheel.add(0D, 0.1D, 0D));
                if (level().getBlockState(obstacle).blocksMotion())
                {
                    int maximumStep = Math.max(1, Mth.ceil(step));
                    for (int rise = 1; rise <= maximumStep; rise++)
                    {
                        if (!level().getBlockState(obstacle.above(rise)).blocksMotion())
                        {
                            stepCorrection = Math.max(stepCorrection, Math.min(step, rise + 0.05D));
                            break;
                        }
                    }
                }
            }

            Vec3 rayStart = wheel.add(0D, Math.max(0.5D, step + 0.25D), 0D);
            Vec3 rayEnd = wheel.add(0D, -1.5D, 0D);
            BlockHitResult hit = level().clip(new ClipContext(rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.BLOCK)
                continue;
            double surface = hit.getLocation().y;
            double desiredWheelY = surface + 0.35D;
            double error = desiredWheelY - wheel.y;
            if (error < -1.25D || error > step + 1D)
                continue;
            verticalCorrection += Mth.clamp(error * spring, -0.2D, Math.max(0.2D, step));
            ++contacts;

            com.flansmod.common.vector.Vector3f local = definition.getPosition();
            if (local.x >= 0F) { frontHeight += surface; frontX += local.x; ++frontCount; }
            else { backHeight += surface; backX += local.x; ++backCount; }
            if (local.z >= 0F) { rightHeight += surface; rightZ += local.z; ++rightCount; }
            else { leftHeight += surface; leftZ += local.z; ++leftCount; }
        }
        if (contacts == 0)
            return velocity.add(0D, stepCorrection, 0D);

        double correctedY = velocity.y + verticalCorrection / contacts;
        if (correctedY < 0D)
            correctedY *= Mth.clamp(1D - spring * 0.8D, 0.05D, 1D);
        correctedY += stepCorrection;

        if (alignToTerrain && frontCount > 0 && backCount > 0)
        {
            double front = frontHeight / frontCount;
            double back = backHeight / backCount;
            double length = Math.max(0.5D, frontX / frontCount - backX / backCount);
            float targetPitch = (float) -Math.toDegrees(Math.atan2(front - back, length));
            float pitch = Mth.lerp(Math.max(0.05F, spring * 0.25F), getPitch(), Mth.clamp(targetPitch, -30F, 30F));
            float roll = getRoll();
            if (configType.isCanRoll() && leftCount > 0 && rightCount > 0)
            {
                double left = leftHeight / leftCount;
                double right = rightHeight / rightCount;
                double width = Math.max(0.5D, rightZ / rightCount - leftZ / leftCount);
                float targetRoll = (float) Math.toDegrees(Math.atan2(right - left, width));
                roll = Mth.lerp(Math.max(0.05F, spring * 0.25F), getRoll(), Mth.clamp(targetRoll, -30F, 30F));
            }
            setOrientation(getYaw(), pitch, roll);
        }
        return new Vec3(velocity.x, correctedY, velocity.z);
    }

    protected boolean isNearGround(int distance)
    {
        BlockPos.MutableBlockPos cursor = blockPosition().mutable();
        int depth = Math.max(1, Math.min(16, distance));
        for (int offset = 0; offset <= depth; offset++)
        {
            cursor.set(getBlockX(), Mth.floor(getBoundingBox().minY) - offset, getBlockZ());
            if (level().getBlockState(cursor).blocksMotion())
                return true;
        }
        return false;
    }

    protected boolean shouldSquashEntities()
    {
        return false;
    }

    protected void handleCollisionConsequences(@NotNull Vec3 requestedVelocity)
    {
        if (configType == null || requestedVelocity.lengthSqr() < 0.0025D)
            return;
        double horizontalSpeed = requestedVelocity.horizontalDistance();
        if (horizontalCollision && configType.isCollisionDamageEnable()
            && Math.abs(getThrottle()) >= Math.max(0F, configType.getCollisionDamageThrottle()))
        {
            float amount = (float) Math.min(100D, horizontalSpeed * Math.max(0F, configType.getCollisionDamageTimes()) * 10D);
            if (amount > 0F)
                damagePart(EnumDriveablePart.CORE, amount, level().damageSources().flyIntoWall());
        }

        boolean squash = shouldSquashEntities();
        AABB impactBox = getBoundingBox().inflate(Math.min(1.5D, horizontalSpeed + 0.25D), 0.25D, Math.min(1.5D, horizontalSpeed + 0.25D));
        for (Entity entity : level().getEntities(this, impactBox, candidate -> candidate.isAlive() && !isPartOfThis(candidate)))
        {
            if (squash && entity instanceof LivingEntity && horizontalSpeed > 0.12D)
                entity.hurt(level().damageSources().flyIntoWall(), (float) Math.min(40D, 2D + horizontalSpeed * 12D));
            Vec3 push = entity.position().subtract(position());
            if (push.horizontalDistanceSqr() < 1.0E-6D)
                push = getForwardVector();
            entity.push(push.x * 0.2D, Math.min(0.25D, horizontalSpeed * 0.1D), push.z * 0.2D);
        }

        if (horizontalCollision && !configType.getCollisionPoints().isEmpty())
        {
            for (DriveablePosition point : configType.getCollisionPoints())
            {
                if (point == null)
                    continue;
                Vec3 world = localToWorld(point.getPosition().x, point.getPosition().y, point.getPosition().z)
                    .add(requestedVelocity.normalize().scale(0.2D));
                BlockPos blockPos = BlockPos.containing(world);
                if (level().getBlockState(blockPos).blocksMotion())
                    damagePart(point.getPart(), (float) Math.min(20D, horizontalSpeed * 5D), level().damageSources().flyIntoWall());
            }
        }
    }

    /** Executes a bounded, permission-checked legacy harvester pass. */
    protected void harvestConfiguredBlocks()
    {
        if (level().isClientSide || tickCount % 3 != 0 || configType == null || driveableData == null
            || !configType.isHarvestBlocks() || !isPartIntact(EnumDriveablePart.HARVESTER)
            || !FlansMod.teamsManager.isDriveablesBreakBlocks())
            return;
        if (!(level() instanceof ServerLevel serverLevel) || !(getControllingEntity() instanceof Player player))
            return;

        com.flansmod.common.vector.Vector3f size = configType.getHarvestBoxSize();
        com.flansmod.common.vector.Vector3f offset = configType.getHarvestBoxPos();
        double sx = Math.min(8D, Math.abs(size.x));
        double sy = Math.min(8D, Math.abs(size.y));
        double sz = Math.min(8D, Math.abs(size.z));
        if (sx < 0.01D || sy < 0.01D || sz < 0.01D)
            return;
        Vec3 centre = localToWorld(offset.x + size.x * 0.5D, offset.y + size.y * 0.5D, offset.z + size.z * 0.5D);
        Vec3 extent = new Vec3(Math.abs(getForwardVector().x) * sx + Math.abs(getUpVector().x) * sy + Math.abs(getRightVector().x) * sz,
            Math.abs(getForwardVector().y) * sx + Math.abs(getUpVector().y) * sy + Math.abs(getRightVector().y) * sz,
            Math.abs(getForwardVector().z) * sx + Math.abs(getUpVector().z) * sy + Math.abs(getRightVector().z) * sz).scale(0.5D);
        AABB bounds = new AABB(centre.subtract(extent), centre.add(extent));
        int processed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(bounds.minX), Mth.floor(bounds.minY), Mth.floor(bounds.minZ),
            Mth.floor(bounds.maxX), Mth.floor(bounds.maxY), Mth.floor(bounds.maxZ)))
        {
            if (++processed > 128)
                break;
            if (!serverLevel.mayInteract(player, pos))
                continue;
            BlockState state = serverLevel.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(serverLevel, pos) < 0F || !canHarvestState(state))
                continue;
            BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
            if (blockEntity != null)
                continue; // Never silently consume container contents or machines.

            if (configType.isCollectHarvest())
            {
                List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null, player, ItemStack.EMPTY);
                if (!ModUtils.destroyBlock(serverLevel, pos, player, false))
                    continue;
                for (ItemStack drop : drops)
                {
                    ItemStack remainder = insertIntoCargo(drop.copy());
                    if (!remainder.isEmpty())
                        Block.popResource(serverLevel, pos, remainder);
                }
            }
            else
                ModUtils.destroyBlock(serverLevel, pos, player, configType.isDropHarvest());
        }
    }

    protected boolean canHarvestState(@NotNull BlockState state)
    {
        if (configType == null || configType.getMaterialsHarvested().isEmpty())
            return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(java.util.Locale.ROOT);
        for (String raw : configType.getMaterialsHarvested())
        {
            String token = raw.toLowerCase(java.util.Locale.ROOT).replace("minecraft:", "").replace("material.", "");
            if (path.contains(token) || state.getTags().anyMatch(tag -> tag.location().getPath().contains(token)))
                return true;
            if (token.equals("wood") && (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)))
                return true;
            if (token.equals("leaves") && state.is(BlockTags.LEAVES))
                return true;
        }
        return false;
    }

    protected ItemStack insertIntoCargo(@NotNull ItemStack incoming)
    {
        if (driveableData == null || incoming.isEmpty())
            return incoming;
        for (int slot = 0; slot < driveableData.getNumCargoSlots() && !incoming.isEmpty(); slot++)
        {
            ItemStack existing = driveableData.getCargo(slot);
            if (existing.isEmpty())
            {
                int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
                ItemStack placed = incoming.copy();
                placed.setCount(moved);
                driveableData.setCargo(slot, placed);
                incoming.shrink(moved);
            }
            else if (ItemStack.isSameItemSameTags(existing, incoming) && existing.getCount() < existing.getMaxStackSize())
            {
                int moved = Math.min(incoming.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                driveableData.setCargo(slot, existing);
                incoming.shrink(moved);
            }
        }
        return incoming;
    }

    @Override
    public EntityDimensions getDimensions(@NotNull Pose pose)
    {
        DriveablePart core = driveableData == null ? null : driveableData.getPart(EnumDriveablePart.CORE);
        CollisionBox box = core == null ? null : core.getBox();
        if (box == null)
            return super.getDimensions(pose);
        // Entity collision remains intentionally compact. Precise hits use the
        // rotated per-part boxes and culling uses the wider detection radius.
        float width = Mth.clamp(Math.max(box.getWidth(), box.getDepth()), 0.5F, 4F);
        float height = Mth.clamp(box.getHeight(), 0.5F, 6F);
        return EntityDimensions.scalable(width, height);
    }

    @Override
    @NotNull
    public AABB getBoundingBoxForCulling()
    {
        float radius = configType == null ? 8F : Mth.clamp(configType.getBulletDetectionRadius() + 2F, 4F, 64F);
        return new AABB(getX() - radius, getY() - radius, getZ() - radius, getX() + radius, getY() + radius, getZ() + radius);
    }

    public static Optional<Driveable> spawn(@NotNull Level level, @NotNull DriveableType type, double x, double y, double z,
                                            float yaw, @Nullable Player placer, @Nullable ItemStack sourceStack)
    {
        if (level.isClientSide || !validSpawnCoordinate(x) || !validSpawnCoordinate(y) || !validSpawnCoordinate(z) || !Float.isFinite(yaw))
            return Optional.empty();
        Driveable entity = create(level, type, x, y, z, yaw, placer, sourceStack == null ? ItemStack.EMPTY : sourceStack);
        if (entity == null || !level.getWorldBorder().isWithinBounds(entity.blockPosition()) || !level.noCollision(entity, entity.getBoundingBox()))
            return Optional.empty();
        return level.addFreshEntity(entity) ? Optional.of(entity) : Optional.empty();
    }

    @Nullable
    public static Driveable create(@NotNull Level level, @NotNull DriveableType type, double x, double y, double z,
                                   float yaw, @Nullable Player placer, @NotNull ItemStack sourceStack)
    {
        if (type instanceof PlaneType planeType)
            return new Plane(level, planeType, x, y, z, yaw, placer, sourceStack);
        if (type instanceof VehicleType vehicleType)
            return new Vehicle(level, vehicleType, x, y, z, yaw, placer, sourceStack);
        if (type instanceof MechaType mechaType)
            return new Mecha(level, mechaType, x, y, z, yaw, placer, sourceStack);
        return null;
    }

    private static boolean validSpawnCoordinate(double coordinate)
    {
        return Double.isFinite(coordinate) && Math.abs(coordinate) <= MAX_SPAWN_COORDINATE;
    }
}
