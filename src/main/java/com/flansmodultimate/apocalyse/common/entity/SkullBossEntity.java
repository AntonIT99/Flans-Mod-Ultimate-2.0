package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.config.ModCommonConfig;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.MobSpawnType;
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

public class SkullBossEntity extends Monster
{
    private static final int IDLE_TICKS = 20;
    private static final int ACTION_TICKS = 80;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.flansmodapocalypse.skullboss"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
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

        if (level().isClientSide || !ModCommonConfig.apocalypseMobsEnabled())
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

        tickAction(target);
        if (actionTicks >= ACTION_TICKS)
            switchAction(Action.IDLE);
    }

    private Action chooseAction()
    {
        int bound = ModCommonConfig.apocalypseNukeDropsEnabled() ? 4 : 3;
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

    private void tickAction(LivingEntity target)
    {
        switch (currentAction)
        {
            case LAUGH -> tickLaugh();
            case SPAWN_DRONES -> {
                if (actionTicks == 2)
                    spawnDrones();
            }
            case SHOOT_TNT -> {
                if (actionTicks % 20 == 0)
                    shootTnt(target);
            }
            case DROP_NUKE -> {
                if (actionTicks == 2)
                    callNukeDrop(target);
            }
            case IDLE -> {
                // no-op
            }
        }
    }

    private void tickLaugh()
    {
        if (actionTicks == 2)
            playSound(resolveSound("skullboss_laugh", SoundEvents.WITHER_AMBIENT), 8.0F, 0.8F + random.nextFloat() * 0.4F);

        if (actionTicks % 5 == 0 && level() instanceof ServerLevel serverLevel)
        {
            serverLevel.explode(this, getX() + random.nextGaussian() * 10.0D, getY() + random.nextGaussian() * 10.0D, getZ() + random.nextGaussian() * 10.0D, 2.0F, false, Level.ExplosionInteraction.NONE);
        }
    }

    private void spawnDrones()
    {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        playSound(resolveSound("skullboss_spawn", SoundEvents.WITHER_SPAWN), 8.0F, 1.0F);
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++)
        {
            SkullDroneEntity drone = ApocalypseContent.skullDrone.get().create(serverLevel);
            if (drone == null)
                continue;
            drone.moveTo(getX() + random.nextGaussian() * 4.0D, getY() - 2.0D + random.nextDouble() * 4.0D, getZ() + random.nextGaussian() * 4.0D, random.nextFloat() * 360.0F, 0.0F);
            drone.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(drone.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            serverLevel.addFreshEntity(drone);
        }
    }

    private void shootTnt(LivingEntity target)
    {
        if (!(level() instanceof ServerLevel serverLevel))
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

    private void callNukeDrop(LivingEntity target)
    {
        if (!(level() instanceof ServerLevel serverLevel) || !ModCommonConfig.apocalypseNukeDropsEnabled())
            return;
        NukeDropEntity nuke = new NukeDropEntity(ApocalypseContent.nukeDrop.get(), serverLevel);
        nuke.moveTo(target.getX(), Math.min(serverLevel.getMaxBuildHeight() - 4.0D, target.getY() + 48.0D), target.getZ(), 0.0F, 0.0F);
        serverLevel.addFreshEntity(nuke);
    }

    private static SoundEvent resolveSound(String name, SoundEvent fallback)
    {
        return FlansMod.getSoundEvent(name).map(RegistryObject::get).orElse(fallback);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
            return false;
        return super.hurt(source, Math.min(amount, 99.0F));
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(Items.GUNPOWDER, 16 + random.nextInt(16)));
        if (random.nextBoolean())
            spawnAtLocation(new ItemStack(Items.TOTEM_OF_UNDYING));
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
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source)
    {
        return false;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        currentAction = Action.byId(tag.getInt("ApocalypseAction"));
        actionTicks = tag.getInt("ApocalypseActionTicks");
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt("ApocalypseAction", currentAction.ordinal());
        tag.putInt("ApocalypseActionTicks", actionTicks);
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
