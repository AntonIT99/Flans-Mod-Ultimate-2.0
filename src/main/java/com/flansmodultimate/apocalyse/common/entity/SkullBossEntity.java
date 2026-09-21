package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.config.ModApocalypseConfig;
import lombok.EqualsAndHashCode;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Skull Boss, as the 1.12.2 {@code EntitySkullBoss}.
 *
 * <p>It does not chase anyone. It swings around its arena on a spring pulled towards its home,
 * the origin above the boss pillars for a boss summoned there, and bobs between that height
 * and 80 blocks below it on a slow sine. It fights whoever summoned it and switches to
 * whoever last hurt it. Its laughs, drones and TNT carry on whether or not it has a target.</p>
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SkullBossEntity extends Monster
{
    private static final String NBT_ACTION = "apocalypse_action";
    private static final String NBT_ACTION_TICKS = "apocalypse_action_ticks";
    private static final String NBT_HOME = "apocalypse_home";
    private static final int IDLE_TICKS = 20;
    private static final int ACTION_TICKS = 80;
    private static final float LAUGH_EXPLOSION_POWER = 10.0F;
    /** The 1.12.2 lerp speed of 0.1 spread over twenty ticks. */
    private static final double HOME_PULL = 0.1D / 20D;
    /** The boss hovers this far below its spawn point on average: 180 for a spawn at y=220. */
    private static final double HOVER_DROP = 40.0D;
    private static final double HOVER_SWING = 40.0D;
    /** Not in 1.12.2: a boss whose target is gone looks for the nearest player in this range. */
    private static final double REACQUIRE_RANGE = 128.0D;
    private static final float MAX_DAMAGE = 99.0F;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.flansmodapocalypse.skullboss"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private Action currentAction = Action.IDLE;
    private int actionTicks;
    @Nullable
    private Vec3 home;

    public SkullBossEntity(EntityType<? extends SkullBossEntity> type, Level level)
    {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 5000;
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1024.0D)
            .add(Attributes.FLYING_SPEED, 0.35D)
            .add(Attributes.FOLLOW_RANGE, REACQUIRE_RANGE)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.ATTACK_DAMAGE, 12.0D);
    }

    @Override
    protected void registerGoals()
    {
        // Deliberately none: movement, targeting and attacks are the legacy state machine in tick().
    }

    /** The point the boss circles and hovers below. */
    public void setHome(Vec3 home)
    {
        this.home = home;
    }

    @Override
    public void tick()
    {
        setNoGravity(true);
        super.tick();
        fallDistance = 0F;
        bossEvent.setProgress(getHealth() / getMaxHealth());
        Level level = level();

        if (level.isClientSide || !ModApocalypseConfig.apocalypseMobsEnabled())
            return;

        if (home == null)
            home = position();
        Vec3 motion = getDeltaMovement();
        double hoverY = home.y - HOVER_DROP + Math.sin(tickCount / 200.0D) * HOVER_SWING;
        motion = new Vec3(motion.x - (getX() - home.x) * HOME_PULL, (hoverY - getY()) * HOME_PULL, motion.z - (getZ() - home.z) * HOME_PULL);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);

        LivingEntity target = validTarget();
        if (target != null)
            turnTowards(target);

        actionTicks++;
        switch (currentAction)
        {
            case IDLE -> {
                if (actionTicks >= IDLE_TICKS)
                    switchAction(chooseAction());
            }
            case LAUGH -> tickLaugh(level);
            case SPAWN_DRONES -> {
                if (actionTicks == 2)
                    spawnDrone(level, target);
            }
            case SHOOT_TNT -> {
                if (actionTicks % 20 == 0 && target != null)
                    shootTnt(level, target);
            }
            case DROP_NUKE -> {
                if (actionTicks == 2 && target != null)
                    callNukeDrop(level, target);
            }
        }
        if (currentAction != Action.IDLE && actionTicks >= ACTION_TICKS)
            switchAction(Action.IDLE);
    }

    /**
     * The summoner or last attacker. Unlike 1.12.2, once they are gone the boss takes on the
     * nearest player instead of floating on without anyone to aim its TNT at.
     */
    @Nullable
    private LivingEntity validTarget()
    {
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target.isRemoved() || target.level() != level()))
        {
            setTarget(null);
            target = null;
        }
        if (target == null && tickCount % 20 == 0)
        {
            target = level().getNearestPlayer(TargetingConditions.forCombat().range(REACQUIRE_RANGE)
                .selector(EntitySelector.NO_CREATIVE_OR_SPECTATOR::test).ignoreLineOfSight(), this);
            setTarget(target);
        }
        return target;
    }

    private void turnTowards(LivingEntity target)
    {
        double dX = target.getX() - getX();
        double dY = target.getY() - getY();
        double dZ = target.getZ() - getZ();
        float targetYaw = (float) (Mth.atan2(dZ, dX) * Mth.RAD_TO_DEG) - 90F;
        float targetPitch = (float) (-Mth.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)) * Mth.RAD_TO_DEG);
        setYRot(getYRot() + Mth.wrapDegrees(targetYaw - getYRot()) / 20F);
        setXRot(getXRot() + (targetPitch - getXRot()) / 20F);
        setYHeadRot(getYRot());
        yBodyRot = getYRot();
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

    private void tickLaugh(Level level)
    {
        if (actionTicks == 2)
            playSound(resolveSound("skullboss_laugh", SoundEvents.WITHER_AMBIENT), 8.0F, 0.8F + random.nextFloat() * 0.4F);

        if (actionTicks % 5 == 0 && level instanceof ServerLevel serverLevel)
        {
            serverLevel.explode(this, getX() + random.nextGaussian() * 10.0D, getY() + random.nextGaussian() * 10.0D,
                getZ() + random.nextGaussian() * 10.0D, LAUGH_EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE);
        }
    }

    /** One armed drone five blocks below the boss, sent after the boss's target. */
    private void spawnDrone(Level level, @Nullable LivingEntity target)
    {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        playSound(resolveSound("skullboss_spawn", SoundEvents.WITHER_SPAWN), 8.0F, 1.0F);
        SkullDroneEntity drone = ApocalypseContent.skullDrone.get().create(serverLevel);
        if (drone == null)
            return;
        drone.moveTo(getX(), getY() - 5.0D, getZ(), random.nextFloat() * 360.0F, 0.0F);
        drone.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(drone.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        if (target != null)
            drone.setTarget(target);
        serverLevel.addFreshEntity(drone);
    }

    private void shootTnt(Level level, LivingEntity target)
    {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        Vec3 direction = target.position().subtract(position());
        if (direction.lengthSqr() < 0.0001D)
            return;

        Vec3 offset = direction.normalize().scale(2.0D);
        PrimedTnt tnt = new PrimedTnt(serverLevel, getX() + offset.x, getY() + offset.y, getZ() + offset.z, this);
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
        nuke.moveTo(target.getX(), Math.min(serverLevel.getMaxBuildHeight() - 4.0D, target.getY() + 48.0D), target.getZ(), 0.0F, 0.0F);
        serverLevel.addFreshEntity(nuke);
    }

    private static SoundEvent resolveSound(String name, SoundEvent fallback)
    {
        return FlansMod.getSoundEvent(name).map(RegistryObject::get).orElse(fallback);
    }

    /**
     * Immune to explosions and fire, to its drones, and to Flan's weapons with no player behind
     * them, so survivors and AI mechas cannot wear it down. Damage is capped at 99 and then
     * halved on Normal and quartered on Hard, as in 1.12.2. Whoever hurts it becomes its target.
     */
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
            return false;
        if (source.getEntity() instanceof SkullDroneEntity || (isFlanDamage(source) && !(source.getEntity() instanceof Player)))
            return false;

        float scaled = Math.min(amount, MAX_DAMAGE) * switch (level().getDifficulty())
        {
            case HARD -> 0.25F;
            case NORMAL -> 0.5F;
            default -> 1.0F;
        };
        boolean hurt = super.hurt(source, scaled);
        if (!level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker != this)
            setTarget(attacker);
        return hurt;
    }

    private static boolean isFlanDamage(DamageSource source)
    {
        return source.is(FlanDamageSources.SHOOTABLE) || source.is(FlanDamageSources.HEADSHOT)
            || source.is(FlanDamageSources.MELEE) || source.is(FlanDamageSources.EXPLOSION);
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
        currentAction = Action.byId(tag.getInt(NBT_ACTION));
        actionTicks = tag.getInt(NBT_ACTION_TICKS);
        if (tag.contains(NBT_HOME, CompoundTag.TAG_COMPOUND))
        {
            CompoundTag homeTag = tag.getCompound(NBT_HOME);
            home = new Vec3(homeTag.getDouble("x"), homeTag.getDouble("y"), homeTag.getDouble("z"));
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_ACTION, currentAction.ordinal());
        tag.putInt(NBT_ACTION_TICKS, actionTicks);
        if (home != null)
        {
            CompoundTag homeTag = new CompoundTag();
            homeTag.putDouble("x", home.x);
            homeTag.putDouble("y", home.y);
            homeTag.putDouble("z", home.z);
            tag.put(NBT_HOME, homeTag);
        }
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
