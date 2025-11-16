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
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
public class Bullet extends Shootable
{
    public static final int RENDER_DISTANCE = 128;

    protected BulletType bulletType;

    protected FiredShot firedShot;
    protected Entity lockedOnTo; // For homing missiles
    protected int bulletLife = 600; // Kill bullets after 30 seconds
    protected int ticksInAir;
    protected float currentPenetratingPower;
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
        bulletType = firedShot.getBulletType();

        setPos(origin);
        setArrowHeading(direction, firedShot.getFireableGun().getSpread() * firedShot.getBulletType().getBulletSpread(), firedShot.getFireableGun().getBulletSpeed());
        currentPenetratingPower = firedShot.getBulletType().getPenetratingPower();
    }

    public BulletType getBulletType()
    {
        if (bulletType == null && InfoType.getInfoType(getShortName()) instanceof BulletType bType)
        {
            bulletType = bType;
        }
        return bulletType;
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
        if (bulletType.isLockOnToPlanes() || bulletType.isLockOnToVehicles() || bulletType.isLockOnToMechas() || bulletType.isLockOnToLivings() || bulletType.isLockOnToPlayers())
        {
            Vector3f motionVec = new Vector3f(velocity);
            Entity closestEntity = null;
            float closestAngle = bulletType.getMaxLockOnAngle() * (float) Math.PI / 180F;

            for (Entity entity : ModUtils.queryEntitiesInRange(level, this, BulletType.LOCK_ON_RANGE, null))
            {
                if (bulletType.isLockOnToMechas() && entity instanceof Mecha
                    || bulletType.isLockOnToVehicles() && entity instanceof Vehicle
                    || bulletType.isLockOnToPlanes() && entity instanceof Plane
                    || bulletType.isLockOnToPlayers() && entity instanceof Player
                    || bulletType.isLockOnToLivings() && entity instanceof LivingEntity)
                {
                    Vector3f relPosVec = new Vector3f(entity.getX() - getX(), entity.getY() - getY(), entity.getZ() - getZ());
                    float angle = Math.abs(Vector3f.angle(motionVec, relPosVec));
                    if (angle < closestAngle)
                    {
                        closestEntity = entity;
                        closestAngle = angle;
                    }
                }
            }

            if(closestEntity != null)
                lockedOnTo = closestEntity;
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
                bulletType = type;
            if (bulletType == null)
            {
                FlansMod.log.warn("Unknown bullet type {}, discarding.", shortname);
                discard();
            }
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
            bulletType = bType;

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

            firedShot = new FiredShot(fireablegun, bulletType);
        }
        else
        {
            discard();
        }
    }

    @Override
    public void tick()
    {
        //TODO: add FMU logic
        super.tick();
        Level level = level();
        try
        {
            if (!level.isClientSide && checkforuuids)
            {
                resolvePendingUUIDs((ServerLevel) level);
                checkforuuids = false;
            }

            DebugHelper.spawnDebugVector(level, position(), velocity, 1000);
            if (handleFuseAndLifetime())
                return;
            if (!level.isClientSide)
                performRaytraceAndApplyHits(level);
            applyDragAndGravity();
            updatePositionAndOrientation();
            applyHomingIfLocked();

            if (level.isClientSide)
                clientTick((ClientLevel) level);

        }
        catch (Exception ex)
        {
            FlansMod.log.error("Error ticking bullet {}", shortname, ex);
            discard();
        }
    }

    protected void applyDragAndGravity()
    {
        float drag = isInWater() ? 0.8F : 0.99F;
        float gravity = 0.02F * bulletType.getFallSpeed();

        velocity = velocity.scale(drag).add(0.0, -gravity, 0.0);
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
        if (bulletType.isTrailParticles())
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

            ParticleHelper.spawnFromString(level, bulletType.getTrailParticleType(), x, y, z);
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

    protected boolean handleFuseAndLifetime()
    {
        ticksInAir++;

        if (bulletType.getFuse() > 0 && ticksInAir > bulletType.getFuse() && !isRemoved())
        {
            //TODO: detonate() instead of discard()?
            discard();
            return true;
        }
        if (tickCount > bulletLife)
        {
            discard();
            return true;
        }
        return isRemoved();
    }

    protected void performRaytraceAndApplyHits(Level level)
    {
        Vector3f origin = new Vector3f((float)getX(), (float)getY(), (float)getZ());
        Vector3f motion = new Vector3f((float)velocity.x, (float)velocity.y, (float)velocity.z);
        Optional<ServerPlayer> playerOptional = firedShot.getPlayerAttacker();
        Entity ignore = playerOptional.isPresent() ? playerOptional.get() : firedShot.getCausingEntity().orElse(null);

        int pingMs = firedShot.getPlayerAttacker().map(p -> p.latency).orElse(0);

        List<BulletHit> hits = FlansModRaytracer.raytrace(level, ignore, ticksInAir > 20, this, origin, motion, pingMs, 0f, getHitboxSize());

        if (hits.isEmpty())
            return;

        for (BulletHit bulletHit : hits)
        {
            Vector3f hitPos = new Vector3f(origin.x + motion.x * bulletHit.intersectTime, origin.y + motion.y * bulletHit.intersectTime, origin.z + motion.z * bulletHit.intersectTime);

            currentPenetratingPower = ShootingHelper.onHit(level, hitPos, motion, firedShot, bulletHit, currentPenetratingPower, this);
            if (currentPenetratingPower <= 0F)
            {
                ShootingHelper.onDetonate(level, firedShot, hitPos, this);
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
        double pull = angle * bulletType.getLockOnForce();
        pull = pull * pull;

        velocity = velocity.scale(0.95).add(pull * dX / d2, pull * dY / d2, pull * dZ / d2);
        setDeltaMovement(velocity);
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
            {
                shooter = player;
            }
            else
            {
                shooter = level.getEntity(shooteruuid); // null if not loaded
            }
            shooteruuid = null;
        }

        if (player != null)
        {
            firedShot = new FiredShot(firedShot.getFireableGun(), bulletType, shooter, player);
        }
        else
        {
            firedShot = new FiredShot(firedShot.getFireableGun(), bulletType, shooter, (shooter instanceof LivingEntity living) ? living : null);
        }
    }
}
