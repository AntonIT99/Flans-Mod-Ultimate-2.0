package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.guns.FiredShot;
import com.wolffsarmormod.common.types.BulletType;
import lombok.Getter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
    protected BulletType configType;

    protected int bulletLife = 600; // Kill bullets after 30 seconds
    protected int ticksInAir;
    protected FiredShot shot;
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

    public Bullet(Level level, FiredShot shot, Vec3 origin, Vec3 direction)
    {
        this(ArmorMod.bulletEntity.get(), level);
        ticksInAir = 0;
        this.shot = shot;
        this.entityData.set(BULLET_TYPE, shot.getBulletType().getShortName());

        setPos(origin.x, origin.y, origin.z);
        setDeltaMovement(direction.x, direction.y, direction.z);
        setArrowHeading(direction.x, direction.y, direction.z, shot.getFireableGun().getSpread() * shot.getBulletType().getBulletSpread(), shot.getFireableGun().getBulletSpeed());

        currentPenetratingPower = shot.getBulletType().getPenetratingPower();
    }

    public void setArrowHeading(double d, double d1, double d2, float spread, float speed)
    {
        spread /= 5F;
        float f2 = (float) Math.sqrt(d * d + d1 * d1 + d2 * d2);
        d /= f2;
        d1 /= f2;
        d2 /= f2;
        d *= speed;
        d1 *= speed;
        d2 *= speed;
        d += random.nextGaussian() * 0.005D * spread * speed;
        d1 += random.nextGaussian() * 0.005D * spread * speed;
        d2 += random.nextGaussian() * 0.005D * spread * speed;
        setDeltaMovement(d, d1, d2);
        float f3 = (float) Math.sqrt(d * d + d2 * d2);
        float yaw   = (float) Mth.wrapDegrees((Math.atan2(d,  d2) * 180.0D / Math.PI));
        float pitch = (float) Mth.wrapDegrees((Math.atan2(d1, f3) * 180.0D / Math.PI));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;

        getLockOnTarget();
    }

    /**
     * Find the entity nearest to the missile's trajectory, anglewise
     */
    private void getLockOnTarget()
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

    @Override
    protected void defineSynchedData()
    {
        entityData.define(BULLET_TYPE, "");
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound)
    {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound)
    {

    }
}
