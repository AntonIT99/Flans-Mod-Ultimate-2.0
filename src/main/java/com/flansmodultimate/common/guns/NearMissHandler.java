package com.flansmodultimate.common.guns;

import com.flansmodultimate.FlansModSounds;
import com.flansmodultimate.api.entity.ISuppressionListener;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketBulletFlyby;
import com.flansmodultimate.network.client.effects.PacketCombatFeedback;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatches at most one sound and one suppression impulse per shot and player along the actual travelled path, and tells
 * the addons' suppression listeners about mobs the shot passed.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NearMissHandler
{
    private static final List<ISuppressionListener> LISTENERS = new CopyOnWriteArrayList<>();

    /** Allocated only when the first nearby player is found, and owned by one projectile or hitscan shot. */
    public static final class Recipients
    {
        private final Set<UUID> sounded = new HashSet<>();
        private final Set<UUID> suppressed = new HashSet<>();
    }

    private record Policy(String sound, double soundRange, double suppressionRange, double suppressionMultiplier, boolean affectsRiders)
    {
        private static Policy from(CommonConfigSnapshot config, BulletType type)
        {
            String sound = type.getFlyBySound() == null ? FlansModSounds.SOUND_BULLETFLYBY : type.getFlyBySound();
            double soundRange = config.bulletFlybySounds() && !sound.isEmpty() ? Math.min(32D, type.getFlyBySoundRange() * config.bulletFlybyRangeMultiplier()) : 0D;
            double suppressionRange = config.nearMissSuppression() && type.getSuppression() > 0F && config.suppressionIntensity() > 0D
                ? Math.min(32D, type.getSuppressionRange() * config.suppressionRangeMultiplier())
                : 0D;
            return new Policy(sound, soundRange, suppressionRange, config.suppressionIntensity() * type.getSuppression() / 16D, config.suppressionAffectsRiders());
        }
    }

    public static void addListener(ISuppressionListener listener)
    {
        if (!LISTENERS.contains(listener))
            LISTENERS.add(listener);
    }

    public static void removeListener(ISuppressionListener listener)
    {
        LISTENERS.remove(listener);
    }

    @Nullable
    public static Recipients dispatch(Level level, FiredShot shot, Vec3 start, Vec3 end, Set<UUID> directHits, @Nullable Recipients recipients)
    {
        recipients = dispatchToMobs(level, shot, start, end, recipients);
        if (!(level instanceof ServerLevel serverLevel) || start.distanceToSqr(end) < 1E-12D)
            return recipients;
        var config = ModCommonConfig.get();
        if (config == null)
            return recipients;
        Policy policy = Policy.from(config, shot.getBulletType());
        double range = Math.max(policy.soundRange(), policy.suppressionRange());
        if (range <= 0D)
            return recipients;
        var owners = shot.getOwnerEntities();
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(start, end).inflate(range)))
        {
            if (eligible(player, owners, directHits))
            {
                Vec3 closest = visiblePoint(level, player, start, end, range);
                if (closest != null)
                {
                    if (recipients == null)
                        recipients = new Recipients();
                    dispatchToPlayer(player, shot, closest, policy, recipients);
                }
            }
        }
        return recipients;
    }

    /**
     * Tells the suppression listeners about the mobs a shot passed, within the suppression range of its round, whatever
     * players have chosen for their own suppression feedback.
     */
    @Nullable
    private static Recipients dispatchToMobs(Level level, FiredShot shot, Vec3 start, Vec3 end, @Nullable Recipients recipients)
    {
        if (LISTENERS.isEmpty() || !(level instanceof ServerLevel serverLevel) || start.distanceToSqr(end) < 1E-12D)
            return recipients;
        BulletType type = shot.getBulletType();
        var config = ModCommonConfig.get();
        double range = config == null || type.getSuppression() <= 0F ? 0D : Math.min(32D, type.getSuppressionRange() * config.suppressionRangeMultiplier());
        if (range <= 0D)
            return recipients;
        var owners = shot.getOwnerEntities();
        LivingEntity shooter = shot.getAttacker().orElse(null);
        for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, new AABB(start, end).inflate(range), candidate -> candidate.isAlive() && !owners.contains(candidate)))
        {
            Vec3 closest = visiblePoint(level, mob, start, end, range);
            if (closest == null)
                continue;
            if (recipients == null)
                recipients = new Recipients();
            float intensity = NearMissFeedback.intensity(ShootingHelper.getDamage(mob, null, shot), closest.distanceTo(mob.getEyePosition()), range, type.getSuppression() / 16D);
            if (intensity > 0F && recipients.suppressed.add(mob.getUUID()))
                notifyListeners(mob, shooter, Math.min(1F, intensity));
        }
        return recipients;
    }

    private static void notifyListeners(Mob mob, @Nullable LivingEntity shooter, float intensity)
    {
        for (ISuppressionListener listener : LISTENERS)
        {
            try
            {
                listener.suppressed(mob, shooter, intensity);
            }
            // A listener belongs to another mod: whatever it throws is reported and must not break the shot.
            catch (RuntimeException e)
            {
                FlansLog.log.error("Suppression listener {} failed", listener.getClass().getName(), e);
            }
        }
    }

    private static boolean eligible(ServerPlayer player, List<Entity> owners, Set<UUID> directHits)
    {
        return player.isAlive() && !player.isSpectator() && !owners.contains(player) && !directHits.contains(player.getUUID());
    }

    @Nullable
    private static Vec3 visiblePoint(Level level, LivingEntity entity, Vec3 start, Vec3 end, double range)
    {
        Vec3 eye = entity.getEyePosition();
        Vec3 closest = NearMissFeedback.closestPoint(start, end, eye);
        if (closest.distanceToSqr(eye) > range * range)
            return null;
        return level.clip(new ClipContext(eye, closest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS ? closest : null;
    }

    private static void dispatchToPlayer(ServerPlayer player, FiredShot shot, Vec3 closest, Policy policy, Recipients recipients)
    {
        Vec3 eye = player.getEyePosition();
        double distance = closest.distanceTo(eye);
        if (policy.soundRange() > 0D && distance <= policy.soundRange() && recipients.sounded.add(player.getUUID()))
            PacketHandler.sendTo(new PacketBulletFlyby(policy.sound(), closest), player);
        if (policy.suppressionRange() <= 0D || distance > policy.suppressionRange() || (player.isPassenger() && !policy.affectsRiders()))
            return;
        float intensity = NearMissFeedback.intensity(ShootingHelper.getDamage(player, null, shot), distance, policy.suppressionRange(), policy.suppressionMultiplier());
        if (intensity > 0F && recipients.suppressed.add(player.getUUID()))
        {
            Vec3 offset = closest.subtract(eye);
            float direction = (float) Math.atan2(-offset.x, offset.z);
            PacketHandler.sendTo(new PacketCombatFeedback(false, intensity, direction), player);
        }
    }
}
