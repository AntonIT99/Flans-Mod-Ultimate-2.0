package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.FlansDriveables;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcSight;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Human limits on how an NPC fights: how long it takes to open fire on a target it has just seen, how far its first rounds
 * go astray until its aim settles, how much of a moving target's run it allows for, and how hard it hits players. Every NPC
 * has its own, set in the Wolff's Mod tab, which by default put no limit on it. A Teams bot takes those of its battle's
 * difficulty instead, kept in its persistent data until it leaves the battle. Server thread.
 *
 * @param reactionTicks
 *            ticks between first seeing a target and the first shot at it
 * @param settleTicks
 *            ticks, from the first shot on, over which the extra aim error dies away
 * @param startError
 *            greatest extra aim error, in degrees, of the first shot
 * @param lead
 *            share, 0 to 1, of a moving target's run the NPC allows for
 * @param playerDamage
 *            factor on the damage the NPC deals to players
 * @param nonLethal
 *            whether the NPC's hits leave a player at least half a heart
 */
public record NpcSkill(int reactionTicks, int settleTicks, float startError, float lead, float playerDamage, boolean nonLethal)
{
    /** No limits at all. */
    public static final NpcSkill NONE = new NpcSkill(0, 0, 0F, 1F, 1F, false);

    /** Key of a battle's limits in the NPC's persistent data. */
    private static final String KEY = "WolffsModSkill";
    private static final String NBT_REACTION = "reaction";
    private static final String NBT_SETTLE = "settle";
    private static final String NBT_ERROR = "error";
    private static final String NBT_LEAD = "lead";
    private static final String NBT_PLAYER_DAMAGE = "player_damage";
    private static final String NBT_NON_LETHAL = "non_lethal";
    /** Ticks out of sight after which a target counts as newly seen again. */
    private static final int FORGET_TICKS = 60;
    /** Sight is checked every few ticks only; it costs a ray trace through the blocks. */
    private static final int WATCH_INTERVAL = 4;
    /** The least health, in hit points, a non-lethal NPC leaves a player. */
    private static final float SPARED_HEALTH = 1F;
    /** Steps of the aim error setting per degree, and of the share settings per whole. */
    private static final float ERROR_STEPS = 10F;
    private static final float PERCENT = 100F;
    /**
     * Ticks a hand-laid turret holds one offset before it takes the next. A gun that is laid rather than fired round by
     * round is pointed every tick, so a fresh error each time would only shake it; held for a while, the same error reads
     * as a gunner who is still correcting.
     */
    private static final int DRIFT_TICKS = 10;
    /** What each limited NPC knows about its current target. */
    private static final Map<Entity, Engagement> ENGAGEMENTS = new WeakHashMap<>();

    /**
     * Ready-made limits, from none at all down to those of a trainee: the Teams difficulties, and what an NPC's own limits
     * can be filled in from. Saves hold the index, so a new level goes at the end.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Level
    {
        // spotless:off
        /** Fills nothing in: the limits stand as they were set. */
        CUSTOM(NONE),
        /** Fights to the full potential of its equipment, without any human limit: it fires the moment it sees a target. */
        ACE(NONE),
        /** A human's reaction: about as hard as a skilled player. */
        ELITE(new NpcSkill(5, 12, 3F, 0.95F, 1F, false)),
        VETERAN(new NpcSkill(10, 24, 5F, 0.85F, 1F, false)),
        REGULAR(new NpcSkill(16, 36, 8F, 0.7F, 1F, false)),
        /** Below Regular, hits on players deal less damage: half of it for a recruit. */
        RECRUIT(new NpcSkill(24, 50, 11F, 0.5F, 0.5F, false)),
        /** Shoots a lot and rarely hits, and deals a quarter of its damage to players. */
        TRAINEE(new NpcSkill(34, 70, 14F, 0.3F, 0.25F, false));
        // spotless:on

