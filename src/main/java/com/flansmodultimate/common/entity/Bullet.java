package com.flansmodultimate.common.entity;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.PenetrationLoss;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.raytracing.FlansModRaytracer;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.event.BulletHitEvent;
import com.flansmodultimate.event.BulletLockOnEvent;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.network.server.PacketManualGuidance;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Bullet extends Shootable implements IFlanEntity<BulletType>
{
    public static final int RENDER_DISTANCE = 128;

    protected static final String NBT_ATTACKER = "attacker_uuid";
    protected static final String NBT_SHOOTER = "shooter_uuid";
    protected static final String NBT_FIREABLE_GUN = "fireable_gun";
    protected static final String NBT_FIREABLE_GUN_TYPE_NAME = "info_type";
    protected static final String NBT_FIREABLE_GUN_SPREAD = "spread";
    protected static final String NBT_FIREABLE_GUN_SPEED = "speed";
    protected static final String NBT_FIREABLE_GUN_DAMAGE = "damage";
    protected static final String NBT_FIREABLE_GUN_SPREAD_PATTERN = "spread_pattern";

    protected BulletType configType;
    protected FiredShot firedShot;
    /** Kill bullets after 30 seconds */
    protected int bulletLife = 600;
    protected int ticksInAir;
    protected boolean initialTick = true;
    protected double initialSpeed;
    protected int soundTime;
    protected Vec3 lookVector;
    protected Vec3 initialPos;
    protected boolean hasSetLook;
    protected int impactX;
    protected int impactY;
    protected int impactZ;
    protected boolean isFirstPositionSetting;
    protected boolean isPositionUpper = true;
    @Setter
    protected Vec3 ownerPos;
    @Setter
    protected Vec3 ownerLook;

    /** Penetration */
    protected float penetratingPower;
    /** When the bullet loses penetration, the cause and amount is saved to this list */
    @Getter
    protected final List<PenetrationLoss> penetrationLosses = new ArrayList<>();

    /** Hitmarker information on the server side */
    protected boolean lastHitHeadshot = false;
    protected float lastHitPenAmount = 1F;

    /** If this is non-zero, then the player raytrace code will look back in time to when the player thinks their bullet should have hit */
    protected int pingOfShooter;

    /** For homing missiles */
    protected Entity lockedOnTo; // For homing missiles
    protected double prevDistanceToEntity;
    protected boolean toggleLock;
    protected int closeCount;
    protected double prevDistanceToTarget;

    /** Submunitions */
    protected int submunitionDelay = 20;
    protected boolean hasSetSubDelay;

    /** VLS */
    protected boolean hasSetVlsDelay;
    protected int vlsDelay;

    /** These values are used to store the UUIDs until the next entity update is performed. This prevents issues caused by the loading order */
    protected boolean checkForUUIDs;
    /** UUID of the living entity which fired that bullet (either directly or indirectly) */
    protected UUID attackerUUID;
    /** UUID of the entity which shot that bullet (directly) */
    protected UUID shooterUUID;

    @OnlyIn(Dist.CLIENT)
    protected boolean playedFlybySound;

    public Bullet(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Bullet(Level level, FiredShot firedShot, Vec3 origin, Vec3 direction)
    {
        super(FlansMod.bulletEntity.get(), level, firedShot.getBulletType());
        this.firedShot = firedShot;
        configType = firedShot.getBulletType();
        penetratingPower = firedShot.getBulletType().getPenetratingPower();
        setPos(origin);
        setArrowHeading(direction, firedShot.getFireableGun().getSpread() * firedShot.getBulletType().getBulletSpread(), firedShot.getFireableGun().getBulletSpeed());
    }

    @Override
    public BulletType getConfigType()
    {
        if (configType == null && InfoType.getInfoType(getShortName()) instanceof BulletType bType)
        {
            configType = bType;
        }
        return configType;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq)
    {
        return distSq < (RENDER_DISTANCE * RENDER_DISTANCE);
    }

    public void setArrowHeading(Vec3 direction, float spread, float speed)
    {
        final double SPREAD_DIVISOR = 5.0;
        final double BASE_JITTER = 0.005;

        double jitter = BASE_JITTER * (spread / SPREAD_DIVISOR) * speed;
        velocity = direction.normalize().scale(speed).add(random.nextGaussian() * jitter, random.nextGaussian() * jitter, random.nextGaussian() * jitter);
        setDeltaMovement(velocity);
        setOrientation(velocity);
        getLockOnTarget(level());
    }

    protected void setOrientation(Vec3 velocity)
    {
        // horizontal length: sqrt(x^2 + z^2)
        double lengthXZ = Math.hypot(velocity.x, velocity.z);
        // compute yaw/pitch in degrees
        float yaw = (float) Math.toDegrees(Mth.atan2(velocity.x, velocity.z));
        float pitch = (float) Math.toDegrees(Mth.atan2(velocity.y, lengthXZ));

        // set current + previous rotations
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
    }

    public void setVelocity(Vec3 velocity)
    {
        setDeltaMovement(velocity);

        // only initialize rotation once (same behavior as the old check)
        if (xRotO == 0.0F && yRotO == 0.0F)
        {
            setOrientation(velocity);
            moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        }
    }

    /**
     * Find the entity nearest to the missile's trajectory, anglewise
     */
    protected void getLockOnTarget(Level level)
    {
        boolean lockPlanes = configType.isLockOnToPlanes();
        boolean lockVehicles = configType.isLockOnToVehicles();
        boolean lockMechas = configType.isLockOnToMechas();
        boolean lockLivings = configType.isLockOnToLivings();
        boolean lockPlayers = configType.isLockOnToPlayers();

        if (!(lockPlanes || lockVehicles || lockMechas || lockLivings || lockPlayers))
            return;

        Vec3 motionDir = velocity.normalize();
        float maxAngleRad = configType.getMaxLockOnAngle() * Mth.DEG_TO_RAD;
        float minCosAllowed = Mth.cos(maxAngleRad);
        double bestCos = -1.0; // cos in [-1, 1]; bigger = smaller angle
        Entity closestEntity = null;

        for (Entity entity : ModUtils.queryEntitiesInRange(level, this, BulletType.LOCK_ON_RANGE, null))
        {
            if (lockMechas && entity instanceof Mecha
                || lockVehicles && (entity instanceof Vehicle || ModUtils.isVehicleLike(entity))
                || lockPlanes && (entity instanceof Plane || ModUtils.isPlaneLike(entity))
                || lockPlayers && entity instanceof Player
                || lockLivings && entity instanceof LivingEntity)
            {
                Vec3 relDir = entity.position().subtract(position()).normalize();
                double cos = motionDir.dot(relDir);

                if (cos >= minCosAllowed && cos > bestCos)
                {
                    bestCos = cos;
                    closestEntity = entity;
                }
            }
        }

        if (closestEntity != null)
        {
            BulletLockOnEvent bulletLockOnEvent = new BulletLockOnEvent(this, closestEntity);
            MinecraftForge.EVENT_BUS.post(bulletLockOnEvent);
            if (!bulletLockOnEvent.isCanceled())
                lockedOnTo = bulletLockOnEvent.getLockedOnTo();
        }
    }

    @Override
    public boolean displayFireAnimation()
    {
        return false;
    }

    @Override
    public boolean fireImmune()
    {
        return true;
    }

    @Override
    public boolean isOnFire()
    {
        return false;
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        super.writeSpawnData(buf);
        buf.writeInt(firedShot.getCausingEntity().map(Entity::getId).orElse(0));
        buf.writeInt(firedShot.getAttacker().map(Entity::getId).orElse(0));
        buf.writeInt(Optional.ofNullable(lockedOnTo).map(Entity::getId).orElse(0));
        buf.writeInt(impactX);
        buf.writeInt(impactY);
        buf.writeInt(impactZ);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        try
        {
            super.readSpawnData(buf);
            int shooterId = buf.readInt();
            int attackerId = buf.readInt();
            int lockedOnToId = buf.readInt();
            impactX = buf.readInt();
            impactY = buf.readInt();
            impactZ = buf.readInt();

            Level level = level();
            Entity shooter = null;
            LivingEntity attacker = null;

            setOrientation(velocity);

            if (InfoType.getInfoType(shortname) instanceof BulletType type)
                configType = type;
            if (configType == null)
            {
                FlansMod.log.warn("Unknown bullet type {}, discarding.", shortname);
                discard();
            }

            if (shooterId != 0)
                shooter = level.getEntity(attackerId);
            if (attackerId != 0 && level.getEntity(attackerId) instanceof LivingEntity living)
                attacker = living;
            if (lockedOnToId != 0)
                lockedOnTo = level.getEntity(lockedOnToId);

            firedShot = new FiredShot(null, configType, shooter, attacker);
            penetratingPower = configType.getPenetratingPower();
        }
        catch (Exception e)
        {
            discard();
            FlansMod.log.warn("Failed to read bullet spawn data", e);
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);

        FireableGun gun = firedShot.getFireableGun();

        if (gun != null)
        {
            CompoundTag gunTag = new CompoundTag();

            gunTag.putString(NBT_FIREABLE_GUN_TYPE_NAME, gun.getType().getShortName());
            gunTag.putFloat(NBT_FIREABLE_GUN_SPREAD, gun.getSpread());
            gunTag.putFloat(NBT_FIREABLE_GUN_SPEED, gun.getBulletSpeed());
            gunTag.putFloat(NBT_FIREABLE_GUN_DAMAGE, gun.getDamage());
            gunTag.putString(NBT_FIREABLE_GUN_SPREAD_PATTERN, gun.getSpreadPattern().name());
            tag.put(NBT_FIREABLE_GUN, gunTag);

            firedShot.getAttacker().ifPresent(livingEntity -> tag.putUUID(NBT_ATTACKER, livingEntity.getUUID()));
            firedShot.getCausingEntity().ifPresent(entity -> tag.putUUID(NBT_SHOOTER, entity.getUUID()));
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        InfoType infoType = InfoType.getInfoType(shortname);
        FireableGun fireableGun = null;

        if (infoType instanceof BulletType bType)
        {
            configType = bType;

            if (tag.contains(NBT_FIREABLE_GUN, Tag.TAG_COMPOUND))
            {
                CompoundTag gun = tag.getCompound(NBT_FIREABLE_GUN);
                float damage = gun.getFloat(NBT_FIREABLE_GUN_DAMAGE);
                float spread = gun.getFloat(NBT_FIREABLE_GUN_SPREAD);
                float speed = gun.getFloat(NBT_FIREABLE_GUN_SPEED);
                EnumSpreadPattern spreadPattern = EnumSpreadPattern.valueOf(gun.getString(NBT_FIREABLE_GUN_SPREAD_PATTERN));

                InfoType fireableGunInfoType = InfoType.getInfoType(gun.getString(NBT_FIREABLE_GUN_TYPE_NAME));
                if (fireableGunInfoType != null)
                    fireableGun = new FireableGun(fireableGunInfoType, damage, spread, speed, spreadPattern);
            }

            if (tag.hasUUID(NBT_ATTACKER))
            {
                attackerUUID = tag.getUUID(NBT_ATTACKER);
                checkForUUIDs = true;
            }
            if (tag.hasUUID(NBT_SHOOTER))
            {
                shooterUUID = tag.getUUID(NBT_SHOOTER);
                checkForUUIDs = true;
            }

            firedShot = new FiredShot(fireableGun, configType, null, null);
        }
        else
        {
            discard();
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        Level level = level();
        try
        {
            resolveUUIDs(level);
            setInitialSpeed();
            updatePreviousPosition();

            if (shouldDespawn())
            {
                detonated = true;
                discard();
                return;
            }

            updatePingOfShooter(level);
            handleSubmunitionsTimers();
            handleVLSTimer();
            initLookAndOrigin();
            decrementSoundTime();

            if (handleMaxRange(level))
                return;
            if (handleFuseAndLifetime(level))
                return;

            handleDetonationConditions(level);
            handleSubmunitions(level);

            DebugHelper.spawnDebugVector(level, position(), velocity, 1000);

            performRaytraceAndApplyHits(level);
            applyDragAndGravity();
            updatePenetrationPower();
            applyHomingIfLocked(level);
            updateShootForSettingPos();
            updateManualGuidance(level);
            updateTorpedoVelocity();
            updatePositionAndOrientation();

            if (level.isClientSide)
                clientTick((ClientLevel) level);
        }
        catch (Exception ex)
        {
            FlansMod.log.error("Error ticking bullet {}", shortname, ex);
            discard();
        }
    }

    protected void setInitialSpeed()
    {
        if (initialTick)
        {
            initialSpeed = velocity.length();
            initialTick = false;
        }
    }

    protected void updatePreviousPosition()
    {
        xo = getX();
        yo = getY();
        zo = getZ();
    }

    protected void resolveUUIDs(Level level)
    {
        if (level.isClientSide || !checkForUUIDs)
            return;

        ServerLevel serverLevel = (ServerLevel) level;
        Entity shooter = null;
        LivingEntity attacker = null;

        if (shooterUUID != null)
            shooter = serverLevel.getEntity(shooterUUID); // null if not loaded
        if (attackerUUID != null && serverLevel.getEntity(attackerUUID) instanceof LivingEntity living)
            attacker = living;

        firedShot.setShooter(shooter);
        firedShot.setAttacker(attacker);
        checkForUUIDs = false;
    }

    protected void updatePingOfShooter(Level level)
    {
        if (!level.isClientSide)
            firedShot.getPlayerAttacker().ifPresent(player -> pingOfShooter = player.latency);
    }

    protected void handleSubmunitionsTimers()
    {
        if (!hasSetSubDelay && configType.isHasSubmunitions())
        {
            submunitionDelay = configType.getSubMunitionTimer();
            hasSetSubDelay = true;
        }
        else if (configType.isHasSubmunitions())
        {
            submunitionDelay--;
        }
    }

    protected void handleVLSTimer()
    {
        if (!hasSetVlsDelay && configType.isVls())
        {
            vlsDelay = configType.getVlsTime();
            hasSetVlsDelay = true;
        }

        if (vlsDelay > 0)
            vlsDelay--;
    }

    protected void initLookAndOrigin()
    {
        LivingEntity owner = firedShot.getAttacker().orElse(null);
        if (!hasSetLook && owner != null)
        {
            lookVector = owner.getLookAngle();
            initialPos = owner.position();
            hasSetLook = true;
        }
    }

    protected void decrementSoundTime()
    {
        if (soundTime > 0)
            soundTime--;
    }

    /** returns true if bullet was discarded */
    protected boolean handleMaxRange(Level level)
    {
        LivingEntity owner = firedShot.getAttacker().orElse(null);
        if (owner == null)
            return false;

        double rangeX = owner.getX() - getX();
        double rangeY = owner.getY() - getY();
        double rangeZ = owner.getZ() - getZ();
        double range = Math.sqrt(rangeX * rangeX + rangeY * rangeY + rangeZ * rangeZ);

        if (configType.getMaxRange() != -1 && configType.getMaxRange() < range)
        {
            // Optional fuse detonation on max range
            if (tickCount > configType.getFuse() && configType.getFuse() > 0)
                detonate(level);
            else
                setDead(level);
        }

        return isRemoved();
    }

    /** returns true if bullet was discarded */
    protected boolean handleFuseAndLifetime(Level level)
    {
        ticksInAir++;

        if (ticksInAir > configType.getFuse() && configType.getFuse() > 0 && !isRemoved())
        {
            setDead(level);
        }
        if (tickCount > bulletLife)
        {
            setDead(level);
        }

        return isRemoved();
    }

    @Override
    protected boolean handleEntityInProximityTriggerRange(Level level, Entity entity) {
        if (getConfigType().getDamageToTriggerer() > 0F)
            entity.hurt(firedShot.getDamageSource(level, this), getConfigType().getDamageToTriggerer());

        return true;
    }

    @Override
    public boolean isShooterEntity(Entity entity)
    {
        return entity == firedShot.getAttacker().orElse(null) || entity == firedShot.getCausingEntity().orElse(null);
    }

    @Override
    public Optional<LivingEntity> getOwner()
    {
        return firedShot.getAttacker();
    }

    protected void handleSubmunitions(Level level)
    {
        if (configType.isHasSubmunitions() && submunitionDelay < 0)
        {
            deploySubmunitions(level);
            submunitionDelay = 9001;
        }
    }

    public void deploySubmunitions(Level level)
    {
        if (StringUtils.isBlank(configType.getSubmunition()))
            return;

        ShootableType submunitionType = ShootableType.getAmmoType(configType.getSubmunition(), configType.getContentPack()).orElse(null);
        if (submunitionType == null)
            return;

        Shootable shootable = ShootableFactory.createShootable(level, firedShot, submunitionType, position(), velocity.normalize());

        for (int sm = 0; sm < configType.getNumSubmunitions(); sm++)
        {
            level.addFreshEntity(shootable);
        }

        if (configType.isDestroyOnDeploySubmunition())
            detonate(level);
    }

    protected void performRaytraceAndApplyHits(Level level)
    {
        //TODO: allow client interpolation?
        if (level.isClientSide)
            return;

        Vec3 origin = position();
        List<BulletHit> hits = FlansModRaytracer.raytraceShot(level, this, firedShot.getAttacker().orElse(null), ticksInAir > 20 ? firedShot.getOwnerEntities() : Collections.emptyList(), origin, velocity, pingOfShooter, 0F, getHitboxSize(), configType);

        if (hits.isEmpty())
            return;

        lastHitPenAmount = 0F;
        lastHitHeadshot = false;

        for (BulletHit bulletHit : hits)
        {
            BulletHitEvent bulletHitEvent = new BulletHitEvent(this, bulletHit);
            MinecraftForge.EVENT_BUS.post(bulletHitEvent);
            if (bulletHitEvent.isCanceled())
                continue;

            Vec3 hitPos = origin.add(velocity.scale(bulletHit.getIntersectTime()));

            ShootingHelper.HitData hitData = ShootingHelper.onHit(level, firedShot, bulletHit, hitPos, velocity, new ShootingHelper.HitData(penetratingPower, lastHitPenAmount, lastHitHeadshot), this);
            penetratingPower = hitData.penetratingPower();
            lastHitPenAmount = hitData.lastHitPenAmount();
            lastHitHeadshot = hitData.lastHitHeadshot();

            if (penetratingPower <= 0F || (configType.isExplodeOnImpact() && ticksInAir > 1))
            {
                setPos(hitPos);
                setDead(level);
                break;
            }
        }
    }

    public void handleBounceOrStop(Level level, BlockHitResult hitResult, Vec3 hitVec)
    {
        if (configType.getBounciness() <= 0F)
        {
            // No bounce: stop at impact point
            setPos(hitVec);
            setDead(level);
            return;
        }

        Vec3 preHitVel = hitVec.subtract(position());
        Vec3 postHitVel = velocity.subtract(preHitVel);

        Vec3 surfaceNormal = getSurfaceNormal(hitResult.getDirection());

        if (velocity.lengthSqr() < 0.1F * initialSpeed)
        {
            setPos(hitVec);
            setDead(level);
            return;
        }

        double lambda = postHitVel.length() / velocity.length();

        double normalProjection = surfaceNormal.dot(postHitVel);
        // normal component (scaled down)
        Vec3 normal = surfaceNormal.scale(-normalProjection);

        // tangential component
        Vec3 orthog = postHitVel.add(normal);

        normal = normal.scale(configType.getBounciness() / 3.0F);
        orthog = orthog.scale(configType.getBounciness());

        postHitVel = orthog.add(normal);
        Vec3 totalVel = preHitVel.add(postHitVel);

        setPos(position().add(totalVel));
        setDeltaMovement(postHitVel.scale(1.0D / lambda));
    }

    protected static Vec3 getSurfaceNormal(Direction side)
    {
        return switch (side) {
            case DOWN -> new Vec3(0, -1, 0);
            case UP -> new Vec3(0, 1, 0);
            case NORTH -> new Vec3(0, 0, -1);
            case SOUTH -> new Vec3(0, 0, 1);
            case EAST -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(-1, 0, 0);
        };
    }

    protected void applyDragAndGravity()
    {
        if (configType.isTorpedo())
            return;

        double gravity = ShootableType.FALL_SPEED_COEFFICIENT * configType.getFallSpeed();
        float drag = configType.getDragInAir();

        if (isInWater())
            drag = configType.getDragInWater();
        else if (isInLava())
            drag = ShootableType.LAVA_DEFAULT_DRAG;

        velocity = velocity.scale(drag).add(0, -gravity, 0);
        setDeltaMovement(velocity);
    }

    protected void updateTorpedoVelocity()
    {
        if (!configType.isTorpedo())
            return;

        if (isInWater())
        {
            Vec3 direction = velocity.normalize();
            velocity = new Vec3(direction.x, velocity.y * 0.3, direction.z);
        }
        else
        {
            double gravity = ShootableType.FALL_SPEED_COEFFICIENT * configType.getFallSpeed();
            velocity = velocity.add(0, -gravity, 0);
        }
        setDeltaMovement(velocity);
    }

    protected void updatePositionAndOrientation()
    {
        // move by current velocity
        setPos(position().add(velocity));

        // recompute target angles from motion
        float horiz = (float) Math.hypot(velocity.x, velocity.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(velocity.x, velocity.z)));
        float targetPitch = (float) (Math.toDegrees(Math.atan2(velocity.y, horiz)));

        // smooth like old: prev + (curr - prev) * 0.2, but with proper wrap
        setYRot(Mth.rotLerp(0.2F, yRotO, targetYaw));
        setXRot(Mth.rotLerp(0.2F, xRotO, targetPitch));

        // update "previous" fields for next tick interpolation
        yRotO = getYRot();
        xRotO = getXRot();
    }

    protected void updatePenetrationPower()
    {
        // Damp penetration too
        float prevPenetratingPower = penetratingPower;
        penetratingPower *= (1 - configType.getPenetrationDecay());

        if (configType.getPenetrationDecay() > 0F)
            penetrationLosses.add(new PenetrationLoss((prevPenetratingPower - penetratingPower), PenetrationLoss.Type.DECAY));
    }

    protected void applyHomingIfLocked(Level level)
    {
        if (isRemoved())
            return;

        LivingEntity owner = firedShot.getAttacker().orElse(null);

        // No lock → try laser guidance
        if (lockedOnTo == null)
        {
            if (configType.isLaserGuidance() && owner != null)
            {
                BlockHitResult hit = FlansModRaytracer.getSpottedPoint(owner, 1.0F, configType.getMaxRangeOfMissile(), false);
                if (hit.getType() != HitResult.Type.MISS)
                    applyLaserGuidance(Vec3.atCenterOf(hit.getBlockPos()));
            }
            return;
        }

        // Target present: sound & tracking
        if (lockedOnTo instanceof Driveable driveable)
        {
            String lockedOnSound = driveable.getConfigType().getLockedOnSound();
            if (StringUtils.isNotBlank(lockedOnSound) && soundTime <= 0 && !level.isClientSide)
            {
                PacketPlaySound.sendSoundPacket(lockedOnTo.getX(), lockedOnTo.getY(), lockedOnTo.getZ(), driveable.getConfigType().getLockedOnSoundRange(), level.dimension(), lockedOnSound ,false);
                soundTime = driveable.getConfigType().getSoundTime();
            }
        }

        if (tickCount > configType.getTickStartHoming())
        {
            // Geometry: missile → target vector
            double dX = lockedOnTo.getX() - getX();
            double dZ = lockedOnTo.getZ() - getZ();
            double dY;
            double dXYZ;

            if (configType.isDoTopAttack() && Math.abs(lockedOnTo.getX() - getX()) > 2.0 && Math.abs(lockedOnTo.getZ() - getZ()) > 2.0)
                dY = lockedOnTo.getY() + 30.0 - getY();
            else
                dY = lockedOnTo.getY() - getY();

            if (!configType.isDoTopAttack())
                dXYZ = distanceTo(lockedOnTo);
            else
                dXYZ = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

            // SACLOS: target must stay in crosshair
            if (owner != null && configType.isEnableSACLOS())
            {
                double dXp = lockedOnTo.getX() - owner.getX();
                double dYp = lockedOnTo.getY() - owner.getY();
                double dZp = lockedOnTo.getZ() - owner.getZ();

                Vec3 playerLook = owner.getLookAngle();
                Vec3 playerToTarget = new Vec3(dXp, dYp, dZp);

                double angleSaclos = Math.abs(Vector3f.angle(new Vector3f(playerLook), new Vector3f(playerToTarget)));
                if (angleSaclos > Math.toRadians(configType.getMaxDegreeOfSACLOS()))
                    lockedOnTo = null;
            }

            // Range / toggle lock logic
            if (toggleLock)
            {
                toggleLock = false;
                if (dXYZ > configType.getMaxRangeOfMissile())
                    lockedOnTo = null;
            }

            updateVelocityForHoming(velocity, dX, dY, dZ, dXYZ);

            // "Close count" logic – if distance grows too long, drop lock
            if (tickCount > 4 && dXYZ > prevDistanceToEntity)
            {
                closeCount++;
                if (closeCount > 15)
                    lockedOnTo = null;
            }
            else if (closeCount > 0)
                closeCount--;

            prevDistanceToEntity = dXYZ;
        }

        // Flare / countermeasure check
        if (lockedOnTo instanceof Driveable driveable && (driveable.isVarFlare() || driveable.getTicksFlareUsing() > 0))
        {
            lockedOnTo = null;
        }
    }

    protected void applyLaserGuidance(Vec3 targetPos)
    {
        // Wait until homing is allowed
        if (tickCount <= configType.getTickStartHoming())
            return;

        // Vector from missile to target
        double dX = targetPos.x - getX();
        double dY = targetPos.y - getY();
        double dZ = targetPos.z - getZ();
        double dXYZ = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        if (toggleLock)
            toggleLock = false;

        updateVelocityForHoming(velocity, dX, dY, dZ, dXYZ);

        // If we’re getting farther away for too long, “drop” the guidance
        if (tickCount > 4 && dXYZ > prevDistanceToTarget)
            closeCount++;
        else if (closeCount > 0)
            closeCount--;

        prevDistanceToTarget = dXYZ;
    }

    private void updateVelocityForHoming(Vec3 velocity, double dX, double dY, double dZ, double dXYZ)
    {
        double speed = velocity.length();
        if (speed < 1.0e-6 || dXYZ < 1.0e-6)
            return; // no meaningful motion

        Vec3 desiredMotion = new Vec3(dX * speed / dXYZ, dY * speed / dXYZ, dZ * speed / dXYZ);
        Vec3 desiredDirection = desiredMotion.normalize();
        Vec3 currentDirection = velocity.normalize();

        // Angle between current and desired directions (radians)
        float angle = Math.abs(Vector3f.angle(new Vector3f(currentDirection), new Vector3f(desiredDirection)));

        // FOV / seeker cone: if target too far off boresight, lose lock
        if (angle > Math.toRadians(configType.getMaxDegreeOfMissile()))
        {
            lockedOnTo = null;
            return;
        }

        // Turn rate per tick scaled by lockOnForce (1 is like 10G)
        double maxTurnThisTick = Math.toRadians(configType.getLockOnForce() * 28.10 / 60);

        // If angle is tiny or within our turn budget, snap to desired direction
        if (angle < 1.0e-3F || angle <= maxTurnThisTick)
        {
            setDeltaMovement(desiredMotion);
            return;
        }

        // Rotate partially towards desiredDir by fraction t = maxTurn / angle
        double t = Math.max(0.0, Math.min(1.0, maxTurnThisTick / angle));
        Vec3 newDir = currentDirection.scale(1.0 - t).add(desiredDirection.scale(t)).normalize();
        Vec3 newVelocity = newDir.scale(speed);
        setDeltaMovement(newVelocity);
    }

    protected void updateShootForSettingPos()
    {
        LivingEntity owner = firedShot.getAttacker().orElse(null);

        if (owner != null && configType.isShootForSettingPos() && !isFirstPositionSetting)
        {
            if (owner instanceof Player player)
            {
                ItemStack stack = player.getMainHandItem();

                if (!stack.isEmpty() && stack.getItem() instanceof GunItem itemGun)
                {
                    impactX = itemGun.getImpactX();
                    impactY = itemGun.getImpactY();
                    impactZ = itemGun.getImpactZ();
                }
            }
            isFirstPositionSetting = true;
        }

        // Phase 1: go straight up until we’re high enough
        if (configType.isShootForSettingPos() && isFirstPositionSetting && isPositionUpper)
        {
            // straight up with same speed
            setDeltaMovement(0.0, velocity.length(), 0.0);

            if (owner != null && getY() - configType.getShootForSettingPosHeight() > owner.getY())
                isPositionUpper = false;
        }

        // Phase 2: travel horizontally toward impactX/Z, then start diving down
        if (configType.isShootForSettingPos() && isFirstPositionSetting && !isPositionUpper)
        {
            double rootx = impactX - getX();
            double rootz = impactZ - getZ();
            double roota = Math.sqrt(rootx * rootx + rootz * rootz);

            double ySpeed = velocity.y;
            double speed = velocity.length();

            if (roota != 0.0)
            {
                double newMotionX = rootx * speed / roota;
                double newMotionZ = rootz * speed / roota;

                // keep current vertical speed, rotate horizontal component
                setDeltaMovement(newMotionX, ySpeed, newMotionZ);
            }

            // close enough to the target X/Z → turn straight down
            if (Math.abs(impactX - getX()) < 1.0 && Math.abs(impactZ - getZ()) < 1.0)
            {
                setDeltaMovement(0.0, -speed, 0.0);
            }
        }
    }

    protected void updateManualGuidance(Level level)
    {
        LivingEntity owner = firedShot.getAttacker().orElse(null);

        if (owner != null && configType.isManualGuidance() && vlsDelay <= 0 && lockedOnTo == null)
        {
            // CLIENT: send updated guidance to the server when player moves / looks ---
            if (level.isClientSide && owner == Minecraft.getInstance().player)
            {
                Vec3 tempPos = owner.position();
                Vec3 look = owner.getLookAngle();

                double deltaPos = (ownerPos == null) ? 1000.0F : tempPos.subtract(ownerPos).lengthSqr();
                double deltaLook = (ownerLook == null) ? 1000.0F : look.subtract(ownerLook).lengthSqr();

                ownerPos = tempPos;
                ownerLook = look;

                if (deltaPos > 1.0 || deltaLook > 0.0001)
                    PacketHandler.sendToServer(new PacketManualGuidance(getId(), (float) ownerPos.x, (float) ownerPos.y, (float) ownerPos.z, (float) ownerLook.x, (float) ownerLook.y, (float) ownerLook.z));
            }

            // SERVER + CLIENT: apply guidance to the missile's motion

            // Use last known look / pos from packet, or fall back to the current owner state
            Vec3 lookVec = (ownerLook != null) ? ownerLook : owner.getLookAngle();
            Vec3 origin = (ownerPos != null) ? ownerPos : owner.position();

            if (configType.isFixedDirection())
            {
                lookVec = lookVector;
                origin = initialPos;
            }

            float x = (float) (getX() - origin.x);
            float y = (float) (getY() - origin.y);
            float z = (float) (getZ() - origin.z);

            float d = (float) Math.sqrt(x * x + y * y + z * z);
            d += configType.getTurnRadius();

            lookVec = lookVec.normalize();

            Vec3 targetPoint = origin.add(lookVec.scale(d));
            Vec3 diff = targetPoint.subtract(position());

            float speed2 = configType.getTrackPhaseSpeed();
            float turnSpeed = configType.getTrackPhaseTurn();

            diff = diff.normalize();

            Vec3 targetSpeed = diff.scale(speed2);

            velocity = velocity.add(targetSpeed.subtract(velocity).scale(turnSpeed));

            setDeltaMovement(velocity);
        }
    }

    /** detonate() also discards bullet entities */
    @Override
    public void detonate(Level level)
    {
        if (level.isClientSide || detonated || isRemoved() || tickCount < configType.getPrimeDelay())
            return;

        detonate(level, firedShot.getAttacker().orElse(null));
    }

    public void setDead(Level level)
    {
        if (isRemoved())
            return;

        ShootingHelper.onBulletDeath(level, configType, position(), this, Optional.ofNullable(firedShot).flatMap(FiredShot::getAttacker).orElse(null));
        discard();
    }

    @OnlyIn(Dist.CLIENT)
    protected void clientTick(ClientLevel level)
    {
        playFlybyIfClose(level);
        spawnWaterBubbles(level);
        spawnParticles(level);
        clearFire();
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnWaterBubbles(ClientLevel level)
    {
        if (!isInWater())
            return;

        for (int i = 0; i < 4; i++)
        {
            double bubbleMotion = 0.25;
            level.addParticle(ParticleTypes.BUBBLE, getX() - velocity.x * bubbleMotion, getY() - velocity.y * bubbleMotion, getZ() - velocity.z * bubbleMotion, velocity.x, velocity.y + 0.1F, velocity.z);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnParticles(ClientLevel level)
    {
        if (!configType.isTrailParticles() || ticksInAir <= 1)
            return;

        double dX = (getX() - xo) / 10.0;
        double dY = (getY() - yo) / 10.0;
        double dZ = (getZ() - zo) / 10.0;

        float spread = 0.1F;

        if (vlsDelay > 0 && StringUtils.isNotBlank(configType.getBoostPhaseParticle()))
        {
            for (int i = 0; i < 10; i++)
            {
                double x = xo + dX * i + random.nextGaussian() * spread;
                double y = yo + dY * i + random.nextGaussian() * spread;
                double z = zo + dZ * i + random.nextGaussian() * spread;
                ParticleHelper.spawnFromString(configType.getBoostPhaseParticle(), x, y, z, 0, 0, 0, 1F);
            }
        }
        else if (!configType.isVls() || vlsDelay <= 0)
        {
            for (int i = 0; i < 10; i++)
            {
                double x = xo + dX * i + random.nextGaussian() * spread;
                double y = yo + dY * i + random.nextGaussian() * spread;
                double z = zo + dZ * i + random.nextGaussian() * spread;
                ParticleHelper.spawnFromString(configType.getTrailParticleType(), x, y, z, 0, 0, 0, 1F);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void playFlybyIfClose(ClientLevel level)
    {
        Minecraft mc = Minecraft.getInstance();
        if (playedFlybySound || mc.player == null)
            return;
        if (distanceToSqr(mc.player) >= 25.0) // within 5 blocks
            return;
        if (mc.player == firedShot.getAttacker().orElse(null))
            return;

        playedFlybySound = true;
        float soundVolume = 10.0F;
        float soundPitch = 1.0F / (random.nextFloat() * 0.4F + 0.8F);
        FlansMod.getSoundEvent(FlansMod.SOUND_BULLETFLYBY).ifPresent(soundEvent ->
                level.playLocalSound(getX(), getY(), getZ(), soundEvent.get(), SoundSource.HOSTILE, soundVolume, soundPitch, false));
    }
}
