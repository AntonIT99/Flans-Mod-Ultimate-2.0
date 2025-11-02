package com.wolffsarmormod.common.entity;

import com.flansmod.common.vector.Vector3f;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.ModUtils;
import com.wolffsarmormod.client.render.ParticleHelper;
import com.wolffsarmormod.common.guns.EnumSpreadPattern;
import com.wolffsarmormod.common.guns.FireableGun;
import com.wolffsarmormod.common.guns.FiredShot;
import com.wolffsarmormod.common.guns.ShotHandler;
import com.wolffsarmormod.common.raytracing.BulletHit;
import com.wolffsarmormod.common.raytracing.FlansModRaytracer;
import com.wolffsarmormod.common.types.BulletType;
import com.wolffsarmormod.common.types.InfoType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Bullet extends Shootable
{
    public static final int RENDER_DISTANCE = 128;

    /** Server side */
    protected FiredShot firedShot;
    protected Entity lockedOnTo; // For homing missiles
    protected int bulletLife = 600; // Kill bullets after 30 seconds
    protected int ticksInAir;
    protected float currentPenetratingPower;

    @OnlyIn(Dist.CLIENT)
    protected boolean playedFlybySound;

    /** These values are used to store the UUIDs until the next entity update is performed. This prevents issues caused by the loading order */
    protected boolean checkforuuids;
    protected UUID playeruuid;
    protected UUID shooteruuid;

    public Bullet(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Bullet(Level level, FiredShot firedShot, Vec3 origin, Vec3 direction)
    {
        super(ArmorMod.bulletEntity.get(), level, firedShot.getBulletType());
        this.firedShot = firedShot;
        ticksInAir = 0;

        setPos(origin);
        System.out.println("VORHER " + direction);
        setArrowHeading(direction, firedShot.getFireableGun().getSpread() * firedShot.getBulletType().getBulletSpread(), firedShot.getFireableGun().getBulletSpeed());
        System.out.println("NACHHER " + direction);
        currentPenetratingPower = firedShot.getBulletType().getPenetratingPower();
    }

    @Override
    public void setDeltaMovement(double dx, double dy, double dz)
    {
        super.setDeltaMovement(dx, dy, dz);
        // Immediately show motion
        hasImpulse = true;
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
        Vec3 velocity = direction.normalize().scale(speed).add(random.nextGaussian() * jitter, random.nextGaussian() * jitter, random.nextGaussian() * jitter);
        setDeltaMovement(velocity);
        computeRotations(velocity);
        getLockOnTarget();
    }

    protected void computeRotations(Vec3 velocity)
    {
        final double RAD_TO_DEG = 180.0 / Math.PI;

        // horizontal length: sqrt(x^2 + z^2)
        double lengthXZ = Math.hypot(velocity.x, velocity.z);
        // compute yaw/pitch in degrees
        float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(velocity.y, lengthXZ) * RAD_TO_DEG);

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
            computeRotations(velocity);
            moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        }
    }

    /**
     * Find the entity nearest to the missile's trajectory, anglewise
     */
    protected void getLockOnTarget()
    {
        BulletType type = firedShot.getBulletType();

        if (type.isLockOnToPlanes() || type.isLockOnToVehicles() || type.isLockOnToMechas() || type.isLockOnToLivings() || type.isLockOnToPlayers())
        {
            Vector3f motionVec = new Vector3f(getDeltaMovement());
            Entity closestEntity = null;
            float closestAngle = type.getMaxLockOnAngle() * (float) Math.PI / 180F;

            for (Entity entity : ModUtils.queryEntitiesInRange(level(), this, BulletType.LOCK_ON_RANGE, null))
            {
                //TODO: driveable entities
                /*if (type.lockOnToMechas && entity instanceof EntityMecha
                        || type.lockOnToVehicles && entity instanceof EntityVehicle
                        || type.lockOnToPlanes && entity instanceof EntityPlane
                        || type.lockOnToPlayers && entity instanceof EntityPlayer
                        || type.lockOnToLivings && entity instanceof EntityLivingBase)*/
                if (type.isLockOnToPlayers() && entity instanceof Player || type.isLockOnToLivings() && entity instanceof LivingEntity)
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
    @NotNull
    public Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        super.writeSpawnData(buf);
        Vec3 v = getDeltaMovement();
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        try
        {
            super.readSpawnData(buf);
            double vx = buf.readDouble();
            double vy = buf.readDouble();
            double vz = buf.readDouble();

            setDeltaMovement(vx, vy, vz);
        }
        catch (Exception e)
        {
            discard();
            ArmorMod.log.debug("Failed to read bullet spawn data", e);
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

            firedShot.getPlayerOptional().ifPresent((ServerPlayer player) -> tag.putUUID("player", player.getUUID()));
            firedShot.getShooterOptional().ifPresent((Entity shooter) -> tag.putUUID("shooter", shooter.getUUID()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        FireableGun fireablegun = null;
        setShortName(tag.getString("type"));
        InfoType infoType = InfoType.getInfoType(shortname);

        if (infoType != null)
        {
            if (!(infoType instanceof BulletType bulletType))
                return;

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
    }

    @Override
    public void tick()
    {
        super.tick();
        try
        {
            if (checkforuuids)
            {
                resolvePendingUUIDs();
                checkforuuids = false;
            }

            applyDragAndGravity(fallSpeed);
            move(MoverType.SELF, getDeltaMovement());
            updateOrientationFromVelocity();

            if (level().isClientSide)
                clientTick();
            else
                serverTick();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            discard();
        }
    }

    /* ======================= Physics & Orientation ======================= */

    protected void applyDragAndGravity(float fallSpeed)
    {
        float drag = isInWater() ? 0.8F : 0.99F;
        float gravity = 0.02F * fallSpeed;

        Vec3 v = getDeltaMovement().scale(drag).add(0.0, -gravity, 0.0);
        setDeltaMovement(v);
    }

    protected void updateOrientationFromVelocity()
    {
        Vec3 v = getDeltaMovement();
        float horiz = (float) Math.sqrt(v.x * v.x + v.z * v.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(v.x, v.z)));
        float targetPitch = (float) (Math.toDegrees(Math.atan2(v.y, horiz)));

        setYRot(net.minecraft.util.Mth.rotLerp(0.2F, getYRot(), targetYaw));
        setXRot(net.minecraft.util.Mth.rotLerp(0.2F, getXRot(), targetPitch));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    /* ======================= Water / Particles / Sound ======================= */

    @OnlyIn(Dist.CLIENT)
    protected void clientTick()
    {
        if (isInWater())
            spawnWaterBubbles();
        if (trailParticles) {
            spawnParticles();
        }
        playFlybyIfClose();
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnWaterBubbles()
    {
        Vec3 v = getDeltaMovement();
        for (int i = 0; i < 4; i++)
        {
            double t = 0.25;
            level().addParticle(ParticleTypes.BUBBLE, getX() - v.x * t, getY() - v.y * t, getZ() - v.z * t, v.x, v.y, v.z);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnParticles()
    {
        if (!level().isClientSide)
            return;

        ClientLevel client = (ClientLevel) level();

        // segment between previous and current position
        double dX = (getX() - xo) / 10.0;
        double dY = (getY() - yo) / 10.0;
        double dZ = (getZ() - zo) / 10.0;

        float spread = 0.1F;

        boolean fancyLike = Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST;

        for (int i = 0; i < 10; i++)
        {
            double x = xo + dX * i + random.nextGaussian() * spread;
            double y = yo + dY * i + random.nextGaussian() * spread;
            double z = zo + dZ * i + random.nextGaussian() * spread;

            ParticleHelper.spawnFromString(client, trailParticleType, x, y, z, fancyLike);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void playFlybyIfClose()
    {
        Minecraft mc = Minecraft.getInstance();
        if (playedFlybySound || mc.player == null)
            return;
        if (distanceToSqr(mc.player) >= 25.0) // within 5 blocks
            return;

        playedFlybySound = true;
        float soundVolume = 10.0F;
        float soundPitch = 1.0F / (random.nextFloat() * 0.4F + 0.8F);
        ArmorMod.getSoundEvent(ArmorMod.SOUND_BULLETFLYBY).ifPresent(soundEvent ->
                level().playLocalSound(getX(), getY(), getZ(), soundEvent.get(), SoundSource.HOSTILE, soundVolume, soundPitch, false));
    }

    /* ======================= Server Tick Orchestration ======================= */

    protected void serverTick()
    {
        //TODO: Debug Mode
        //if (FlansMod.DEBUG) spawnDebugVector();

        if (handleFuseAndLifetime())
            return; // may discard

        performRaytraceAndApplyHits();
        applyHomingIfLocked();
    }

    private void spawnDebugVector()
    {
        //TODO: Debug Mode
        /*Vec3 v = getDeltaMovement();
        level().addFreshEntity(new EntityDebugVector(level(),
                new Vector3f((float)getX(), (float)getY(), (float)getZ()),
                new Vector3f((float)v.x, (float)v.y, (float)v.z),
                20));*/
    }

    /**
     * @return true if entity was discarded
     */
    private boolean handleFuseAndLifetime()
    {
        ticksInAir++;
        BulletType type = firedShot.getBulletType();

        if (type.getFuse() > 0 && ticksInAir > type.getFuse() && !isRemoved())
        {
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

    /* ======================= Raytrace & Hit Handling ======================= */

    private void performRaytraceAndApplyHits()
    {
        Vec3 dv = getDeltaMovement();
        Vector3f origin = new Vector3f((float)getX(), (float)getY(), (float)getZ());
        Vector3f motion = new Vector3f((float)dv.x, (float)dv.y, (float)dv.z);

        Optional<ServerPlayer> playerOptional = firedShot.getPlayerOptional();
        Entity ignore = playerOptional.isPresent() ? playerOptional.get() : firedShot.getShooterOptional().orElse(null);

        int pingMs = firedShot.getPlayerOptional().map(p -> p.latency).orElse(0);

        List<BulletHit> hits = FlansModRaytracer.raytrace(level(), ignore, ticksInAir > 20, this, origin, motion, pingMs, 0f);

        if (hits.isEmpty())
            return;

        for (BulletHit bulletHit : hits)
        {
            Vector3f hitPos = new Vector3f(origin.x + motion.x * bulletHit.intersectTime, origin.y + motion.y * bulletHit.intersectTime, origin.z + motion.z * bulletHit.intersectTime);

            currentPenetratingPower = ShotHandler.onHit(level(), hitPos, motion, firedShot, bulletHit, currentPenetratingPower);
            if (currentPenetratingPower <= 0F)
            {
                ShotHandler.onDetonate(level(), firedShot, hitPos);
                discard();
                break;
            }
        }
    }

    /* ======================= Homing ======================= */

    protected void applyHomingIfLocked()
    {
        if (lockedOnTo == null || isRemoved()) return;

        double dX = lockedOnTo.getX() - getX();
        double dY = lockedOnTo.getY() - getY();
        double dZ = lockedOnTo.getZ() - getZ();
        double d2 = dX * dX + dY * dY + dZ * dZ;
        if (d2 < 1.0e-6) return;

        Vec3 vNow = getDeltaMovement();
        Vector3f motion = new Vector3f((float)vNow.x, (float)vNow.y, (float)vNow.z);
        Vector3f toTarget = new Vector3f((float)dX, (float)dY, (float)dZ);

        float angle = Math.abs(Vector3f.angle(motion, toTarget));
        double pull = angle * firedShot.getBulletType().getLockOnForce();
        pull = pull * pull;

        Vec3 vv = vNow.scale(0.95).add(pull * dX / d2, pull * dY / d2, pull * dZ / d2);
        setDeltaMovement(vv);
    }

    /* ======================= UUID Resolution ======================= */

    protected void resolvePendingUUIDs()
    {
        if (level().isClientSide)
            return;

        ServerLevel sl = (ServerLevel) level();
        ServerPlayer player = null;
        Entity shooter = null;

        if (playeruuid != null)
        {
            player = (ServerPlayer) sl.getPlayerByUUID(playeruuid);
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
                shooter = sl.getEntity(shooteruuid); // null if not loaded
            }
            shooteruuid = null;
        }

        if (shooter != null)
        {
            firedShot = new FiredShot(firedShot.getFireableGun(), firedShot.getBulletType(), shooter, player);
        }
    }
}
