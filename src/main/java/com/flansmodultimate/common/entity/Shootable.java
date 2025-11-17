package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class Shootable extends Entity implements IEntityAdditionalSpawnData
{
    public static final float DEFAULT_HITBOX_SIZE = 0.5F;

    protected static final EntityDataAccessor<String> SHOOTABLE_TYPE = SynchedEntityData.defineId(Shootable.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> HITBOX_SIZE = SynchedEntityData.defineId(Shootable.class, EntityDataSerializers.FLOAT);

    protected String shortname = StringUtils.EMPTY;
    protected Vec3 velocity = new Vec3(0, 0, 0);

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

    public String getShortName()
    {
        shortname = entityData.get(SHOOTABLE_TYPE);
        return shortname;
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(SHOOTABLE_TYPE, shortname);
    }

    public float getHitboxSize()
    {
        return entityData.get(HITBOX_SIZE);
    }

    public void setHitboxSize(float hitboxSize)
    {
        hitboxSize = Math.max(0.01F, hitboxSize);
        entityData.set(HITBOX_SIZE, hitboxSize);
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
        entityData.define(SHOOTABLE_TYPE, StringUtils.EMPTY);
        entityData.define(HITBOX_SIZE, DEFAULT_HITBOX_SIZE);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);
        if (HITBOX_SIZE.equals(key))
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
        shortname = buf.readUtf();
        setHitboxSize(buf.readFloat());
        double vx = buf.readDouble();
        double vy = buf.readDouble();
        double vz = buf.readDouble();
        velocity = new Vec3(vx, vy, vz);
        setDeltaMovement(velocity);
    }

    protected boolean shouldDespawn(ShootableType configType)
    {
        int despawnTime = configType.getDespawnTime();
        if (ModCommonConfigs.shootableDefaultRespawnTime.get() > 0)
        {
            despawnTime = Math.min(despawnTime, ModCommonConfigs.shootableDefaultRespawnTime.get());
        }
        return despawnTime > 0 && tickCount > despawnTime;
    }

    protected void handleDetonationConditions(Level level, ShootableType configType)
    {
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
            if (isShooterEntity(entity) && (tickCount < 10 || !ModCommonConfigs.shootableProximityTriggerFriendlyFire.get()))
                continue;

            // Living proximity
            if (entity instanceof LivingEntity living && living.distanceToSqr(this) < rLivingSq)
            {
                //TODO: Teams
                //TODO: check team of thrower and check ModCommonConfigs.grenadeProximityTriggerFriendlyFire.get()

                // Friendly fire check
                /*if (TeamsManager.getInstance() != null
                        && TeamsManager.getInstance().currentRound != null
                        && entity instanceof ServerPlayer
                        && player.isPresent()) {

                    EntityDamageSourceFlan damageSource =
                            new EntityDamageSourceFlan(getType().shortName, this, player.get(), getType());

                    if (!TeamsManager.getInstance().currentRound.gametype.playerAttacked((ServerPlayer) entity, damageSource)) {
                        continue;
                    }
                }*/

                if (handleEntityInProximityTriggerRange(level, living))
                    break;
            }

            // Driveable proximity
            if (entity instanceof Driveable driveable && entity.distanceToSqr(this) < rDriveableSq && handleEntityInProximityTriggerRange(level, driveable))
                break;
        }
    }

    protected abstract boolean isShooterEntity(Entity entity);

    protected abstract boolean handleEntityInProximityTriggerRange(Level level, Entity entity);

    protected abstract void detonate(Level level);
}
