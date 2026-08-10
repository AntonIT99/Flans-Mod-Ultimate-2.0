package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Parachute extends Entity implements IEntityWithComplexSpawn, IFlanEntity<ToolType>
{
    public static final float DEFAULT_HITBOX_WIDTH = 1.0F;
    public static final float DEFAULT_HITBOX_HEIGHT = 0.5F;
    public static final String NBT_TYPE_NAME = "type";

    private static final EntityDataAccessor<String> DATA_TOOL_TYPE = SynchedEntityData.defineId(Parachute.class, EntityDataSerializers.STRING);
    private static final double START_Y_OFFSET = -2.5D;
    private static final double RIDER_Y_OFFSET = 2.5D;
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

    public Parachute(Level level, ToolType type, Player player)
    {
        this(FlansMod.parachuteEntity.get(), level);
        configType = type;
        setShortName(type.getShortName());
        setPos(player.getX(), player.getY() + START_Y_OFFSET, player.getZ());
        setYRot(player.getYRot());
        yRotO = getYRot();
    }

    public static boolean canUseParachute(Player player)
    {
        if (!player.isAlive())
            return false;

        AABB clearanceBox = player.getBoundingBox().inflate(0.0D, 3.0D, 0.0D);
        return player.level().noCollision(player, clearanceBox);
    }

    @Nullable
    public static Parachute spawnAndMount(Level level, Player player, ToolType type)
    {
        if (level.isClientSide || !canUseParachute(player))
            return null;

        Parachute parachute = new Parachute(level, type, player);
        if (!level.addFreshEntity(parachute))
            return null;

        if (player.getVehicle() != null)
            player.stopRiding();

        if (!player.startRiding(parachute, true))
        {
            parachute.discard();
            return null;
        }
        return parachute;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(DATA_TOOL_TYPE, StringUtils.EMPTY);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf)
    {
        buf.writeUtf(getShortName());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf)
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

        motionX *= HORIZONTAL_DRAG;
        motionZ *= HORIZONTAL_DRAG;
        setDeltaMovement(motionX, DESCENT_SPEED, motionZ);
        move(MoverType.SELF, getDeltaMovement());

        if (!level().isClientSide && (onGround() || isInWater()))
            discard();
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move)
    {
        move.accept(passenger, getX(), getY() + RIDER_Y_OFFSET, getZ());
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
        if (!level().isClientSide)
            discard();
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
    @NotNull
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity)
    {
        return super.getAddEntityPacket(serverEntity);
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
            FlansMod.log.warn("Unknown parachute type {}, discarding.", getShortName());
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
