package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.client.render.ParticleHelper;
import com.wolffsarmormod.common.guns.EnumSpreadPattern;
import com.wolffsarmormod.common.guns.FireableGun;
import com.wolffsarmormod.common.guns.FiredShot;
import com.wolffsarmormod.common.types.BulletType;
import com.wolffsarmormod.common.types.InfoType;
import lombok.Getter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class Bullet extends Shootable
{
    private static final EntityDataAccessor<String> BULLET_TYPE = SynchedEntityData.defineId(Bullet.class, EntityDataSerializers.STRING);

    @Getter
    protected FiredShot firedShot;

    protected int bulletLife = 600; // Kill bullets after 30 seconds
    protected int ticksInAir;

    /** For homing missiles */
    protected Entity lockedOnTo;
    protected float currentPenetratingPower;

    @OnlyIn(Dist.CLIENT)
    protected boolean playedFlybySound;

    /** These values are used to store the UUIDs until the next entity update is performed. This prevents issues caused by the loading order */
    protected UUID playeruuid;
    protected UUID shooteruuid;
    protected boolean checkforuuids;

    public Bullet(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Bullet(Level level, FiredShot firedShot, Vec3 origin, Vec3 direction)
    {
        this(ArmorMod.bulletEntity.get(), level);
        ticksInAir = 0;
        this.firedShot = firedShot;
        this.entityData.set(BULLET_TYPE, firedShot.getBulletType().getShortName());

        setPos(origin.x, origin.y, origin.z);
        setArrowHeading(direction.x, direction.y, direction.z, firedShot.getFireableGun().getSpread() * firedShot.getBulletType().getBulletSpread(), firedShot.getFireableGun().getBulletSpeed());

        currentPenetratingPower = firedShot.getBulletType().getPenetratingPower();
    }

    public void setArrowHeading(double dx, double dy, double dz, float spread, float speed)
    {
        spread /= 5F;
        float f2 = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= f2;
        dy /= f2;
        dz /= f2;
        dx *= speed;
        dy *= speed;
        dz *= speed;
        dx += random.nextGaussian() * 0.005D * spread * speed;
        dy += random.nextGaussian() * 0.005D * spread * speed;
        dz += random.nextGaussian() * 0.005D * spread * speed;
        setDeltaMovement(dx, dy, dz);
        getLockOnTarget();
    }

    @Override
    public void setDeltaMovement(double dx, double dy, double dz)
    {
        super.setDeltaMovement(dx, dy, dz);
        hasImpulse = true; //optional but helps clients notice an immediate motion change

        // only initialize rotation once (same behavior as the old check)
        if (xRotO == 0.0F && yRotO == 0.0F)
        {
            // horizontal length
            double f = Math.sqrt(dx * dx + dz * dz);

            // compute yaw/pitch in degrees
            float yaw   = (float) (Mth.atan2(dx, dz) * (180.0F / Math.PI));
            float pitch = (float) (Mth.atan2(dy, f)  * (180.0F / Math.PI));

            // set current + previous rotations
            setYRot(yaw);
            setXRot(pitch);
            yRotO = yaw;
            xRotO = pitch;
        }
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(BULLET_TYPE, "");
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
            double vx = buf.readDouble();
            double vy = buf.readDouble();
            double vz = buf.readDouble();
            setDeltaMovement(vx, vy, vz);

            // If you want the client to immediately show motion:
            hasImpulse = true;
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
        tag.putString("type", firedShot.getBulletType().getShortName());

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
            firedShot.getShooterOptional().ifPresent((net.minecraft.world.entity.Entity shooter) -> tag.putUUID("shooter", shooter.getUUID()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        String shortName = tag.getString("type");
        InfoType.getInfoType(shortName).ifPresent(infoType -> {
            if (!(infoType instanceof BulletType bulletType))
                return;

            // sync your tracked data (String accessor you defined earlier)
            entityData.set(BULLET_TYPE, shortName);

            if (tag.contains("fireablegun", Tag.TAG_COMPOUND))
            {
                CompoundTag gun = tag.getCompound("fireablegun");
                float damage = gun.getFloat("damage");
                float vDamage = gun.getFloat("vehicledamage");
                float spread = gun.getFloat("spread");
                float speed = gun.getFloat("speed");

                InfoType.getInfoType(gun.getString("infotype")).ifPresent(fireablegunInfoType -> {
                    FireableGun fireablegun = new FireableGun(fireablegunInfoType, damage, vDamage, spread, speed, EnumSpreadPattern.CIRCLE);
                    firedShot = new FiredShot(fireablegun, bulletType);
                });
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
        });
    }

    @Override
    public void tick()
    {

    }

    @OnlyIn(Dist.CLIENT)
    protected void clientTick()
    {

    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnParticles()
    {
        if (!this.level().isClientSide)
            return;

        ClientLevel client = (ClientLevel) this.level();

        // segment between previous and current position
        double dX = (this.getX() - this.xo) / 10.0;
        double dY = (this.getY() - this.yo) / 10.0;
        double dZ = (this.getZ() - this.zo) / 10.0;

        float spread = 0.1F;

        String trailParticleType = firedShot.getBulletType().getTrailParticleType();
        boolean fancyLike = Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST;

        for (int i = 0; i < 10; i++)
        {
            double x = this.xo + dX * i + this.random.nextGaussian() * spread;
            double y = this.yo + dY * i + this.random.nextGaussian() * spread;
            double z = this.zo + dZ * i + this.random.nextGaussian() * spread;

            ParticleHelper.spawnFromString(client, trailParticleType, x, y, z, fancyLike);
        }
    }

    /**
     * Find the entity nearest to the missile's trajectory, anglewise
     */
    protected void getLockOnTarget()
    {
        //TODO: implement
        /*BulletType type = shot.getBulletType();

        if(type.lockOnToPlanes || type.lockOnToVehicles || type.lockOnToMechas || type.lockOnToLivings || type.lockOnToPlayers)
        {
            Vector3f motionVec = new Vector3f(motionX, motionY, motionZ);
            Entity closestEntity = null;
            float closestAngle = type.maxLockOnAngle * 3.14159265F / 180F;

            for(Object obj : world.loadedEntityList)
            {
                Entity entity = (Entity)obj;
                if((type.lockOnToMechas && entity instanceof EntityMecha)
                        || (type.lockOnToVehicles && entity instanceof EntityVehicle)
                        || (type.lockOnToPlanes && entity instanceof EntityPlane)
                        || (type.lockOnToPlayers && entity instanceof EntityPlayer)
                        || (type.lockOnToLivings && entity instanceof EntityLivingBase))
                {
                    Vector3f relPosVec = new Vector3f(entity.posX - posX, entity.posY - posY, entity.posZ - posZ);
                    float angle = Math.abs(Vector3f.angle(motionVec, relPosVec));
                    if(angle < closestAngle)
                    {
                        closestEntity = entity;
                        closestAngle = angle;
                    }
                }
            }

            if(closestEntity != null)
                lockedOnTo = closestEntity;
        }*/
    }
}
