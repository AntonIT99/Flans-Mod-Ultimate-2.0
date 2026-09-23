package com.flansmodultimate.common.driveables;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Per-entity runtime state for a driveable's shaped collision hull.
 *
 * <p>Entities treat the hull as solid during their own movement (see
 * {@link DriveableCollisionWorld}), so standing on and walking into a hull
 * behave like terrain. What movement cannot know is that the hull itself moves:
 * each tick this helper poses the hull, carries entities that were standing on
 * it along with it, and pushes out entities it moved into. Candidate discovery
 * is a bounded spatial query and never scans the world's loaded-entity list.</p>
 *
 * <p>An entity the push cannot free, because terrain blocks every way out, is
 * let out instead: see {@link DriveableHullEscape}.</p>
 */
public final class DriveableCollisionHelper
{
    private static final int MAX_CANDIDATES = 128;
    private static final double MAX_QUERY_RADIUS = 96D;
    private static final double QUERY_MARGIN = 1D;
    private static final double MAX_PLATFORM_DELTA = 3D;
    private static final double MAX_SEPARATION_PER_TICK = 0.75D;
    private static final double SEPARATION_THRESHOLD = 1.0E-3D;
    private static final double SEPARATION_SKIN = 1.0E-4D;
    /** How fast an entity trapped inside a hull is eased out of it, in blocks per tick. */
    private static final double ESCAPE_SPEED = 0.25D;
    /** Directions tried, in order, when the shortest way out of a hull leads into terrain. */
    private static final double[] ESCAPE_FALLBACKS = {
        0D, 1D, 0D,
        1D, 0D, 0D,
        -1D, 0D, 0D,
        0D, 0D, 1D,
        0D, 0D, -1D,
        0D, -1D, 0D
    };

    private final DriveableCollisionProfile profile;
    private final DriveableHullGeometry geometry;
    private final DriveableHullEscape escape = new DriveableHullEscape();
    private final double[] boundsScratch = new double[6];
    private final double[] vectorScratch = new double[4];
    private final double[] escapeScratch = new double[4];
    @Nullable
    private Driveable owner;
    @Nullable
    private DriveableCollisionWorld.LevelHulls registry;
    private int lastTick = Integer.MIN_VALUE;
    private double lastX;
    private double lastY;
    private double lastZ;

    public DriveableCollisionHelper(DriveableCollisionProfile profile)
    {
        this.profile = profile == null ? DriveableCollisionProfile.compile(null) : profile;
        geometry = new DriveableHullGeometry(this.profile);
    }

    public boolean matches(DriveableCollisionProfile candidate)
    {
        return profile == candidate;
    }

    public boolean hasGeometry()
    {
        return !profile.isEmpty();
    }

    DriveableHullGeometry geometry()
    {
        return geometry;
    }

    DriveableHullEscape escape()
    {
        return escape;
    }

    @Nullable
    Driveable owner()
    {
        return owner;
    }

    public void tick(Driveable driveable)
    {
        if (driveable == null || driveable.isRemoved() || profile.isEmpty())
            return;
        DriveableType type = driveable.getConfigType();
        if (type == null)
            return;

        owner = driveable;
        boolean continuous = lastTick == driveable.tickCount - 1
            && squaredDistance(lastX, lastY, lastZ, driveable.getX(), driveable.getY(), driveable.getZ())
                <= MAX_PLATFORM_DELTA * MAX_PLATFORM_DELTA;
        lastTick = driveable.tickCount;
        lastX = driveable.getX();
        lastY = driveable.getY();
        lastZ = driveable.getZ();
        geometry.update(driveable.getX(), driveable.getY(), driveable.getZ(), driveable.getYaw(),
            driveable.getPitch(), driveable.getRoll(), driveable.getTurretYaw(), driveable.getTurretPitch(),
            driveable.getCollisionTurretPivot(), driveable.getCollisionTurretOffset(), driveable::isPartIntact,
            continuous);
        register(driveable.level());
        if (!geometry.queryBounds(boundsScratch))
            return;

        double minX = Math.max(boundsScratch[0], driveable.getX() - MAX_QUERY_RADIUS);
        double minY = Math.max(boundsScratch[1], driveable.getY() - MAX_QUERY_RADIUS);
        double minZ = Math.max(boundsScratch[2], driveable.getZ() - MAX_QUERY_RADIUS);
        double maxX = Math.min(boundsScratch[3], driveable.getX() + MAX_QUERY_RADIUS);
        double maxY = Math.min(boundsScratch[4], driveable.getY() + MAX_QUERY_RADIUS);
        double maxZ = Math.min(boundsScratch[5], driveable.getZ() + MAX_QUERY_RADIUS);
        if (minX > maxX || minY > maxY || minZ > maxZ)
            return;

        Level level = driveable.level();
        boolean clientSide = level.isClientSide;
        AABB query = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(QUERY_MARGIN);
        List<Entity> candidates = level.getEntities(driveable, query,
            candidate -> DriveableCollisionWorld.collidesWithHulls(candidate) && !driveable.isPartOfThis(candidate)
                && (!clientSide || DriveableCollisionWorld.isSimulatedHere(candidate)));
        int count = Math.min(MAX_CANDIDATES, candidates.size());
        for (int index = 0; index < count; index++)
            handleCandidate(driveable, type, candidates.get(index));
    }

