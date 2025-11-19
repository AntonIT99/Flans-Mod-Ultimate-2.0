package com.flansmodultimate.common.entity;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.render.ParticleHelper;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.BulletHit;
import com.flansmodultimate.common.raytracing.FlansModRaytracer;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.event.BulletLockOnEvent;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
    protected Entity lockedOnTo; // For homing missiles
    protected int bulletLife = 600; // Kill bullets after 30 seconds
    protected int ticksInAir;
    protected boolean initialTick = true;
    protected double initialSpeed;
    protected float currentPenetratingPower;
    /** If this is non-zero, then the player raytrace code will look back in time to when the player thinks their bullet should have hit */
    protected int pingOfShooter;

    protected int submunitionDelay = 20;
    protected boolean hasSetSubDelay;
    protected boolean hasSetVlsDelay;
    protected int vlsDelay;
    protected int soundTime;
    protected Vec3 lookVector;
    protected Vec3 initialPos;
    protected boolean hasSetLook;

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
        ticksInAir = 0;
        configType = firedShot.getBulletType();
        setPos(origin);
        setArrowHeading(direction, firedShot.getFireableGun().getSpread() * firedShot.getBulletType().getBulletSpread(), firedShot.getFireableGun().getBulletSpeed());
        currentPenetratingPower = firedShot.getBulletType().getPenetratingPower();
    }

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
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        try
        {
            super.readSpawnData(buf);
            int shooterId = buf.readInt();
            int attackerId = buf.readInt();

            setOrientation(velocity);

            if (InfoType.getInfoType(shortname) instanceof BulletType type)
                configType = type;
            if (configType == null)
            {
                FlansMod.log.warn("Unknown bullet type {}, discarding.", shortname);
                discard();
            }

            Level level = level();
            Entity shooter = null;
            LivingEntity attacker = null;

            if (shooterId != 0)
                shooter = level.getEntity(attackerId);
            if (attackerId != 0 && level.getEntity(attackerId) instanceof LivingEntity living)
                attacker = living;

            firedShot = new FiredShot(null, configType, shooter, attacker);
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

            if (shouldDespawn(configType))
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

            //TODO: add FMU logic (WIP) - continue here
            handleDetonationConditions(level, configType);
            handleSubmunitions(level);

            DebugHelper.spawnDebugVector(level, position(), velocity, 1000);

            performRaytraceAndApplyHits(level);
            applyDragAndGravity();
            updatePositionAndOrientation();
            applyHomingIfLocked();
            updateTorpedoVelocity();

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
    private boolean handleMaxRange(Level level)
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
        if (level.isClientSide)
            return;

        Vec3 origin = position();
        Optional<ServerPlayer> playerOptional = firedShot.getPlayerAttacker();
        Entity ignore = playerOptional.isPresent() ? playerOptional.get() : firedShot.getCausingEntity().orElse(null);

        int pingMs = firedShot.getPlayerAttacker().map(p -> p.latency).orElse(0);

        List<BulletHit> hits = FlansModRaytracer.raytrace(level, ignore, ticksInAir > 20, this, new Vector3f(origin), new Vector3f(velocity), pingMs, 0f, getHitboxSize());

        if (hits.isEmpty())
            return;

        for (BulletHit bulletHit : hits)
        {
            Vec3 hitPos = origin.add(velocity.scale(bulletHit.intersectTime));

            currentPenetratingPower = ShootingHelper.onHit(level, firedShot, bulletHit, hitPos, velocity, currentPenetratingPower, this);
            if (currentPenetratingPower <= 0F)
            {
                setPos(hitPos);
                detonate(level);
                break;
            }
        }
    }

    protected void applyDragAndGravity()
    {
        if (configType.isTorpedo())
            return;

        float drag = isInWater() ? configType.getDragInWater() : configType.getDragInAir();
        double gravity = 0.02 * configType.getFallSpeed();
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
            double gravity = 0.02 * configType.getFallSpeed();
            velocity = velocity.add(0, -gravity, 0);
        }
        setDeltaMovement(velocity);
    }

    protected void updatePositionAndOrientation()
    {
        // move by current velocity
        setPos(position().add(velocity));

        // recompute target angles from motion (same math as 1.7.10)
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

    protected void applyHomingIfLocked()
    {
        if (lockedOnTo == null || isRemoved())
            return;

        double dX = lockedOnTo.getX() - getX();
        double dY = lockedOnTo.getY() - getY();
        double dZ = lockedOnTo.getZ() - getZ();
        double d2 = dX * dX + dY * dY + dZ * dZ;
        if (d2 < 1.0e-6)
            return;

        Vector3f motion = new Vector3f((float)velocity.x, (float)velocity.y, (float)velocity.z);
        Vector3f toTarget = new Vector3f((float)dX, (float)dY, (float)dZ);

        float angle = Math.abs(Vector3f.angle(motion, toTarget));
        double pull = angle * configType.getLockOnForce();
        pull = pull * pull;

        velocity = velocity.scale(0.95).add(pull * dX / d2, pull * dY / d2, pull * dZ / d2);
        setDeltaMovement(velocity);
    }

    /** detonate() also discards bullet entities */
    @Override
    public void detonate(Level level)
    {
        if (level.isClientSide || detonated || isRemoved() || tickCount < configType.getPrimeDelay())
            return;

        detonate(level, configType, firedShot.getAttacker().orElse(null));
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
        spawnWaterBubbles(level);
        spawnTrailParticles(level);
        playFlybyIfClose(level);
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnWaterBubbles(ClientLevel level)
    {
        if (!isInWater())
            return;

        for (int i = 0; i < 4; i++)
        {
            double t = 0.25;
            level.addParticle(ParticleTypes.BUBBLE, getX() - velocity.x * t, getY() - velocity.y * t, getZ() - velocity.z * t, velocity.x, velocity.y, velocity.z);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnTrailParticles(ClientLevel level)
    {
        if (!configType.isTrailParticles() || ticksInAir <= 1)
            return;

        double dX = (getX() - xo) / 10.0;
        double dY = (getY() - yo) / 10.0;
        double dZ = (getZ() - zo) / 10.0;

        float spread = 0.1F;

        for (int i = 0; i < 10; i++)
        {
            double x = xo + dX * i + random.nextGaussian() * spread;
            double y = yo + dY * i + random.nextGaussian() * spread;
            double z = zo + dZ * i + random.nextGaussian() * spread;

            ParticleHelper.spawnFromString(level, configType.getTrailParticleType(), x, y, z);
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

        playedFlybySound = true;
        float soundVolume = 10.0F;
        float soundPitch = 1.0F / (random.nextFloat() * 0.4F + 0.8F);
        FlansMod.getSoundEvent(FlansMod.SOUND_BULLETFLYBY).ifPresent(soundEvent ->
                level.playLocalSound(getX(), getY(), getZ(), soundEvent.get(), SoundSource.HOSTILE, soundVolume, soundPitch, false));
    }
}
