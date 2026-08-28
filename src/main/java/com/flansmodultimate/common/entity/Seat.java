package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.IControllable;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.SeatInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight mount proxy for a driveable seat.
 *
 * <p>Seats are intentionally not persisted. The owning driveable recreates
 * them after loading and the parent entity ID provides an O(1) client bind,
 * avoiding the legacy loaded-entity scans.</p>
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Seat extends Entity implements IControllable
{
    private static final EntityDataAccessor<Integer> DATA_PARENT_ID = SynchedEntityData.defineId(Seat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SEAT_INDEX = SynchedEntityData.defineId(Seat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_AIM_YAW = SynchedEntityData.defineId(Seat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AIM_PITCH = SynchedEntityData.defineId(Seat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_INPUT_MASK = SynchedEntityData.defineId(Seat.class, EntityDataSerializers.INT);

    private static final int MAX_ORPHAN_TICKS = 100;
    private static final double LEGACY_PLAYER_RIDING_OFFSET = -0.35D;
    private static final double MECHA_COCKPIT_RIDING_OFFSET = -0.2D;

    @Getter @Nullable
    protected Driveable driveable;
    @Getter @Nullable
    protected SeatInfo seatInfo;
    @Getter
    protected float prevAimYaw;
    @Getter
    protected float prevAimPitch;

    private boolean clientViewAimInitialized;
    private float clientViewAimYaw;
    private float clientViewAimPitch;
    private float clientViewParentYaw;

    private int orphanTicks;
    private int localInputMask;
    private int previousInputMask;
    private int lastInputSequence;
    private long lastInputGameTime;
    private boolean receivedInputSequence;

    public Seat(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
        noPhysics = true;
    }

    public Seat(Level level, Driveable parent, int seatIndex, SeatInfo info)
    {
        super(FlansMod.seatEntity.get(), level);
        noPhysics = true;
        bind(parent, seatIndex, info);
    }

    public void bind(@NotNull Driveable parent, int seatIndex, @Nullable SeatInfo info)
    {
        driveable = parent;
        seatInfo = info;
        entityData.set(DATA_PARENT_ID, parent.getId());
        entityData.set(DATA_SEAT_INDEX, seatIndex);
        parent.registerSeatProxy(this);
        snapToParent();
    }

    public int getParentId()
    {
        return entityData.get(DATA_PARENT_ID);
    }

    public int getSeatIndex()
    {
        return entityData.get(DATA_SEAT_INDEX);
    }

    public float getAimYaw()
    {
        return entityData.get(DATA_AIM_YAW);
    }

    public float getAimPitch()
    {
        return entityData.get(DATA_AIM_PITCH);
    }

    public float getViewAimYaw()
    {
        return clientViewAimInitialized ? clientViewAimYaw : getAimYaw();
    }

    public float getViewAimPitch()
    {
        return clientViewAimInitialized ? clientViewAimPitch : getAimPitch();
    }

    public float getRequestedAimYaw()
    {
        return getViewAimYaw();
    }

    public float getRequestedAimPitch()
    {
        return getViewAimPitch();
    }

    public boolean isAimRequestPending(float epsilon)
    {
        return Math.abs(Mth.wrapDegrees(getRequestedAimYaw() - getAimYaw())) >= epsilon
            || Math.abs(getRequestedAimPitch() - getAimPitch()) >= epsilon;
    }

    public float getMountedViewYaw()
    {
        return driveable == null ? getYRot() : Mth.wrapDegrees(getMountedForwardYaw() + getViewAimYaw());
    }

    /** World yaw of a rider looking straight along the rendered driveable. */
    public float getMountedForwardYaw()
    {
        if (driveable == null)
            return getYRot();
        return driveable.getEntityFacingYaw();
    }

    public float getMountedViewPitch()
    {
        return driveable == null ? getXRot()
            : Mth.clamp(driveable.getPitch() + getViewAimPitch(), -89.9F, 89.9F);
    }

    public int getInputMask()
    {
        return entityData.get(DATA_INPUT_MASK);
    }

    public Entity getRiddenByEntity()
    {
        return getFirstPassenger();
    }

    public boolean isDriverSeat()
    {
        return seatInfo != null ? seatInfo.isDriver() : getSeatIndex() == 0;
    }

    public boolean isInputDown(int input)
    {
        return DriveableInput.isDown(getInputMask(), input);
    }

    public boolean isInputRising(int input)
    {
        return DriveableInput.isDown(getInputMask(), input) && !DriveableInput.isDown(previousInputMask, input);
    }

    public void finishInputTick()
    {
        previousInputMask = getInputMask();
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(DATA_PARENT_ID, -1);
        entityData.define(DATA_SEAT_INDEX, -1);
        entityData.define(DATA_AIM_YAW, 0F);
        entityData.define(DATA_AIM_PITCH, 0F);
        entityData.define(DATA_INPUT_MASK, 0);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        // Proxy entities are noSave. Their parent owns all persistent state.
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        // Proxy entities are noSave. Their parent owns all persistent state.
    }

    @Override
    public void tick()
    {
        super.tick();
        prevAimYaw = getAimYaw();
        prevAimPitch = getAimPitch();

        if (!resolveParent())
        {
            if (++orphanTicks > MAX_ORPHAN_TICKS)
                discard();
            return;
        }

        orphanTicks = 0;
        if (driveable == null || driveable.isRemoved())
        {
            discard();
            return;
        }

        if (seatInfo == null && driveable.getConfigType() != null)
            seatInfo = driveable.getConfigType().getSeat(getSeatIndex());

        consumeClientParentYaw();
        driveable.registerSeatProxy(this);
        snapToParent();

        EnumDriveablePart part = seatInfo == null ? EnumDriveablePart.CORE : seatInfo.getPart();
        if (!driveable.isPartIntact(part) && isVehicle())
            ejectPassengers();

        Entity passenger = getFirstPassenger();
        if (passenger != null)
        {
            passenger.fallDistance = 0F;
            if (!level().isClientSide)
                driveable.markUsed();
        }
        else if (!level().isClientSide && getInputMask() != 0)
        {
            entityData.set(DATA_INPUT_MASK, 0);
        }
        if (passenger == null && level().isClientSide)
            clientViewAimInitialized = false;
    }

    private boolean resolveParent()
    {
        if (driveable != null && !driveable.isRemoved() && driveable.getId() == getParentId())
            return true;

        Entity candidate = level().getEntity(getParentId());
        if (candidate instanceof Driveable parent)
        {
            driveable = parent;
            seatInfo = parent.getConfigType() == null ? null : parent.getConfigType().getSeat(getSeatIndex());
            return true;
        }
        return false;
    }

    private void snapToParent()
    {
        if (driveable == null)
            return;
        Vec3 position = driveable.getSeatWorldPosition(getSeatIndex());
        setPos(position.x, position.y, position.z);
        setDeltaMovement(Vec3.ZERO);
        setYRot(Mth.wrapDegrees(getMountedForwardYaw() + getAimYaw()));
        setXRot(Mth.clamp(driveable.getEntityFacingPitch() + getAimPitch(), -89.9F, 89.9F));
    }

    /**
     * Once bound, the seat follows the already-interpolated parent locally.
     * Applying its own movement packets as well would introduce a second,
     * slightly different camera position every network tick.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport)
    {
        if (level().isClientSide && driveable != null)
            return;
        super.lerpTo(x, y, z, yaw, pitch, steps, teleport);
    }

    @Override
    public void lerpMotion(double x, double y, double z)
    {
        if (level().isClientSide && driveable != null)
            return;
        super.lerpMotion(x, y, z);
    }

    @Override
    public boolean isPickable()
    {
        return isAlive();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (driveable == null || level().isClientSide)
            return driveable != null;
        EnumDriveablePart part = seatInfo == null ? EnumDriveablePart.CORE : seatInfo.getPart();
        return driveable.damagePart(part, amount, source);
    }

    @Override
    @NotNull
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        if (driveable == null || !driveable.canPlayerAccess(player))
            return InteractionResult.PASS;
        if (getFirstPassenger() != null && getFirstPassenger() != player)
            return InteractionResult.sidedSuccess(level().isClientSide);
        if (level().isClientSide)
            return InteractionResult.SUCCESS;

        if (player == getFirstPassenger())
        {
            player.stopRiding();
            return InteractionResult.CONSUME;
        }
        if (player.getVehicle() != null)
            player.stopRiding();
        return player.startRiding(this, true) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger)
    {
        return getPassengers().isEmpty() && passenger instanceof LivingEntity;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move)
    {
        if (!hasPassenger(passenger))
            return;
        // Legacy driveable seat coordinates already describe the rider anchor.
        // EntityPlayer 1.7.10 contributed a -0.35 Y offset here, whereas the
        // modern default mount offset raises the player by about 0.45 blocks.
        double ridingOffset = getPassengerRidingOffset(passenger);
        Vec3 riderPosition = driveable == null
            ? new Vec3(getX(), getY() + ridingOffset, getZ())
            : driveable.getRiderWorldPosition(getSeatIndex(), ridingOffset);
        move.accept(passenger, riderPosition.x, riderPosition.y, riderPosition.z);
        passenger.setDeltaMovement(driveable == null ? Vec3.ZERO : driveable.getDeltaMovement());
        passenger.fallDistance = 0F;
    }

    public double getPassengerRidingOffset(@NotNull Entity passenger)
    {
        if (!(passenger instanceof Player))
            return 0D;
        return LEGACY_PLAYER_RIDING_OFFSET + (driveable instanceof Mecha ? MECHA_COCKPIT_RIDING_OFFSET : 0D);
    }

    @Override
    @NotNull
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger)
    {
        if (driveable != null)
            return driveable.getSafeDismountPosition(passenger, getSeatIndex());
        return super.getDismountLocationForPassenger(passenger);
    }

    public boolean acceptInput(@NotNull ServerPlayer player, int mask, float requestedYaw, float requestedPitch, int sequence)
    {
        if (getFirstPassenger() != player || driveable == null || !isNewSequence(sequence))
            return false;

        long now = level().getGameTime();
        // Aim authority is budgeted by elapsed server ticks, not packet count.
        // Same-tick packets may still update held controls, but cannot multiply
        // the configured turret/head aiming speed.
        long elapsed = receivedInputSequence ? Math.max(0L, Math.min(10L, now - lastInputGameTime)) : 1L;
        lastInputGameTime = now;
        lastInputSequence = sequence;
        receivedInputSequence = true;

        SeatInfo info = seatInfo;
        float yawSpeed = info == null ? 8F : Mth.clamp(Math.abs(info.getAimingSpeed().x), 0.25F, 45F);
        float pitchSpeed = info == null ? 8F : Mth.clamp(Math.abs(info.getAimingSpeed().y), 0.25F, 45F);
        float yawDelta = Mth.clamp(Mth.wrapDegrees(requestedYaw - getAimYaw()), -yawSpeed * elapsed, yawSpeed * elapsed);
        float pitchDelta = Mth.clamp(requestedPitch - getAimPitch(), -pitchSpeed * elapsed, pitchSpeed * elapsed);

        float yaw = getAimYaw() + yawDelta;
        float pitch = getAimPitch() + pitchDelta;
        if (info != null)
        {
            if (info.getMinYaw() <= -359.9F && info.getMaxYaw() >= 359.9F)
                yaw = Mth.wrapDegrees(yaw);
            else
                yaw = Mth.clamp(Mth.wrapDegrees(yaw), info.getMinYaw(), info.getMaxYaw());
            pitch = Mth.clamp(pitch, info.getMinPitch(), info.getMaxPitch());
        }
        else
        {
            yaw = Mth.wrapDegrees(yaw);
            pitch = Mth.clamp(pitch, -89.9F, 89.9F);
        }

        entityData.set(DATA_AIM_YAW, yaw);
        entityData.set(DATA_AIM_PITCH, pitch);
        previousInputMask = getInputMask();
        entityData.set(DATA_INPUT_MASK, DriveableInput.sanitize(mask));
        return true;
    }

    private boolean isNewSequence(int sequence)
    {
        return !receivedInputSequence || sequence - lastInputSequence > 0;
    }

    @Override
    public void onMouseMoved(double deltaX, double deltaY)
    {
        SeatInfo info = seatInfo;
        float yawSpeed = info == null ? 0.15F : Math.max(0.01F, Math.abs(info.getAimingSpeed().x) * 0.075F);
        float pitchSpeed = info == null ? 0.15F : Math.max(0.01F, Math.abs(info.getAimingSpeed().y) * 0.075F);
        applyClientAimDelta((float) deltaX * yawSpeed, -(float) deltaY * pitchSpeed);
    }

    /** Applies a local view impulse while retaining the limits declared by this seat. */
    public void applyClientAimDelta(float yawDelta, float pitchDelta)
    {
        if (!Float.isFinite(yawDelta) || !Float.isFinite(pitchDelta))
            return;

        initializeClientViewAim();
        SeatInfo info = seatInfo;
        float yaw = clientViewAimYaw + yawDelta;
        float pitch = clientViewAimPitch + pitchDelta;
        if (info != null)
        {
            if (info.getMinYaw() <= -359.9F && info.getMaxYaw() >= 359.9F)
                yaw = Mth.wrapDegrees(yaw);
            else
                yaw = Mth.clamp(Mth.wrapDegrees(yaw), info.getMinYaw(), info.getMaxYaw());
            pitch = Mth.clamp(pitch, info.getMinPitch(), info.getMaxPitch());
        }
        clientViewAimYaw = yaw;
        clientViewAimPitch = Mth.clamp(pitch, -89.9F, 89.9F);
    }

    public void synchronizeClientViewWithAim()
    {
        clientViewAimYaw = getAimYaw();
        clientViewAimPitch = getAimPitch();
        clientViewParentYaw = driveable == null ? 0F : driveable.getYaw();
        clientViewAimInitialized = true;
    }

    /** Removes torso rotation from relative aim so a mecha look input is consumed only once. */
    public void consumeAimYaw(float yawDelta)
    {
        if (Float.isFinite(yawDelta))
            entityData.set(DATA_AIM_YAW, Mth.wrapDegrees(getAimYaw() - yawDelta));
    }

    private void consumeClientParentYaw()
    {
        if (!level().isClientSide || !clientViewAimInitialized || driveable == null)
            return;
        float parentYaw = driveable.getYaw();
        float yawDelta = Mth.wrapDegrees(parentYaw - clientViewParentYaw);
        clientViewAimYaw = Mth.wrapDegrees(clientViewAimYaw - yawDelta);
        clientViewParentYaw = parentYaw;
    }

    private void initializeClientViewAim()
    {
        if (!clientViewAimInitialized)
            synchronizeClientViewWithAim();
    }

    /** Recentres the local view when entering mouse-flight mode. */
    public void resetClientAim()
    {
        clientViewAimYaw = 0F;
        clientViewAimPitch = 0F;
        clientViewParentYaw = driveable == null ? 0F : driveable.getYaw();
        clientViewAimInitialized = true;
    }

    @Override
    public boolean pressKey(int key, Player player, boolean isOnEvent)
    {
        int input = DriveableInput.forLegacyKey(key);
        if (input == 0)
            return key == 10 && driveable != null && driveable.pressKey(key, player, isOnEvent);
        localInputMask |= input;
        entityData.set(DATA_INPUT_MASK, DriveableInput.sanitize(localInputMask));
        if (!level().isClientSide && isOnEvent)
            return serverHandleKeyPress(key, player);
        return true;
    }

    @Override
    public boolean serverHandleKeyPress(int key, Player player)
    {
        return driveable != null && getFirstPassenger() == player && driveable.handleLegacyKey(this, key, player);
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
        entityData.set(DATA_INPUT_MASK, DriveableInput.sanitize(localInputMask));
    }

    @Override
    public Entity getControllingEntity()
    {
        return getFirstPassenger();
    }

    @Override
    public boolean isDead()
    {
        return isRemoved();
    }

    @Override
    public float getPlayerRoll()
    {
        return driveable == null ? 0F : driveable.getPlayerRoll();
    }

    @Override
    public float getPrevPlayerRoll()
    {
        return driveable == null ? 0F : driveable.getPrevPlayerRoll();
    }

    @Override
    public float getCameraDistance()
    {
        return driveable == null ? 4F : driveable.getCameraDistance();
    }

    @Override
    public LivingEntity getCamera()
    {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public Seat getSeat(LivingEntity living)
    {
        return hasPassenger(living) ? this : null;
    }
}