        private final NpcSkill skill;
    }

    /** The extra error of one trigger pull, in degrees, shared by every round it fires so that pellets stay together. */
    public record AimError(float yaw, float pitch)
    {
        public static final AimError NONE = new AimError(0F, 0F);

        /** @return an error of up to the given size in degrees, uniform over its disc, so that most rounds land near the target rather than on its edge */
        public static AimError random(RandomSource random, float size)
        {
            if (size <= 0F)
                return NONE;
            double spread = size * Math.sqrt(random.nextDouble());
            double angle = random.nextDouble() * Math.PI * 2D;
            return new AimError((float) (spread * Math.cos(angle)), (float) (spread * Math.sin(angle)));
        }

        /** @return this error with another added, as a gun that is both unsettled and unsteady carries both */
        public AimError plus(AimError other)
        {
            if (other.none())
                return this;
            return none() ? other : new AimError(yaw + other.yaw, pitch + other.pitch);
        }

        /** @return whether the aim carries no error at all, and so leaves a direction as it is */
        public boolean none()
        {
            return yaw == 0F && pitch == 0F;
        }

        /** @return the direction turned by this error, keeping its length */
        public Vec3 apply(Vec3 direction)
        {
            double length = direction.length();
            if (none() || length < 1.0E-6D)
                return direction;
            double yawRadians = Math.atan2(direction.x, direction.z) + Math.toRadians(yaw);
            double pitchRadians = Mth.clamp(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)) + Math.toRadians(pitch), -Math.PI / 2D, Math.PI / 2D);
            double horizontal = Math.cos(pitchRadians);
            return new Vec3(horizontal * Math.sin(yawRadians), Math.sin(pitchRadians), horizontal * Math.cos(yawRadians)).scale(length);
        }
    }

    private static final class Engagement
    {
        private final UUID target;
        private final long since;
        private long seen;

        private Engagement(UUID target, long now)
        {
            this.target = target;
            since = now;
            seen = now;
        }
    }

    /** Gives the entity a battle's limits, which come before its own, even when they are none, until they are cleared. */
    public static void write(Entity entity, NpcSkill skill)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_REACTION, skill.reactionTicks());
        tag.putInt(NBT_SETTLE, skill.settleTicks());
        tag.putFloat(NBT_ERROR, skill.startError());
        tag.putFloat(NBT_LEAD, skill.lead());
        tag.putFloat(NBT_PLAYER_DAMAGE, skill.playerDamage());
        tag.putBoolean(NBT_NON_LETHAL, skill.nonLethal());
        entity.getPersistentData().put(KEY, tag);
    }

    /** Takes a battle's limits off the entity, which then fights within its own again. */
    public static void clear(Entity entity)
    {
        entity.getPersistentData().remove(KEY);
        ENGAGEMENTS.remove(entity);
    }

    /** @return the limits on how the entity fights: its battle's, or else an NPC's own; {@link #NONE} for anyone else */
    public static NpcSkill of(Entity entity)
    {
        // A real entity always carries persistent data; a bare test double, as {@link #own} also allows for, carries none.
        CompoundTag data = entity.getPersistentData();
        if (data == null || !data.contains(KEY, Tag.TAG_COMPOUND))
            return entity instanceof EntityNPCInterface npc ? own(npc) : NONE;
        CompoundTag tag = data.getCompound(KEY);
        return new NpcSkill(Math.max(0, tag.getInt(NBT_REACTION)), Math.max(0, tag.getInt(NBT_SETTLE)), Math.max(0F, tag.getFloat(NBT_ERROR)), Mth.clamp(tag.getFloat(NBT_LEAD), 0F, 1F),
            Math.max(0F, tag.getFloat(NBT_PLAYER_DAMAGE)), tag.getBoolean(NBT_NON_LETHAL));
    }

    /** @return the limits the NPC's own settings put on how it fights; none for an NPC built without the settings, as a bare test double is */
    public static NpcSkill own(EntityNPCInterface npc)
    {
        if (!(npc.stats instanceof NpcWeaponSettings settings))
            return NONE;
        NpcTuning tuning = settings.wolffsmodnpcsTuning();
        return new NpcSkill(tuning.get(Value.REACTION_TIME), tuning.get(Value.AIM_SETTLE_TIME), tuning.get(Value.AIM_ERROR) / ERROR_STEPS, tuning.get(Value.LEAD_SHARE) / PERCENT,
            tuning.get(Value.PLAYER_DAMAGE) / PERCENT, settings.wolffsmodnpcsWeaponOptions().enabled(Feature.NON_LETHAL));
    }

    /** Fills the NPC's own limits in from a level and notes the level; {@link Level#CUSTOM} keeps the values as they are. */
    public static void apply(EntityNPCInterface npc, Level level)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        tuning.set(Value.SKILL_LEVEL, level.ordinal());
        if (level == Level.CUSTOM)
            return;
        NpcSkill skill = level.getSkill();
        tuning.set(Value.REACTION_TIME, skill.reactionTicks());
        tuning.set(Value.AIM_SETTLE_TIME, skill.settleTicks());
        tuning.set(Value.AIM_ERROR, Math.round(skill.startError() * ERROR_STEPS));
        tuning.set(Value.LEAD_SHARE, Math.round(skill.lead() * PERCENT));
        tuning.set(Value.PLAYER_DAMAGE, Math.round(skill.playerDamage() * PERCENT));
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(Feature.NON_LETHAL, skill.nonLethal());
    }

    /** Notes, every few ticks, whether a limited NPC still sees its target, so that a target it lost sight of has to be found again. */
    public static void watch(EntityNPCInterface npc)
    {
        if ((npc.tickCount + npc.getId()) % WATCH_INTERVAL != 0 || of(npc).equals(NONE))
            return;
        LivingEntity target = npc.getTarget();
        if (target != null && target.isAlive() && NpcSight.sees(npc, target))
            engage(npc, target);
    }

    /** @return whether the NPC holds its fire because it has not yet reacted to its target; never for an NPC without limits or without a target */
    public static boolean holdsFire(EntityNPCInterface npc, @Nullable LivingEntity target)
    {
        NpcSkill skill = of(npc);
        return target != null && skill.reactionTicks() > 0 && npc.level().getGameTime() - engage(npc, target).since < skill.reactionTicks();
    }

    /** @return the extra error of the NPC's next trigger pull at the target, which shrinks as its aim settles */
    public static AimError aimError(EntityNPCInterface npc, LivingEntity target)
    {
        NpcSkill skill = of(npc);
        float left = unsettled(npc, target, skill);
        return AimError.random(npc.getRandom(), skill.startError() * left);
    }

    /**
     * The error of a gun that is laid at a target rather than fired round by round, such as the weapon of a driveable an
     * NPC drives, whose rounds leave along a barrel the NPC can only point. It holds each offset for a while, so the
     * barrel drifts onto the target instead of shaking around it.
     *
     * @return the error the NPC's aim carries at the target now, which shrinks as its aim settles
     */
    public static AimError layingError(EntityNPCInterface npc, LivingEntity target)
    {
        NpcSkill skill = of(npc);
        float left = unsettled(npc, target, skill);
        if (left <= 0F)
            return AimError.NONE;
        long window = npc.level().getGameTime() / DRIFT_TICKS;
        return AimError.random(RandomSource.create(window * 31L + (long) npc.getId() * 17L + target.getId()), skill.startError() * left);
    }

    /** @return the share of its starting aim error the NPC has not yet settled out at the target; 0 once it aims true */
    private static float unsettled(EntityNPCInterface npc, LivingEntity target, NpcSkill skill)
    {
        if (skill.startError() <= 0F)
            return 0F;
        long aimed = npc.level().getGameTime() - engage(npc, target).since - skill.reactionTicks();
        return skill.settleTicks() <= 0 ? 0F : 1F - Mth.clamp((float) aimed / skill.settleTicks(), 0F, 1F);
    }

    /** @return the share, 0 to 1, of a moving target's run the NPC allows for */
    public static float leadShare(Entity npc)
    {
        return of(npc).lead();
    }

    /**
     * Aims an NPC that allows for only part of a moving target's run between the full lead and where the target stands,
     * so that its rounds fall behind a crossing target as a hurried gunner's do.
     *
     * @param still
     *            the direction to fire in to hit the target where it stands
     * @param led
     *            the direction to fire in to meet it where it is going
     * @return the direction the NPC actually fires in, of the same length as {@code led}
     */
    public static Vec3 partialLead(Entity npc, Vec3 still, Vec3 led)
    {
        return partialLead(leadShare(npc), still, led);
    }

    /** @see #partialLead(Entity, Vec3, Vec3) */
    public static Vec3 partialLead(float share, Vec3 still, Vec3 led)
    {
        if (share >= 1F)
            return led;
        if (share <= 0F)
            return still;
        return still.lerp(led, share).normalize().scale(led.length());
    }

    /**
     * @return the damage a player takes from a hit, after armour, as the limits of the NPC behind it allow
     */
    public static float damageTo(LivingEntity victim, DamageSource source, float amount)
    {
        EntityNPCInterface npc = responsible(source.getEntity());
        if (!(victim instanceof Player) || npc == null)
            return amount;
        NpcSkill skill = of(npc);
        float dealt = amount * skill.playerDamage();
        return skill.nonLethal() ? Math.min(dealt, Math.max(0F, victim.getHealth() - SPARED_HEALTH)) : dealt;
    }

    /**
     * @return the NPC whose limits a hit answers to: the shooter itself, or, for a round a driveable fired, the NPC at its
     *         wheel, which is the entity credited with the shot; null when no NPC is behind it
     */
    @Nullable
    private static EntityNPCInterface responsible(@Nullable Entity source)
    {
        if (source instanceof EntityNPCInterface npc)
            return npc;
        return source == null ? null : FlansDriveables.getDriver(source).filter(EntityNPCInterface.class::isInstance).map(EntityNPCInterface.class::cast).orElse(null);
    }

    /** Forgets every engagement, as the server stops. */
    public static void forget()
    {
        ENGAGEMENTS.clear();
    }

    private static Engagement engage(EntityNPCInterface npc, LivingEntity target)
    {
        long now = npc.level().getGameTime();
        Engagement engagement = ENGAGEMENTS.get(npc);
        if (engagement == null || !engagement.target.equals(target.getUUID()) || now - engagement.seen > FORGET_TICKS)
        {
            engagement = new Engagement(target.getUUID(), now);
            ENGAGEMENTS.put(npc, engagement);
        }
        engagement.seen = now;
        return engagement;
    }
}
