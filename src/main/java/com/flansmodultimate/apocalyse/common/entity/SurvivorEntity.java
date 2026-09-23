package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.apocalyse.common.util.ApocalypseGunCombat;
import com.flansmodultimate.apocalyse.common.util.ApocalypseGunHelper;
import com.flansmodultimate.apocalyse.common.util.ApocalypseLoot;
import com.flansmodultimate.config.ModApocalypseConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

/**
 * An armed, hostile wasteland survivor, as the 1.12.2 {@code EntitySurvivor}.
 *
 * <p>The legacy survivor was a skeleton underneath: it hunted players and iron golems, and on top
 * of that animals and skull drones, and shot at them with a semi-automatic gun it reloaded from
 * two to five spare magazines. It never targets the Skull Boss, which in turn ignores AI gunfire.</p>
 */
public class SurvivorEntity extends Monster
{
    private static final int RESERVE_SLOTS = 5;
    /** The legacy {@code EntityAIAttackRangedGun} engagement range. */
    private static final float GUN_RANGE = 15.0F;

    private final ApocalypseGunCombat gun = new ApocalypseGunCombat(this, ApocalypseGunCombat.Profile.SURVIVOR, RESERVE_SLOTS);

    public SurvivorEntity(EntityType<? extends SurvivorEntity> type, Level level)
    {
        super(type, level);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.FOLLOW_RANGE, 80.0D)
            .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Wolf.class, 6.0F, 1.0D, 1.2D));
        goalSelector.addGoal(4, new GunAttackGoal(1.0D, GUN_RANGE));
        goalSelector.addGoal(4, new UnarmedAttackGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, true));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, SkullDroneEntity.class, true));
    }

    @Override
    public void aiStep()
    {
        super.aiStep();
        if (!level().isClientSide)
            gun.tick();
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation") // NeoForge marks this as override-only; external callers use EventHooks.
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData spawnData)
    {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        equipDefault(level.getRandom());
        return result;
    }

    public void equipDefault(RandomSource random)
    {
        ApocalypseGunHelper.randomLoadedGun(random, true).ifPresent(stack -> setItemSlot(EquipmentSlot.MAINHAND, stack));
        ApocalypseLoot.dressMob(this, random);
        gun.stockReserve(random, random.nextInt(4) + 2);
        setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            setDropChance(slot, 0.5F);
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

    /**
     * The skeleton bow AI the legacy survivor inherited, holding a gun: close to range, strafe
     * once the target has been in sight for a second, and fire after a one-second aim followed by
     * a cooldown of half a second on Hard and a second otherwise. The gun's own cadence and reloads
     * gate each trigger pull on top of that.
     */
    private final class GunAttackGoal extends Goal
    {
        private static final int AIM_TICKS = 20;

        private final double speedModifier;
        private final float attackRadiusSqr;
        private int attackTime = -1;
        private int seeTime;
        private int aimTicks = -1;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        private GunAttackGoal(double speedModifier, float attackRadius)
        {
            this.speedModifier = speedModifier;
            this.attackRadiusSqr = attackRadius * attackRadius;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse()
        {
            return getTarget() != null && gun.hasGun();
        }

        @Override
        public boolean canContinueToUse()
        {
            return (canUse() || !getNavigation().isDone()) && gun.hasGun();
        }

        @Override
        public void start()
        {
            setAggressive(true);
        }

        @Override
        public void stop()
        {
            setAggressive(false);
            seeTime = 0;
            attackTime = -1;
            aimTicks = -1;
        }

        @Override
        public boolean requiresUpdateEveryTick()
        {
            return true;
        }

        @Override
        public void tick()
        {
            LivingEntity target = getTarget();
            if (target == null)
                return;

            double distanceSqr = distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean canSee = getSensing().hasLineOfSight(target);
            if (canSee != seeTime > 0)
                seeTime = 0;
            seeTime += canSee ? 1 : -1;

            if (distanceSqr <= attackRadiusSqr && seeTime >= 20)
            {
                getNavigation().stop();
                strafingTime++;
            }
            else
            {
                getNavigation().moveTo(target, speedModifier);
                strafingTime = -1;
            }

            if (strafingTime >= 20)
            {
                if (getRandom().nextFloat() < 0.3F)
                    strafingClockwise = !strafingClockwise;
                if (getRandom().nextFloat() < 0.3F)
                    strafingBackwards = !strafingBackwards;
                strafingTime = 0;
            }

            if (strafingTime > -1)
            {
                if (distanceSqr > attackRadiusSqr * 0.75F)
                    strafingBackwards = false;
                else if (distanceSqr < attackRadiusSqr * 0.25F)
                    strafingBackwards = true;
                getMoveControl().strafe(strafingBackwards ? -0.5F : 0.5F, strafingClockwise ? 0.5F : -0.5F);
                lookAt(target, 30.0F, 30.0F);
            }
            else
            {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            if (aimTicks >= 0)
            {
                if (!canSee && seeTime < -60)
                {
                    aimTicks = -1;
                }
                else if (canSee && ++aimTicks >= AIM_TICKS)
                {
                    aimTicks = -1;
                    if (ModApocalypseConfig.apocalypseMobsEnabled())
                        gun.tryFire(target);
                    attackTime = level().getDifficulty() == Difficulty.HARD ? 10 : 20;
                }
            }
            else if (--attackTime <= 0 && seeTime >= -60)
            {
                aimTicks = 0;
            }
        }
    }

    /** The legacy fallback when a survivor has no gun in hand: a plain melee attack. */
    private final class UnarmedAttackGoal extends MeleeAttackGoal
    {
        private UnarmedAttackGoal()
        {
            super(SurvivorEntity.this, 1.2D, false);
        }

        @Override
        public boolean canUse()
        {
            return !gun.hasGun() && super.canUse();
        }

        @Override
        public boolean canContinueToUse()
        {
            return !gun.hasGun() && super.canContinueToUse();
        }
    }
}
