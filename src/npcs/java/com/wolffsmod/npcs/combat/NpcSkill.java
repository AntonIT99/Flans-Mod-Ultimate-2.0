package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.sim.NpcSight;
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
 * Human limits on how an NPC fights, which Teams bots take from their round's difficulty: how long it takes to open fire on
 * a target it has just seen, how far its first rounds go astray until its aim settles, how much of a moving target's run it
 * allows for, and how hard it hits players. Kept in the NPC's persistent data; an NPC without them fights without limits.
 * Server thread.
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

    /** Key of the limits in the NPC's persistent data. */
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
    /** What each limited NPC knows about its current target. */
    private static final Map<Entity, Engagement> ENGAGEMENTS = new WeakHashMap<>();

    /** The extra error of one trigger pull, in degrees, shared by every round it fires so that pellets stay together. */
    public record AimError(float yaw, float pitch)
    {
        public static final AimError NONE = new AimError(0F, 0F);

        /** @return the direction turned by this error, keeping its length */
        public Vec3 apply(Vec3 direction)
        {
            double length = direction.length();
            if (this == NONE || length < 1.0E-6D)
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

    public static void write(Entity entity, NpcSkill skill)
    {
        if (skill.equals(NONE))
        {
            clear(entity);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_REACTION, skill.reactionTicks());
        tag.putInt(NBT_SETTLE, skill.settleTicks());
        tag.putFloat(NBT_ERROR, skill.startError());
        tag.putFloat(NBT_LEAD, skill.lead());
        tag.putFloat(NBT_PLAYER_DAMAGE, skill.playerDamage());
        tag.putBoolean(NBT_NON_LETHAL, skill.nonLethal());
        entity.getPersistentData().put(KEY, tag);
    }

    public static void clear(Entity entity)
    {
        entity.getPersistentData().remove(KEY);
        ENGAGEMENTS.remove(entity);
    }

    /** @return the limits on how the entity fights; {@link #NONE} for one without any */
    public static NpcSkill of(Entity entity)
    {
        if (!entity.getPersistentData().contains(KEY, Tag.TAG_COMPOUND))
            return NONE;
        CompoundTag tag = entity.getPersistentData().getCompound(KEY);
        return new NpcSkill(Math.max(0, tag.getInt(NBT_REACTION)), Math.max(0, tag.getInt(NBT_SETTLE)), Math.max(0F, tag.getFloat(NBT_ERROR)), Mth.clamp(tag.getFloat(NBT_LEAD), 0F, 1F),
            Math.max(0F, tag.getFloat(NBT_PLAYER_DAMAGE)), tag.getBoolean(NBT_NON_LETHAL));
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
        if (skill.startError() <= 0F)
            return AimError.NONE;
        long aimed = npc.level().getGameTime() - engage(npc, target).since - skill.reactionTicks();
        float left = skill.settleTicks() <= 0 ? 0F : 1F - Mth.clamp((float) aimed / skill.settleTicks(), 0F, 1F);
        if (left <= 0F)
            return AimError.NONE;
        RandomSource random = npc.getRandom();
        // Uniform over the disc of the remaining error, so that most first rounds land near the target, not on its edge.
        double size = skill.startError() * left * Math.sqrt(random.nextDouble());
        double angle = random.nextDouble() * Math.PI * 2D;
        return new AimError((float) (size * Math.cos(angle)), (float) (size * Math.sin(angle)));
    }

    /** @return the share, 0 to 1, of a moving target's run the NPC allows for */
    public static float leadShare(Entity npc)
    {
        return of(npc).lead();
    }

    /**
     * @return the damage a player takes from a hit, after armour, as the limits of the NPC behind it allow
     */
    public static float damageTo(LivingEntity victim, DamageSource source, float amount)
    {
        if (!(victim instanceof Player) || !(source.getEntity() instanceof EntityNPCInterface npc))
            return amount;
        NpcSkill skill = of(npc);
        float dealt = amount * skill.playerDamage();
        return skill.nonLethal() ? Math.min(dealt, Math.max(0F, victim.getHealth() - SPARED_HEALTH)) : dealt;
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