    /** Stops entities colliding with this hull, for a driveable leaving its level. */
    public void unregister()
    {
        if (registry != null)
            registry.remove(this);
        registry = null;
    }

    void forgetRegistry()
    {
        registry = null;
    }

    private void register(Level level)
    {
        DriveableCollisionWorld.LevelHulls hulls = DriveableCollisionWorld.hulls(level);
        if (hulls == registry)
            return;
        unregister();
        if (hulls != null)
        {
            hulls.add(this);
            registry = hulls;
        }
    }

    private void handleCandidate(Driveable driveable, DriveableType type, Entity entity)
    {
        // Players are simulated by their own client, against the hull pose it sees.
        boolean simulated = DriveableCollisionWorld.isSimulatedHere(entity);
        AABB box = entity.getBoundingBox();
        if (simulated)
        {
            int support = geometry.findSupport(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            if (support >= 0)
            {
                // Standing on the hull is never being trapped, so an escape ends here and carrying resumes with it.
                if (!escape.isSuspended(entity))
                    carry(entity, support);
                box = entity.getBoundingBox();
                boolean overlaps = geometry.findPenetration(box.minX, box.minY, box.minZ, box.maxX, box.maxY,
                    box.maxZ, true, vectorScratch) && vectorScratch[3] > SEPARATION_THRESHOLD;
                escape.report(entity, overlaps ? vectorScratch[3] : 0D, true);
                if (overlaps)
                    separate(entity, vectorScratch);
                return;
            }
        }

        if (!geometry.findPenetration(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, false,
            vectorScratch) || vectorScratch[3] <= SEPARATION_THRESHOLD)
            return;
        boolean trapped = escape.report(entity, vectorScratch[3], false);
        if (simulated)
        {
            if (trapped)
                easeOut(entity, vectorScratch);
            else
                separate(entity, vectorScratch);
        }
        // An entity sealed inside the hull would otherwise be run over once every tick until it dies.
        if (!trapped && !driveable.level().isClientSide && entity instanceof LivingEntity living
            && Math.abs(vectorScratch[1]) < 0.6D)
            applyConfiguredImpactDamage(driveable, type, living);
    }

    /**
     * Eases an entity trapped inside the hull towards the nearest way out,
     * without the shove {@link #separate} gives. While an entity is trapped the
     * hull is suspended for it, so this only has to keep it drifting free while
     * it walks out itself rather than clear the whole overlap at once.
     *
     * <p>The shortest way out often leads into terrain, which is what trapped
     * the entity in the first place. Every direction is therefore tried against
     * the blocks around the entity, and the one actually leaving it least deep
     * in the hull wins; if none does, it stays put and the suspended hull is
     * what lets it walk out.</p>
     */
    private void easeOut(Entity entity, double[] push)
    {
        AABB box = entity.getBoundingBox();
        double bestDepth = push[3];
        Vec3 best = Vec3.ZERO;
        Vec3 step = escapeStep(entity, box, push[0], push[1], push[2],
            Math.min(ESCAPE_SPEED, push[3] + SEPARATION_SKIN));
        double depth = depthAfter(box, step);
        if (depth < bestDepth)
        {
            bestDepth = depth;
            best = step;
        }
        for (int index = 0; index < ESCAPE_FALLBACKS.length; index += 3)
        {
            step = escapeStep(entity, box, ESCAPE_FALLBACKS[index], ESCAPE_FALLBACKS[index + 1],
                ESCAPE_FALLBACKS[index + 2], ESCAPE_SPEED);
            depth = depthAfter(box, step);
            if (depth < bestDepth)
            {
                bestDepth = depth;
                best = step;
            }
        }
        if (best.lengthSqr() > 0D)
            entity.setPos(entity.getX() + best.x, entity.getY() + best.y, entity.getZ() + best.z);
        entity.resetFallDistance();
    }

    /** The part of one escape step that the blocks around the entity leave free. */
    private static Vec3 escapeStep(Entity entity, AABB box, double x, double y, double z, double distance)
    {
        return Entity.collideBoundingBox(entity, new Vec3(x * distance, y * distance, z * distance), box,
            entity.level(), List.of());
    }

    /** How deep in the hull the entity would still be after moving its box by {@code step}. */
    private double depthAfter(AABB box, Vec3 step)
    {
        if (step.lengthSqr() < SEPARATION_SKIN * SEPARATION_SKIN)
            return Double.POSITIVE_INFINITY;
        AABB moved = box.move(step);
        return geometry.findPenetration(moved.minX, moved.minY, moved.minZ, moved.maxX, moved.maxY, moved.maxZ,
            false, escapeScratch) ? escapeScratch[3] : 0D;
    }

    /** Moves an entity with the surface it stood on, turning it with the hull as well. */
    private void carry(Entity entity, int shape)
    {
        AABB box = entity.getBoundingBox();
        double footX = (box.minX + box.maxX) * 0.5D;
        double footY = box.minY;
        double footZ = (box.minZ + box.maxZ) * 0.5D;
        if (!geometry.carryPoint(shape, footX, footY, footZ, vectorScratch))
            return;
        double deltaX = vectorScratch[0] - footX;
        double deltaY = vectorScratch[1] - footY;
        double deltaZ = vectorScratch[2] - footZ;
        double lengthSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        if (!(lengthSquared <= MAX_PLATFORM_DELTA * MAX_PLATFORM_DELTA))
            return;
        if (lengthSquared > 1.0E-12D)
            DriveableCollisionWorld.moveIgnoringHulls(entity, deltaX, deltaY, deltaZ);
        float yaw = geometry.carryYaw(shape);
        if (Math.abs(yaw) > 1.0E-3F)
        {
            entity.setYRot(entity.getYRot() + yaw);
            entity.setYHeadRot(entity.getYHeadRot() + yaw);
            if (entity instanceof LivingEntity living)
                living.yBodyRot += yaw;
        }
        entity.resetFallDistance();
    }

    private static void separate(Entity entity, double[] push)
    {
        double distance = Math.min(MAX_SEPARATION_PER_TICK, push[3] + SEPARATION_SKIN);
        DriveableCollisionWorld.moveIgnoringHulls(entity, push[0] * distance, push[1] * distance, push[2] * distance);
        Vec3 motion = entity.getDeltaMovement();
        double into = motion.x * push[0] + motion.y * push[1] + motion.z * push[2];
        if (into < 0D)
            entity.setDeltaMovement(motion.x - push[0] * into, motion.y - push[1] * into, motion.z - push[2] * into);
        if (push[1] >= DriveableHullGeometry.MIN_SUPPORT_NORMAL_Y)
        {
            entity.setOnGround(true);
            entity.resetFallDistance();
        }
    }

    private static void applyConfiguredImpactDamage(Driveable driveable, DriveableType type, LivingEntity candidate)
    {
        float throttle = Math.abs(driveable.getThrottle());
        if (!type.isCollisionDamageEnable() || throttle <= Math.max(0F, type.getCollisionDamageThrottle())
            || !canDamageCandidate(driveable, candidate))
            return;
        float amount = throttle * Math.max(0F, type.getCollisionDamageTimes());
        if (amount <= 0F)
            return;
        Entity controller = driveable.getControllingEntity();
        DamageSource source = controller instanceof Player player
            ? driveable.level().damageSources().playerAttack(player)
            : controller instanceof LivingEntity living
                ? driveable.level().damageSources().mobAttack(living)
                : driveable.level().damageSources().flyIntoWall();
        candidate.hurt(source, amount);
    }

    private static boolean canDamageCandidate(Driveable driveable, LivingEntity candidate)
    {
        Entity controller = driveable.getControllingEntity();
        if (controller == null)
            return true;
        if (candidate instanceof ServerPlayer victim && controller instanceof ServerPlayer attacker)
        {
            try
            {
                return FlansMod.teamsManager.getCurrentGameType()
                    .map(gameType -> gameType.canPlayerBeAttacked(victim, attacker))
                    .orElseGet(() -> !victim.isAlliedTo(attacker));
            }
            catch (RuntimeException ignored)
            {
                // Teams state is optional outside an active server round.
            }
        }
        return !candidate.isAlliedTo(controller) && !controller.isAlliedTo(candidate);
    }

    private static double squaredDistance(double ax, double ay, double az, double bx, double by, double bz)
    {
        double x = ax - bx;
        double y = ay - by;
        double z = az - bz;
        return x * x + y * y + z * z;
    }
}
