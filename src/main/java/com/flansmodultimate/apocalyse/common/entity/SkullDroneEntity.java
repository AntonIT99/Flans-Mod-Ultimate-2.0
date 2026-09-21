package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.apocalyse.common.util.ApocalypseGunCombat;
import com.flansmodultimate.apocalyse.common.util.ApocalypseGunHelper;
import com.flansmodultimate.config.ModApocalypseConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * An autonomous flying gun, as the 1.12.2 {@code EntitySkullDrone}.
 *
 * <p>The legacy drone had no goals at all. It picked up the nearest player now and then, turned
 * on whoever hurt it, and lost interest at random. While it had a target it hung at a drifting
 * point five to ten blocks above it and fired its gun at the weapon's own rate, reloading from a
 * single spare magazine. Explosions, fire and its own side's shots could not hurt it, and it never
 * despawned.</p>
 */
public class SkullDroneEntity extends Monster
{
    /** Per-tick chance of dropping the current target, or of looking for one when idle. */
    private static final int TARGET_CHANGE_CHANCE = 1200;
    /** Per-tick chance of picking a new hover point around the target. */
    private static final int REPOSITION_CHANCE = 60;
    private static final double ACQUIRE_RANGE = 100.0D;
    private static final double APPROACH_RATE = 0.06D;

    private final ApocalypseGunCombat gun = new ApocalypseGunCombat(this, ApocalypseGunCombat.Profile.DRONE, 1);
    private Vec3 offsetFromTarget = Vec3.ZERO;

    public SkullDroneEntity(EntityType<? extends SkullDroneEntity> type, Level level)
    {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 60.0D)
            .add(Attributes.FLYING_SPEED, 0.45D)
            .add(Attributes.FOLLOW_RANGE, ACQUIRE_RANGE)
            .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected void registerGoals()
    {
        // Deliberately none: targeting and flight are the legacy state machine in tick().
    }

    @Override
    public void tick()
    {
        setNoGravity(true);
        super.tick();
        fallDistance = 0F;
        if (!level().isClientSide)
            tickServer();
    }

    private void tickServer()
    {
        LivingEntity target = getTarget();
        if (target == null)
        {
            if (random.nextInt(TARGET_CHANGE_CHANCE) == 0)
                acquireNearestPlayer();
            return;
        }
        if (!target.isAlive() || target.isRemoved() || random.nextInt(TARGET_CHANGE_CHANCE) == 0)
        {
            setTarget(null);
            return;
        }

        if (random.nextInt(REPOSITION_CHANCE) == 0)
        {
            offsetFromTarget = new Vec3(offsetFromTarget.x * 0.5D + random.nextGaussian() * 10D,
                random.nextFloat() * 5D + 5D,
                offsetFromTarget.z * 0.5D + random.nextGaussian() * 10D);
        }
        Vec3 motion = target.position().add(offsetFromTarget).subtract(position()).scale(APPROACH_RATE);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);

        double dX = target.getX() - getX();
        double dY = target.getY() - getY();
        double dZ = target.getZ() - getZ();
        float targetYaw = (float) (Mth.atan2(dZ, dX) * Mth.RAD_TO_DEG) - 90F;
        float targetPitch = (float) (-Mth.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)) * Mth.RAD_TO_DEG);
        setYRot(getYRot() + Mth.wrapDegrees(targetYaw - getYRot()) / 2F);
        setXRot(getXRot() + (targetPitch - getXRot()) / 2F);
        setYHeadRot(getYRot());
        yBodyRot = getYRot();

        // The legacy drone only counted its gun down while it had something to shoot at.
        gun.tick();
        if (ModApocalypseConfig.apocalypseMobsEnabled())
            gun.tryFire(target);
    }

    private void acquireNearestPlayer()
    {
        Player player = level().getNearestPlayer(TargetingConditions.forCombat().range(ACQUIRE_RANGE)
            .selector(EntitySelector.NO_CREATIVE_OR_SPECTATOR::test).ignoreLineOfSight(), this);
        if (player != null)
            setTarget(player);
    }

    /** Also nudges the hover point, as the legacy {@code SetTarget} did. */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        super.setTarget(target);
        offsetFromTarget = new Vec3(offsetFromTarget.x + random.nextGaussian() * 5D, 10D,
            offsetFromTarget.z + random.nextGaussian() * 5D);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
            return false;
        if (source.getEntity() instanceof SkullBossEntity || source.getEntity() instanceof SkullDroneEntity)
            return false;
        boolean hurt = super.hurt(source, amount);
        // Retaliation switches target without moving the hover point, as in 1.12.2.
        if (!level().isClientSide && source.getEntity() instanceof LivingEntity attacker
            && !(attacker instanceof Player player && (player.isCreative() || player.isSpectator())))
            super.setTarget(attacker);
        return hurt;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag)
    {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        equipDefault(level.getRandom());
        return result;
    }

    /** A loaded gun and one spare magazine of the same ammunition, as the boss armed its drones. */
    public void equipDefault(RandomSource random)
    {
        ApocalypseGunHelper.randomLoadedGun(random, false).ifPresent(stack -> setItemSlot(EquipmentSlot.MAINHAND, stack));
        gun.stockCopyOfLoadedAmmo();
        setDropChance(EquipmentSlot.MAINHAND, 0.25F);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return false;
    }

    /** Persistent whether or not the saved PersistenceRequired flag survived a summon with NBT. */
    @Override
    public boolean requiresCustomPersistence()
    {
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source)
    {
        return false;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        gun.save(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        gun.load(tag);
    }
}
