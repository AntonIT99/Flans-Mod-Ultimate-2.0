package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.config.ModApocalypseConfig;
import lombok.EqualsAndHashCode;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SkullBossEntity extends Monster
{
    private static final String NBT_ACTION = "apocalypse_action";
    private static final String NBT_ACTION_TICKS = "apocalypse_action_ticks";
    private static final int IDLE_TICKS = 20;
    private static final int ACTION_TICKS = 80;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), Component.translatable("entity.flansmodapocalypse.skullboss"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private Action currentAction = Action.IDLE;
    private int actionTicks;

    public SkullBossEntity(EntityType<? extends SkullBossEntity> type, Level level)
    {
        super(type, level);
        setNoGravity(true);
        xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1000.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.FLYING_SPEED, 0.35D)
            .add(Attributes.FOLLOW_RANGE, 128.0D)
            .add(Attributes.ARMOR, 12.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.ATTACK_DAMAGE, 12.0D);
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 64.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick()
    {
        setNoGravity(true);
        super.tick();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        Level level = level();

        if (level.isClientSide() || !ModApocalypseConfig.apocalypseMobsEnabled())
            return;

        actionTicks++;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive())
        {
            Vec3 hover = target.position().add(0.0D, 18.0D, 0.0D).subtract(position());
            if (hover.lengthSqr() > 4.0D)
                setDeltaMovement(getDeltaMovement().scale(0.80D).add(hover.normalize().scale(0.08D)));
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        else
        {
            setDeltaMovement(getDeltaMovement().scale(0.85D));
        }

        if (currentAction == Action.IDLE)
        {
            if (target != null && actionTicks >= IDLE_TICKS)
                switchAction(chooseAction());
            return;
        }

        if (target == null || !target.isAlive())
        {
            switchAction(Action.IDLE);
            return;
        }

        tickAction(level, target);
        if (actionTicks >= ACTION_TICKS)
            switchAction(Action.IDLE);
    }

    private Action chooseAction()
    {
        int bound = ModApocalypseConfig.apocalypseNukeDropsEnabled() ? 4 : 3;
        return switch (random.nextInt(bound))
        {
            case 0 -> Action.LAUGH;
            case 1 -> Action.SPAWN_DRONES;
            case 2 -> Action.SHOOT_TNT;
            default -> Action.DROP_NUKE;
        };
    }

    private void switchAction(Action action)
    {
        currentAction = action;
        actionTicks = 0;
    }

    private void tickAction(Level level, LivingEntity target)
    {
        switch (currentAction)
        {
            case LAUGH -> tickLaugh(level);
            case SPAWN_DRONES -> {
                if (actionTicks == 2)
                    spawnDrones(level);
            }
            case SHOOT_TNT -> {
                if (actionTicks % 20 == 0)
                    shootTnt(level, target);
            }
            case DROP_NUKE -> {
                if (actionTicks == 2)
                    callNukeDrop(level, target);
            }
            case IDLE -> {
                // no-op
            }
        }
    }

    private void tickLaugh(Level level)
    {
        if (actionTicks == 2)
            playSound(resolveSound("skullboss_laugh", SoundEvents.WITHER_AMBIENT), 8.0F, 0.8F + random.nextFloat() * 0.4F);

        if (actionTicks % 5 == 0 && level instanceof ServerLevel serverLevel)
        {
            serverLevel.explode(this, getX() + random.nextGaussian() * 10.0D, getY() + random.nextGaussian() * 10.0D, getZ() + random.nextGaussian() * 10.0D, 2.0F, false, Level.ExplosionInteraction.NONE);
        }
    }

    private void spawnDrones(Level level)
    {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        playSound(resolveSound("skullboss_spawn", SoundEvents.WITHER_SPAWN), 8.0F, 1.0F);
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++)
        {
            SkullDroneEntity drone = ApocalypseContent.skullDrone.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (drone == null)
                continue;
            drone.snapTo(getX() + random.nextGaussian() * 4.0D, getY() - 2.0D + random.nextDouble() * 4.0D, getZ() + random.nextGaussian() * 4.0D, random.nextFloat() * 360.0F, 0.0F);
            drone.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(drone.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(drone);
        }
    }

    private void shootTnt(Level level, LivingEntity target)
    {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        Vec3 direction = target.getEyePosition().subtract(getEyePosition());
        if (direction.lengthSqr() < 0.0001D)
            return;

        Vec3 normalized = direction.normalize();
        PrimedTnt tnt = new PrimedTnt(serverLevel, getX() + normalized.x * 2.0D, getY() + getBbHeight() * 0.5D + normalized.y * 2.0D, getZ() + normalized.z * 2.0D, this);
        tnt.setNoGravity(true);
        tnt.setDeltaMovement(direction.scale(1.0D / 40.0D));
        serverLevel.addFreshEntity(tnt);
        playSound(SoundEvents.FLINTANDSTEEL_USE, 6.0F, 1.0F);
    }

    private void callNukeDrop(Level level, LivingEntity target)
    {
        if (!(level instanceof ServerLevel serverLevel) || !ModApocalypseConfig.apocalypseNukeDropsEnabled())
            return;

        NukeDropEntity nuke = new NukeDropEntity(ApocalypseContent.nukeDrop.get(), serverLevel);
        nuke.snapTo(target.getX(), Math.min(serverLevel.getMaxY() - 4.0D, target.getY() + 48.0D), target.getZ(), 0.0F, 0.0F);
        serverLevel.addFreshEntity(nuke);
    }

    private static SoundEvent resolveSound(String name, SoundEvent fallback)
    {
        return FlansMod.getSoundEvent(name).map(DeferredHolder::get).orElse(fallback);
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel serverLevel, @NotNull DamageSource source, float amount)
    {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
            return false;

        return super.hurtServer(serverLevel, source, Math.min(amount, 99.0F));
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull net.minecraft.server.level.ServerLevel level, @NotNull DamageSource source, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(level, new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(3)));
        spawnAtLocation(level, new ItemStack(Items.GUNPOWDER, 16 + random.nextInt(16)));
        if (random.nextBoolean())
            spawnAtLocation(level, new ItemStack(Items.TOTEM_OF_UNDYING));
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player)
    {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player)
    {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, @NotNull DamageSource source)
    {
        return false;
    }

    @Override
    public void readAdditionalSaveData(@NotNull ValueInput input)
    {
        super.readAdditionalSaveData(input);
        currentAction = Action.byId(input.getIntOr(NBT_ACTION, 0));
        actionTicks = input.getIntOr(NBT_ACTION_TICKS, 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output)
    {
        super.addAdditionalSaveData(output);
        output.putInt(NBT_ACTION, currentAction.ordinal());
        output.putInt(NBT_ACTION_TICKS, actionTicks);
    }

    private enum Action
    {
        IDLE,
        LAUGH,
        SPAWN_DRONES,
        SHOOT_TNT,
        DROP_NUKE;

        private static Action byId(int id)
        {
            Action[] values = values();
            if (id < 0 || id >= values.length)
                return IDLE;
            return values[id];
        }
    }
}
