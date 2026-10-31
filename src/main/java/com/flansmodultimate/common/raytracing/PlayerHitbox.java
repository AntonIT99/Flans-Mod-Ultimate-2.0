package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.teams.TeamsRound;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketParticle;
import com.flansmodultimate.platform.entity.EntityPlatform;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class PlayerHitbox
{
    /** The player or humanoid NPC the box belongs to */
    public final LivingEntity entity;
    /**
     * Maps box-local coordinates to coordinates relative to the snapshot position
     */
    public final Matrix4f transform;
    /**
     * Inverse of {@link #transform}, used to bring rays into box-local coordinates
     */
    private final Matrix4f inverseTransform;
    /**
     * The lower left corner of this box
     */
    public Vector3f o;
    /**
     * The dimensions of this box
     */
    public Vector3f d;
    /**
     * The velocity of this box, in world axes.
     */
    public Vector3f vel;
    /**
     * The type of hitbox
     */
    public EnumHitboxType type;

    public PlayerHitbox(LivingEntity entity, Matrix4f transform, Vector3f origin, Vector3f dimensions, Vector3f velocity, EnumHitboxType type)
    {
        this.entity = entity;
        this.transform = transform;
        this.inverseTransform = new Matrix4f(transform).invert();
        this.o = origin;
        this.d = dimensions;
        this.type = type;
        this.vel = velocity;
    }

    public PlayerBulletHit raytrace(Vector3f origin, Vector3f motion)
    {
        // Move to local coords for this hitbox, but don't modify the original vectors.
        // An affine transform keeps the ray parameter unchanged, so intersect times stay valid in world space
        origin = inverseTransform.transformPosition(new Vector3f(origin));
        motion = inverseTransform.transformDirection(new Vector3f(motion).sub(vel));

        // We now have an AABB starting at o and with dimensions d and our ray in the same coordinate system
        // We are looking for a point at which the ray enters the box, so we need only consider faces that the ray can see. Partition the space into 3 areas in each axis

        // X - axis and faces x = o.x and x = o.x + d.x
        if (motion.x != 0F)
        {
            if (origin.x < o.x) // Check face x = o.x
            {
                float intersectTime = (o.x - origin.x) / motion.x;
                float intersectY = origin.y + motion.y * intersectTime;
                float intersectZ = origin.z + motion.z * intersectTime;
                if (intersectY >= o.y && intersectY <= o.y + d.y && intersectZ >= o.z && intersectZ <= o.z + d.z)
                    return new PlayerBulletHit(this, intersectTime);
            }
            else if (origin.x > o.x + d.x) // Check face x = o.x + d.x
            {
                float intersectTime = (o.x + d.x - origin.x) / motion.x;
                float intersectY = origin.y + motion.y * intersectTime;
                float intersectZ = origin.z + motion.z * intersectTime;
                if (intersectY >= o.y && intersectY <= o.y + d.y && intersectZ >= o.z && intersectZ <= o.z + d.z)
                    return new PlayerBulletHit(this, intersectTime);
            }
        }

        // Z - axis and faces z = o.z and z = o.z + d.z
        if (motion.z != 0F)
        {
            if (origin.z < o.z) // Check face z = o.z
            {
                float intersectTime = (o.z - origin.z) / motion.z;
                float intersectX = origin.x + motion.x * intersectTime;
                float intersectY = origin.y + motion.y * intersectTime;
                if (intersectX >= o.x && intersectX <= o.x + d.x && intersectY >= o.y && intersectY <= o.y + d.y)
                    return new PlayerBulletHit(this, intersectTime);
            }
            else if (origin.z > o.z + d.z) // Check face z = o.z + d.z
            {
                float intersectTime = (o.z + d.z - origin.z) / motion.z;
                float intersectX = origin.x + motion.x * intersectTime;
                float intersectY = origin.y + motion.y * intersectTime;
                if (intersectX >= o.x && intersectX <= o.x + d.x && intersectY >= o.y && intersectY <= o.y + d.y)
                    return new PlayerBulletHit(this, intersectTime);
            }
        }

        // Y - axis and faces y = o.y and y = o.y + d.y
        if (motion.y != 0F)
        {
            if (origin.y < o.y) // Check face y = o.y
            {
                float intersectTime = (o.y - origin.y) / motion.y;
                float intersectX = origin.x + motion.x * intersectTime;
                float intersectZ = origin.z + motion.z * intersectTime;
                if (intersectX >= o.x && intersectX <= o.x + d.x && intersectZ >= o.z && intersectZ <= o.z + d.z)
                    return new PlayerBulletHit(this, intersectTime);
            }
            else if (origin.y > o.y + d.y) // Check face x = o.x + d.x
            {
                float intersectTime = (o.y + d.y - origin.y) / motion.y;
                float intersectX = origin.x + motion.x * intersectTime;
                float intersectZ = origin.z + motion.z * intersectTime;
                if (intersectX >= o.x && intersectX <= o.x + d.x && intersectZ >= o.z && intersectZ <= o.z + d.z)
                    return new PlayerBulletHit(this, intersectTime);
            }
        }

        return null;
    }

    public ShootingHelper.HitData hitByBullet(FiredShot shot, ShootingHelper.HitData hitData, @Nullable Bullet bullet)
    {
        float lastHitPenAmount;
        float penetratingPower = hitData.penetratingPower();
        boolean lastHitHeadshot = hitData.lastHitHeadshot();

        BulletType bulletType = shot.getBulletType();
        if (bulletType.isSetEntitiesOnFire())
            EntityPlatform.igniteForSeconds(entity, 20);

        bulletType.getHitEffects().forEach(effect -> entity.addEffect(new MobEffectInstance(effect)));

        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = entity.getItemBySlot(EquipmentSlot.FEET);

        float headPenRes = !(head.getItem() instanceof CustomArmorItem headArmour) ? ArmorType.UNARMORED_HELMET_PENETRATION_RESISTANCE : headArmour.getConfigType().getPenetrationResistance();
        float chestPenRes = !(chest.getItem() instanceof CustomArmorItem chestArmour) ? ArmorType.UNARMORED_CHESTPLATE_PENETRATION_RESISTANCE : chestArmour.getConfigType().getPenetrationResistance();
        float legsPenRes = !(legs.getItem() instanceof CustomArmorItem legsArmour) ? ArmorType.UNARMORED_LEGGINGS_PENETRATION_RESISTANCE : legsArmour.getConfigType().getPenetrationResistance();
        float feetPenRes = !(feet.getItem() instanceof CustomArmorItem feetArmour) ? ArmorType.UNARMORED_BOOTS_PENETRATION_RESISTANCE : feetArmour.getConfigType().getPenetrationResistance();
        float totalPenetrationResistance;

        if (type == EnumHitboxType.HEAD)
            totalPenetrationResistance = headPenRes;
        else if (type == EnumHitboxType.LEGS)
            totalPenetrationResistance = legsPenRes + feetPenRes;
        else
            totalPenetrationResistance = chestPenRes;

        float damageModifier = 1F;
        if (penetratingPower <= BulletType.DEFAULT_PENETRATING_POWER * totalPenetrationResistance && ModCommonConfig.get().useNewPenetrationSystem())
            damageModifier = (float) Math.pow((penetratingPower / (BulletType.DEFAULT_PENETRATING_POWER * totalPenetrationResistance)), 2.5);
        else if (ModCommonConfig.get().useNewPenetrationSystem())
            damageModifier = bulletType.getPenetratingPower() < 0.1F ? (penetratingPower / bulletType.getPenetratingPower()) : 1F;

        lastHitPenAmount = Math.max(hitData.lastHitPenAmount(), damageModifier);

        if (type == EnumHitboxType.HEAD)
        {
            damageModifier *= ModCommonConfig.get().headshotDamageModifier();
            lastHitHeadshot = true;
        }
        else if (type == EnumHitboxType.BODY)
        {
            damageModifier *= ModCommonConfig.get().chestshotDamageModifier();
        }
        else if (type == EnumHitboxType.LEGS)
        {
            damageModifier *= ModCommonConfig.get().legshotModifier();
        }
        else if (type == EnumHitboxType.LEFTARM || type == EnumHitboxType.RIGHTARM)
        {
            damageModifier *= ModCommonConfig.get().armshotDamageModifier();
        }

        switch (type)
        {
            case LEGS, BODY, HEAD, LEFTARM, RIGHTARM :
            {
                Vec3 motBefore = entity.getDeltaMovement();

                if (!entity.level().isClientSide)
                {
                    // Calculate the hit damage
                    float hitDamage = ShootingHelper.getDamage(entity, bullet, shot) * damageModifier;
                    // Create a damage source object
                    DamageSource damagesource = shot.getDamageSource(type.equals(EnumHitboxType.HEAD), entity.level(), bullet);

                    // When the damage is 0 (such as with Nerf guns) the entityHurt Forge hook is not called, so this hacky thing is here
                    Optional<TeamsRound> currentRound = FlansMod.teamsManager.getCurrentRound();

                    if (hitDamage == 0 && currentRound.isPresent() && entity instanceof ServerPlayer player)
                        currentRound.get().getGametype().playerAttacked(player, damagesource);

                    // Attack the entity!
                    ShootingHelper.clearDamageCooldown(entity);
                    if (entity.hurt(damagesource, hitDamage))
                    {
                        // If the attack was allowed, we should remove their immortality cooldown so we can shoot them again. Without this, any rapid fire gun become useless
                        entity.hurtTime = Math.min(entity.hurtTime + 1, entity.hurtDuration);
                        entity.invulnerableTime = entity.hurtDuration / 2;
                        // Humanoid NPCs keep the puff every other entity shows when a round hurts it
                        if (!(entity instanceof Player))
                            PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.RED_DUST, entity.getX(), entity.getY(), entity.getZ(), 0, 0, 0), entity.position(),
                                ModCommonConfig.entityHitParticleRange(), entity.level().dimension());
                    }
                }

                // Slowdown when shot in the legs
                if (type == EnumHitboxType.LEGS)
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, true, false));

                // Handle knockback by finding entity motion before and after, and reapplying to negate effect of vanilla code.
                Vec3 motAfter = entity.getDeltaMovement();
                Vec3 deltaV = motAfter.subtract(motBefore).scale(1 - bulletType.getKnockbackModifier());

                if (bulletType.getKnockbackModifier() > 2F)
                    deltaV = new Vec3(deltaV.x, Math.sqrt(deltaV.y), deltaV.z);

                entity.setDeltaMovement(entity.getDeltaMovement().subtract(deltaV));

                if (ModCommonConfig.get().useNewPenetrationSystem())
                    penetratingPower -= totalPenetrationResistance;
                else
                    penetratingPower -= 1F;

                break;
            }
            case RIGHTITEM :
            {
                ItemStack currentStack = entity.getMainHandItem();
                if (!currentStack.isEmpty() && currentStack.getItem() instanceof GunItem gunItem)
                    penetratingPower -= gunItem.getConfigType().getShieldDamageAbsorption();
                break;
            }
            case LEFTITEM :
            {
                ItemStack currentStack = entity.getOffhandItem();
                if (!currentStack.isEmpty() && currentStack.getItem() instanceof GunItem gunItem)
                    penetratingPower -= gunItem.getConfigType().getShieldDamageAbsorption();
                break;
            }
            default :
                break;
        }

        return new ShootingHelper.HitData(penetratingPower, lastHitPenAmount, lastHitHeadshot);
    }
}
