package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.apocalyse.common.util.ApocalypseGunHelper;
import com.flansmodultimate.config.ModApocalypseConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class SkullDroneEntity extends Monster implements RangedAttackMob
{
    private int attackCooldown;

    public SkullDroneEntity(EntityType<? extends SkullDroneEntity> type, Level level)
    {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 50.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.32D)
            .add(Attributes.FLYING_SPEED, 0.45D)
            .add(Attributes.FOLLOW_RANGE, 80.0D)
            .add(Attributes.ARMOR, 6.0D)
            .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 18, 48.0F));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick()
    {
        setNoGravity(true);
        super.tick();
        if (attackCooldown > 0)
            attackCooldown--;

        LivingEntity target = getTarget();
        if (!level().isClientSide() && target != null && target.isAlive())
        {
            Vec3 wanted = target.getEyePosition().subtract(position()).normalize().scale(0.16D);
            setDeltaMovement(getDeltaMovement().scale(0.85D).add(wanted));
            if (distanceToSqr(target) < 100.0D)
                setDeltaMovement(getDeltaMovement().scale(0.45D));
        }
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity target, float distanceFactor)
    {
        if (attackCooldown > 0 || level().isClientSide() || !ModApocalypseConfig.apocalypseMobsEnabled())
            return;
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (ApocalypseGunHelper.shootLoadedGun(this, target))
            attackCooldown = 12 + getRandom().nextInt(12);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation") // NeoForge marks this as override-only; external callers use EventHooks.
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull EntitySpawnReason spawnType, @Nullable SpawnGroupData spawnData)
    {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        equipDefault(level.getRandom());
        return result;
    }

    public void equipDefault(RandomSource random)
    {
        ApocalypseGunHelper.randomLoadedGun(random, false).ifPresent(stack -> setItemSlot(EquipmentSlot.MAINHAND, stack));
        setDropChance(EquipmentSlot.MAINHAND, 0.25F);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, @NotNull DamageSource source)
    {
        return false;
    }
}
