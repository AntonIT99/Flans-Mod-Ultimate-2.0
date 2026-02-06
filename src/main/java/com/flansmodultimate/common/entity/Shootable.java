package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.teams.TeamsRound;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class Shootable extends Entity implements IEntityAdditionalSpawnData
{
    public static final float DEFAULT_HITBOX_SIZE = 0.5F;

    public static final String NBT_TYPE_NAME = "type";

    protected static final EntityDataAccessor<String> DATA_SHOOTABLE_TYPE = SynchedEntityData.defineId(Shootable.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Float> DATA_HITBOX_SIZE = SynchedEntityData.defineId(Shootable.class, EntityDataSerializers.FLOAT);

    protected String shortname = StringUtils.EMPTY;
    protected Vec3 velocity = new Vec3(0, 0, 0);
    /** Stop repeat detonations */
    protected boolean detonated;

    protected Shootable(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    protected Shootable(EntityType<?> entityType, Level level, ShootableType type)
    {
        this(entityType, level);
        setShortName(type.getShortName());
        setHitboxSize(type.getHitBoxSize());
    }

    public abstract ShootableType getConfigType();

    public String getShortName()
    {
        return shortname;
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(DATA_SHOOTABLE_TYPE, shortname);
    }

    public float getHitboxSize()
    {
        return entityData.get(DATA_HITBOX_SIZE);
    }

    public void setHitboxSize(float hitboxSize)
    {
        hitboxSize = Math.max(0.01F, hitboxSize);
        entityData.set(DATA_HITBOX_SIZE, hitboxSize);
        if (!level().isClientSide)
            refreshDimensions();
    }

    @Override
    @NotNull
    public Vec3 getDeltaMovement()
    {
        return velocity;
    }

    @Override
    public void setDeltaMovement(@NotNull Vec3 deltaMovement)
    {
        super.setDeltaMovement(deltaMovement);
        velocity = deltaMovement;
        hasImpulse = true;
    }

    @Override
    public void setDeltaMovement(double pX, double pY, double pZ)
    {
        super.setDeltaMovement(pX, pY, pZ);
        velocity = new Vec3(pX, pY, pZ);
        hasImpulse = true;
    }

    @Override
    public void lerpMotion(double pX, double pY, double pZ)
    {
        // no-op: ignore vanilla client interpolation
    }

    @Override
    @NotNull
    public EntityDimensions getDimensions(@NotNull Pose pose)
    {
        float hitboxSize = getHitboxSize();
        return EntityDimensions.fixed(hitboxSize, hitboxSize);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(DATA_SHOOTABLE_TYPE, StringUtils.EMPTY);
        entityData.define(DATA_HITBOX_SIZE, DEFAULT_HITBOX_SIZE);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);
        if (DATA_HITBOX_SIZE.equals(key))
            refreshDimensions();
    }

    @Override
    @NotNull
    public Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        buf.writeUtf(shortname);
        buf.writeFloat(getHitboxSize());
        buf.writeDouble(velocity.x);
        buf.writeDouble(velocity.y);
        buf.writeDouble(velocity.z);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        setShortName(buf.readUtf());
        setHitboxSize(buf.readFloat());
        double vx = buf.readDouble();
        double vy = buf.readDouble();
        double vz = buf.readDouble();
        velocity = new Vec3(vx, vy, vz);
        setDeltaMovement(velocity);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        setShortName(tag.getString(NBT_TYPE_NAME));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        tag.putString(NBT_TYPE_NAME, shortname);
    }

    protected void applyDragAndGravity()
    {
        double gravity = ShootableType.FALL_SPEED_COEFFICIENT * getConfigType().getFallSpeed();
        float drag = ShootableType.AIR_DEFAULT_DRAG;

        if (isInWater())
            drag = ShootableType.WATER_DEFAULT_DRAG;
        else if (isInLava())
            drag = ShootableType.LAVA_DEFAULT_DRAG;

        velocity = velocity.scale(drag).add(0, -gravity, 0);
        setDeltaMovement(velocity);
    }

    protected boolean shouldDespawn()
    {
        int despawnTime = getConfigType().getDespawnTime();
        if (ModCommonConfig.get().shootableDefaultRespawnTime() > 0)
        {
            despawnTime = Math.min(despawnTime, ModCommonConfig.get().shootableDefaultRespawnTime());
        }
        return despawnTime > 0 && tickCount > despawnTime;
    }

    protected void handleDetonationConditions(Level level)
    {
        ShootableType configType = getConfigType();

        if (level.isClientSide)
            return;

        // Fuse
        if (configType.getFuse() > 0 && tickCount > configType.getFuse())
            detonate(level);

        // Proximity triggers
        if (configType.getLivingProximityTrigger() <= 0 && configType.getDriveableProximityTrigger() <= 0)
            return;

        float checkRadius = Math.max(configType.getLivingProximityTrigger(), configType.getDriveableProximityTrigger());
        double rLivingSq = configType.getLivingProximityTrigger() * configType.getLivingProximityTrigger();
        double rDriveableSq = configType.getDriveableProximityTrigger() * configType.getDriveableProximityTrigger();

        List<Entity> list = ModUtils.queryEntities(level, this, getBoundingBox().inflate(checkRadius, checkRadius, checkRadius));
        for (Entity entity : list)
        {
            if (isShooterEntity(entity) && tickCount < 10)
                continue;

            // Living proximity
            if (entity instanceof LivingEntity living && living.distanceToSqr(this) < rLivingSq)
            {
                if (ModCommonConfig.get().shootableProximityTriggerFriendlyFire())
                {
                    // Check to prevent friendly fire
                    Optional<TeamsRound> currentRound = FlansMod.teamsManager.getCurrentRound();
                    LivingEntity owner = getOwner().orElse(null);
                    if (currentRound.isPresent()
                        && owner instanceof ServerPlayer attacker
                        && entity instanceof ServerPlayer victim
                        && !currentRound.get().getGametype().canPlayerBeAttacked(victim, attacker))
                        continue;
                }

                if (!handleEntityInProximityTriggerRange(level, living))
                    continue;

                detonate(level);
                break;
            }

            // Driveable proximity
            if (entity instanceof Driveable driveable && entity.distanceToSqr(this) < rDriveableSq)
            {
                if (!handleEntityInProximityTriggerRange(level, driveable))
                    continue;

                detonate(level);
                break;
            }
        }
    }

    protected abstract boolean handleEntityInProximityTriggerRange(Level level, Entity entity);

    public abstract boolean isShooterEntity(Entity entity);

    public abstract Optional<LivingEntity> getOwner();

    public abstract void detonate(Level level);

    protected void detonate(Level level, LivingEntity causingEntity)
    {
        detonated = true;
        ShootingHelper.onDetonate(level, getConfigType(), position(), this, causingEntity);
    }
}
