package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansModEntities;
import com.flansmodultimate.common.physics.ModPhysics;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.*;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/**
 * An open parachute, which carries its rider down at a steady descent until it touches the ground. The entity rides at the
 * rider's own feet, so it lands when the rider would, and its canopy is drawn above both; a blast nearby leaves it be, as
 * it carries whoever just left a burning aircraft, but an enabled direct hit collapses it.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Parachute extends FlanSpawnEntity implements SpawnDataEntity, IFlanEntity<ToolType>
{
    public static final float DEFAULT_HITBOX_WIDTH = 1.0F;
    public static final float DEFAULT_HITBOX_HEIGHT = 0.5F;
    public static final String NBT_TYPE_NAME = "type";

    private static final EntityDataAccessor<String> DATA_TOOL_TYPE = SynchedEntityData.defineId(Parachute.class, EntityDataSerializers.STRING);
    private static final double DESCENT_SPEED = -0.3D;
    private static final double STEERING_ACCELERATION = 0.025D;
    private static final double HORIZONTAL_DRAG = 0.93D;

    protected ToolType configType;
    protected String shortname = StringUtils.EMPTY;

    public Parachute(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
        noPhysics = false;
        noCulling = true;
    }

    public Parachute(Level level, ToolType type, LivingEntity rider)
    {
        this(FlansModEntities.parachuteEntity.get(), level);
        configType = type;
        setShortName(type.getShortName());
        setPos(rider.getX(), rider.getY(), rider.getZ());
        setYRot(rider.getYRot());
        yRotO = getYRot();
    }

    public static boolean canUseParachute(LivingEntity rider)
    {
        if (!rider.isAlive())
            return false;

        AABB clearanceBox = rider.getBoundingBox().inflate(0.0D, 3.0D, 0.0D);
        return rider.level().noCollision(rider, clearanceBox);
    }

    @Nullable
    public static Parachute spawnAndMount(Level level, LivingEntity rider, ToolType type)
    {
        if (level.isClientSide || !canUseParachute(rider))
            return null;

        Parachute parachute = new Parachute(level, type, rider);
        if (!level.addFreshEntity(parachute))
            return null;

        if (rider.getVehicle() != null)
            rider.stopRiding();

        if (!rider.startRiding(parachute, true))
        {
            parachute.discard();
            return null;
        }
        return parachute;
    }

    @Override
    protected void defineEntityData(SynchedDataDefinition data)
    {
        data.define(DATA_TOOL_TYPE, StringUtils.EMPTY);
    }

    @Override
    public void writeSpawnData(PacketBuffer buf)
    {
        buf.writeUtf(getShortName());
    }

    @Override
    public void readSpawnData(PacketBuffer buf)
    {
        setShortName(buf.readUtf());
        resolveTypeOrDiscard();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        setShortName(tag.getString(NBT_TYPE_NAME));
        resolveTypeOrDiscard();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        tag.putString(NBT_TYPE_NAME, getShortName());
    }

    @Override
    public void tick()
    {
        super.tick();

        if (getConfigType() == null)
        {
            discard();
            return;
        }

        Entity rider = getFirstPassenger();
        if (!level().isClientSide && (rider == null || rider.getVehicle() != this || !rider.isAlive()))
        {
            discard();
            return;
        }

        if (rider != null)
            rider.fallDistance = 0.0F;

        Vec3 motion = getDeltaMovement();
        double motionX = motion.x;
        double motionZ = motion.z;

        if (rider instanceof LivingEntity living)
        {
            double forward = living.zza;
            double strafe = living.xxa;
            double sinYaw = -Math.sin(living.getYRot() * Mth.DEG_TO_RAD);
            double cosYaw = Math.cos(living.getYRot() * Mth.DEG_TO_RAD);

            motionX += (forward * sinYaw + strafe * cosYaw) * STEERING_ACCELERATION;
            motionZ += (forward * cosYaw - strafe * sinYaw) * STEERING_ACCELERATION;

            yRotO = getYRot();
            setYRot(living.getYRot());
        }

        motionX *= ModPhysics.dragRetention(HORIZONTAL_DRAG, level());
        motionZ *= ModPhysics.dragRetention(HORIZONTAL_DRAG, level());
        setDeltaMovement(motionX, ModPhysics.gravity(DESCENT_SPEED, level()), motionZ);
        move(MoverType.SELF, getDeltaMovement());

        if (!level().isClientSide && (onGround() || isInWater()))
            discard();
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move)
    {
        move.accept(passenger, getX(), getY(), getZ());
        passenger.setDeltaMovement(Vec3.ZERO);
        passenger.fallDistance = 0.0F;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger)
    {
        return getPassengers().isEmpty() && passenger instanceof LivingEntity;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger()
    {
        Entity passenger = getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (!ModCommonConfig.parachutesCanBeHit() || amount <= 0F)
            return false;
        if (!level().isClientSide)
            discard();
        return true;
    }

    /**
     * A canopy is left alone by blasts: it is a wide box hanging over whoever just jumped from a burning aircraft or past a
     * gun position, and the explosion of the aircraft itself would otherwise collapse it in mid-air. Direct hits can collapse it when enabled in the common config.
     */
    @Override
    public boolean ignoreExplosion(@NotNull Explosion explosion)
    {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull net.minecraft.world.level.block.state.BlockState state, @NotNull net.minecraft.core.BlockPos pos)
    {
        fallDistance = 0.0F;
    }

    @Override
    public ItemStack getPickedResult(HitResult target)
    {
        ToolType type = getConfigType();
        return type == null ? ItemStack.EMPTY : ModUtils.getItemStack(type).orElse(ItemStack.EMPTY);
    }

    @Override
    public String getShortName()
    {
        return entityData.get(DATA_TOOL_TYPE);
    }

    public void setShortName(String shortname)
    {
        this.shortname = StringUtils.defaultString(shortname);
        entityData.set(DATA_TOOL_TYPE, this.shortname);
    }

    @Override
    public ToolType getConfigType()
    {
        if (configType == null)
            resolveType();
        return configType;
    }

    private void resolveTypeOrDiscard()
    {
        if (resolveType() == null)
        {
            FlansLog.log.warn("Unknown parachute type {}, discarding.", getShortName());
            discard();
        }
    }

    private @Nullable ToolType resolveType()
    {
        String typeName = getShortName();
        if (StringUtils.isBlank(typeName))
            typeName = shortname;

        if (InfoType.getInfoType(typeName) instanceof ToolType toolType)
        {
            configType = toolType;
            return configType;
        }
        return null;
    }
}
