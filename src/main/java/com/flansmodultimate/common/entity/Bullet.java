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
import com.flansmodultimate.network.PacketFlak;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.StringUtils;

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

    protected BulletType configType;
    protected FiredShot firedShot;
    protected Entity lockedOnTo; // For homing missiles
    protected int bulletLife = 600; // Kill bullets after 30 seconds
    protected int ticksInAir;
    protected boolean initialTick = true;
    protected double initialSpeed;
    protected float currentPenetratingPower;
    /** If this is non-zero, then the player raytrace code will look back in time to when the player thinks their bullet should have hit */
    protected int pingOfShooter = 0;

    protected int submunitionDelay = 20;
    protected boolean hasSetSubDelay = false;
    protected boolean hasSetVlsDelay = false;
    protected int vlsDelay = 0;
    protected int soundTime = 0;
    protected Vec3 lookVector;
    protected Vec3 initialPos;
    protected boolean hasSetLook = false;

    /** These values are used to store the UUIDs until the next entity update is performed. This prevents issues caused by the loading order */
    protected boolean checkforuuids;
    protected UUID playeruuid;
    protected UUID shooteruuid;

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
    public void readSpawnData(FriendlyByteBuf buf)
    {
        try
        {
            super.readSpawnData(buf);
            setOrientation(velocity);
            if (InfoType.getInfoType(shortname) instanceof BulletType type)
                configType = type;
            if (configType == null)
            {
                FlansMod.log.warn("Unknown bullet type {}, discarding.", shortname);
                discard();
            }
            if (firedShot == null)
                firedShot = new FiredShot((FireableGun) null, configType, null, null);
        }
        catch (Exception e)
        {
            discard();
            FlansMod.log.warn("Failed to read bullet spawn data", e);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.putString("type", shortname);

        FireableGun gun = firedShot.getFireableGun();

        if (gun != null)
        {
            CompoundTag gunTag = new CompoundTag();

            gunTag.putString("infotype", gun.getType().getShortName());
            gunTag.putFloat("spread", gun.getSpread());
            gunTag.putFloat("speed", gun.getBulletSpeed());
            gunTag.putFloat("damage", gun.getDamage());
            gunTag.putFloat("vehicledamage", gun.getDamageAgainstVehicles());
            tag.put("fireablegun", gunTag);

            firedShot.getPlayerAttacker().ifPresent((ServerPlayer player) -> tag.putUUID("player", player.getUUID()));
            firedShot.getCausingEntity().ifPresent((Entity shooter) -> tag.putUUID("shooter", shooter.getUUID()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        FireableGun fireablegun = null;
        setShortName(tag.getString("type"));
        InfoType infoType = InfoType.getInfoType(shortname);

        if (infoType instanceof BulletType bType)
        {
            configType = bType;

            if (tag.contains("fireablegun", Tag.TAG_COMPOUND))
            {
                CompoundTag gun = tag.getCompound("fireablegun");
                float damage = gun.getFloat("damage");
                float vDamage = gun.getFloat("vehicledamage");
                float spread = gun.getFloat("spread");
                float speed = gun.getFloat("speed");
                InfoType fireablegunInfoType = InfoType.getInfoType(gun.getString("infotype"));

                if (fireablegunInfoType != null)
                    fireablegun = new FireableGun(fireablegunInfoType, damage, vDamage, spread, speed, EnumSpreadPattern.CIRCLE);
            }

            if (tag.hasUUID("player"))
            {
                playeruuid = tag.getUUID("player");
                checkforuuids = true;
            }
            if (tag.hasUUID("shooter"))
            {
                shooteruuid = tag.getUUID("shooter");
                checkforuuids = true;
            }

            firedShot = new FiredShot(fireablegun, configType, null, null);
        }
        else
        {
            discard();
        }
    }

    @Override
    public void tick()
    {
        //TODO: add FMU logic (WIP)
        super.tick();
        Level level = level();
        try
        {
            if (initialTick)
            {
                initialSpeed = velocity.length();
                initialTick = false;
            }
            if (!level.isClientSide && checkforuuids)
            {
                resolvePendingUUIDs((ServerLevel) level);
                checkforuuids = false;
            }
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

            if (handleFuseAndLifetime(level))
                return;

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

    protected void resolvePendingUUIDs(ServerLevel level)
    {
        ServerPlayer player = null;
        Entity shooter = null;

        if (playeruuid != null)
        {
            player = (ServerPlayer) level.getPlayerByUUID(playeruuid);
            playeruuid = null;
        }

        if (shooteruuid != null)
        {
            if (player != null && shooteruuid.equals(player.getUUID()))
                shooter = player;
            else
                shooter = level.getEntity(shooteruuid); // null if not loaded
            shooteruuid = null;
        }

        if (player != null)
            firedShot = new FiredShot(firedShot.getFireableGun(), configType, shooter, player);
        else
            firedShot = new FiredShot(firedShot.getFireableGun(), configType, shooter, (shooter instanceof LivingEntity living) ? living : null);
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

        FiredShot submunitionShot = null;
        if (submunitionType instanceof BulletType bulletType)
            submunitionShot = new FiredShot(firedShot.getFireableGun(), bulletType, firedShot.getCausingEntity().orElse(null), firedShot.getAttacker().orElse(null));
        Shootable shootable = ShootableFactory.createShootable(level, submunitionType, position(), velocity.normalize(), submunitionShot);


        for (int sm = 0; sm < configType.getNumSubmunitions(); sm++)
        {
            level.addFreshEntity(shootable);
        }

        if (configType.isDestroyOnDeploySubmunition())
            detonate(level);
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

    @Override
    protected boolean isShooterEntity(Entity entity)
    {
        return entity == firedShot.getAttacker().orElse(null) || entity == firedShot.getCausingEntity().orElse(null);
    }

    protected boolean handleEntityInProximityTriggerRange(Level level, Entity entity) {
        if (getConfigType().getDamageToTriggerer() > 0F)
            entity.hurt(firedShot.getDamageSource(level, this), getConfigType().getDamageToTriggerer());

        detonate(level);
        return true;
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

    protected void updatePositionAndOrientation() {
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

    @OnlyIn(Dist.CLIENT)
    protected void clientTick(ClientLevel level)
    {
        if (isInWater())
            spawnWaterBubbles(level);
        if (configType.isTrailParticles())
            spawnParticles(level);
        playFlybyIfClose(level);
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnWaterBubbles(ClientLevel level)
    {
        for (int i = 0; i < 4; i++)
        {
            double t = 0.25;
            level.addParticle(ParticleTypes.BUBBLE, getX() - velocity.x * t, getY() - velocity.y * t, getZ() - velocity.z * t, velocity.x, velocity.y, velocity.z);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnParticles(ClientLevel level)
    {
        // segment between previous and current position
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

    protected boolean handleFuseAndLifetime(Level level)
    {
        ticksInAir++;

        if (configType.getFuse() > 0 && ticksInAir > configType.getFuse() && !isRemoved())
        {
            detonate(level);
            discard();
            return true;
        }
        if (tickCount > bulletLife)
        {
            detonate(level);
            discard();
            return true;
        }
        return isRemoved();
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
                discard();
                break;
            }
        }
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

    public void detonate(Level level)
    {
        if (tickCount < configType.getPrimeDelay() || detonated || isRemoved())
            return;

        detonated = true;

        spawnFlakParticles(level);
        ShootingHelper.onDetonate(level, configType, position(), this, Optional.ofNullable(firedShot).flatMap(FiredShot::getAttacker).orElse(null));
    }

    private void spawnFlakParticles(Level level)
    {
        // Send Flak packet
        if (!level.isClientSide && configType.getFlak() > 0)
        {
            PacketHandler.sendToAllAround(new PacketFlak(getX(), getY(), getZ(), configType.getFlak(), configType.getFlakParticles()), getX(), getY(), getZ(), BulletType.FLAK_PARTICLES_RANGE, level.dimension());
        }
    }

    //TODO: variable for attacker / owner?
    //TODO: getDamage method independent from firedShot?
}
