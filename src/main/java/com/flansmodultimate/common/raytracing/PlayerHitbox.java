package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.BloodEffects;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.LongRangeDamage;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.teams.TeamsRound;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.ArmorZone;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.flansmodultimate.platform.item.ItemStackData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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

    static float bodyPartDamageModifier(EnumHitboxType type)
    {
        if (!com.flansmodultimate.common.teams.TeamsManager.getInstance().isBodyPartDamageModifiers())
            return 1F;
        return switch (type)
        {
            case HEAD -> ModCommonConfig.get().headshotDamageModifier();
            case BODY -> ModCommonConfig.get().chestshotDamageModifier();
            case LEGS -> ModCommonConfig.get().legshotModifier();
            case LEFTARM, RIGHTARM -> ModCommonConfig.get().armshotDamageModifier();
            default -> 1F;
        };
    }

    /**
     * @return the armour penetration resistance protecting this body part of the entity: the helmet for the head, leggings and boots
     *         together for the legs, the chestplate elsewhere. Bare slots count with their unarmoured resistance.
     */
    public static float getArmorPenetrationResistance(LivingEntity entity, EnumHitboxType type)
    {
        if (type == EnumHitboxType.HEAD)
            return getSlotPenetrationResistance(entity.getItemBySlot(EquipmentSlot.HEAD), ArmorType.UNARMORED_HELMET_PENETRATION_RESISTANCE);
        if (type == EnumHitboxType.LEGS)
            return getSlotPenetrationResistance(entity.getItemBySlot(EquipmentSlot.LEGS), ArmorType.UNARMORED_LEGGINGS_PENETRATION_RESISTANCE)
                + getSlotPenetrationResistance(entity.getItemBySlot(EquipmentSlot.FEET), ArmorType.UNARMORED_BOOTS_PENETRATION_RESISTANCE);
        return getSlotPenetrationResistance(entity.getItemBySlot(EquipmentSlot.CHEST), ArmorType.UNARMORED_CHESTPLATE_PENETRATION_RESISTANCE);
    }

    private static float getSlotPenetrationResistance(ItemStack stack, float unarmored)
    {
        return stack.getItem() instanceof CustomArmorItem armour && !armour.isDepleted(stack) ? armour.getConfigType().getPenetrationResistance() : unarmored;
    }

    /**
     * @return the zone struck by a round at the wound travelling along the direction: the back when it arrives from behind,
     *         the face or nape when it strikes the head below eye level
     */
    @Nullable
    public static ArmorZone zoneOf(LivingEntity entity, EnumHitboxType type, Vec3 wound, Vec3 direction)
    {
        Vec3 facing = Vec3.directionFromRotation(0F, entity.getYRot());
        boolean fromBehind = direction.x * facing.x + direction.z * facing.z > 0D;
        return ArmorZone.of(type, fromBehind, wound.y < entity.getEyeY());
    }

    /**
     * Applies the per-zone ratings of every worn piece to the round, playing a deflection and wearing the piece when it is
     * not penetrated. Server side only has effects.
     *
     * @return the share of damage that gets through
     */
    private float zoneArmorDamageFactor(FiredShot shot, @Nullable Bullet bullet, Vec3 wound, Vec3 direction)
    {
        ArmorZone zone = zoneOf(entity, type, wound, direction);
        if (zone == null)
            return 1F;
        BulletType bulletType = shot.getBulletType();
        float penetration = bulletType.getBodyArmorPenetration(LongRangeDamage.flightTicks(bullet, shot, entity));
        float factor = 1F;
        boolean deflected = false;
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() != EquipmentSlot.Type.ARMOR)
                continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof CustomArmorItem armour) || armour.isDepleted(stack) || !armour.getConfigType().isHasZoneArmor())
                continue;
            ZoneArmor.Result result = ZoneArmor.resolve(penetration, armour.getConfigType().getZoneArmor(zone));
            if (result == ZoneArmor.Result.PENETRATED)
                continue;
            factor *= ZoneArmor.damageFactor(result, bulletType.getNonPenPenalty(), bulletType.getBarelyPenPenalty());
            deflected = true;
            if (result == ZoneArmor.Result.NOT_PENETRATED && !entity.level().isClientSide)
                ItemStackData.hurtAndBreak(stack, 1, entity, slot);
        }
        if (deflected && entity.level() instanceof ServerLevel level)
        {
            level.playSound(null, wound.x, wound.y, wound.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 1.3F + level.random.nextFloat() * 0.3F);
            level.sendParticles(ParticleTypes.CRIT, wound.x, wound.y, wound.z, 8, 0.1D, 0.1D, 0.1D, 0.2D);
        }
        return factor;
    }

    public ShootingHelper.HitData hitByBullet(FiredShot shot, ShootingHelper.HitData hitData, @Nullable Bullet bullet)
    {
        return hitByBullet(shot, hitData, bullet, entity.position().add(0D, entity.getBbHeight() * 0.6D, 0D), Vec3.ZERO);
    }

    public ShootingHelper.HitData hitByBullet(FiredShot shot, ShootingHelper.HitData hitData, @Nullable Bullet bullet, Vec3 wound, Vec3 direction)
    {
        float lastHitPenAmount;
        float penetratingPower = hitData.penetratingPower();
        boolean lastHitHeadshot = hitData.lastHitHeadshot();

        BulletType bulletType = shot.getBulletType();
        if (bulletType.isSetEntitiesOnFire())
            EntityPlatform.igniteForSeconds(entity, 20);

        bulletType.getHitEffects().forEach(effect -> entity.addEffect(new MobEffectInstance(effect)));

        float totalPenetrationResistance = getArmorPenetrationResistance(entity, type);

        float damageModifier = 1F;
        if (penetratingPower <= BulletType.DEFAULT_PENETRATING_POWER * totalPenetrationResistance && ModCommonConfig.get().useNewPenetrationSystem())
            damageModifier = (float) Math.pow((penetratingPower / (BulletType.DEFAULT_PENETRATING_POWER * totalPenetrationResistance)), 2.5);
        else if (ModCommonConfig.get().useNewPenetrationSystem())
            damageModifier = bulletType.getPenetratingPower() < 0.1F ? (penetratingPower / bulletType.getPenetratingPower()) : 1F;

        lastHitPenAmount = Math.max(hitData.lastHitPenAmount(), damageModifier);

        damageModifier *= zoneArmorDamageFactor(shot, bullet, wound, direction);
        damageModifier *= bodyPartDamageModifier(type);
        if (type == EnumHitboxType.HEAD)
            lastHitHeadshot = true;

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
                    if (BloodEffects.hurt(entity, wound, direction, () -> entity.hurt(damagesource, hitDamage)))
                    {
                        // If the attack was allowed, we should remove their immortality cooldown so we can shoot them again. Without this, any rapid fire gun become useless
                        entity.hurtTime = Math.min(entity.hurtTime + 1, entity.hurtDuration);
                        entity.invulnerableTime = entity.hurtDuration / 2;

                    }
                }

                // Slowdown when shot in the legs
                if (type == EnumHitboxType.LEGS)
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, true, false));

                ShootingHelper.applyKnockbackModifier(entity, motBefore, bulletType.getKnockbackModifier());

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
