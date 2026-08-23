package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.apocalyse.common.util.ApocalypseGunHelper;
import com.flansmodultimate.apocalyse.common.util.ApocalypseLoot;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class SurvivorEntity extends PathfinderMob implements RangedAttackMob
{
    private int attackCooldown;

    public SurvivorEntity(EntityType<? extends SurvivorEntity> type, Level level)
    {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.30D)
            .add(Attributes.FOLLOW_RANGE, 40.0D)
            .add(Attributes.ARMOR, 2.0D)
            .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.05D, 24, 28.0F));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void aiStep()
    {
        super.aiStep();
        if (attackCooldown > 0)
            attackCooldown--;
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity target, float distanceFactor)
    {
        if (attackCooldown > 0 || level().isClientSide() || !ModApocalypseConfig.apocalypseMobsEnabled())
            return;
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (ApocalypseGunHelper.shootLoadedGun(this, target))
            attackCooldown = 20 + getRandom().nextInt(16);
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
        ApocalypseGunHelper.randomLoadedGun(random, true).ifPresent(stack -> setItemSlot(EquipmentSlot.MAINHAND, stack));
        ApocalypseLoot.dressMob(this, random);
        setDropChance(EquipmentSlot.MAINHAND, 1.0F);
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull net.minecraft.server.level.ServerLevel level, @NotNull DamageSource source, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        ApocalypseLoot.dropSurvivorLoot(this);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return false;
    }
}
