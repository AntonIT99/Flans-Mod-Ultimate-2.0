package com.flansmodultimate.common.eras;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.IFlanEntity;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.InfoType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Era restrictions on Flan's entities: driveables, AA guns and deployed guns. A blocked one is disabled in place, since
 * nobody it blocks may board it; it is despawned only when a round or battle spawned it, or a server or dimension
 * restriction asks for it with {@code despawn=on}. Despawning runs once when restrictions or dates change and when an
 * entity joins a level, never every tick. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EraEntities
{
    /** Persistent marker of an entity an operator placed while bypassing; strict despawning leaves it alone. */
    public static final String BYPASS_TAG = "flansmodultimate_era_bypass";
    private static final int RIDER_CHECK_PERIOD = 20;

    /** The Flan's type an entity stands for: a seat stands for its driveable. */
    @Nullable
    public static InfoType typeOf(Entity entity)
    {
        if (entity instanceof Seat seat)
            return seat.getDriveable() == null ? null : seat.getDriveable().getConfigType();
        return entity instanceof IFlanEntity<?> flanEntity ? flanEntity.getConfigType() : null;
    }

    /** Whether the passenger may board; a refused player is told why. */
    public static boolean mayBoard(Entity passenger, Entity vehicle)
    {
        InfoType type = typeOf(vehicle);
        return type == null || !EraEnforcement.refuses(passenger, type);
    }

    /** About once a second, puts down a rider whose vehicle an era restriction now blocks for them. */
    public static void dismountBlocked(LivingEntity rider)
    {
        Entity vehicle = rider.getVehicle();
        if (vehicle == null || (rider.tickCount + (rider.getUUID().hashCode() & 0x7fffffff)) % RIDER_CHECK_PERIOD != 0)
            return;
        InfoType type = typeOf(vehicle);
        if (type != null && EraEnforcement.refuses(rider, type))
            rider.stopRiding();
    }

    /** Marks what an operator placed while bypassing, so a strict server keeps it after they leave. */
    public static void tagIfBypassing(@Nullable Entity placed, Player placer)
    {
        if (placed != null && EraRestrictions.isBypassing(placer))
            placed.getPersistentData().putBoolean(BYPASS_TAG, true);
    }

    public static boolean isBypassTagged(Entity entity)
    {
        return entity.getPersistentData().getBoolean(BYPASS_TAG);
    }

    /** Despawns every loaded entity that restrictions now despawn; returns how many. */
    public static int sweep(MinecraftServer server)
    {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels())
        {
            List<Entity> doomed = new ArrayList<>();
            for (Entity entity : level.getAllEntities())
                if (shouldDespawn(entity))
                    doomed.add(entity);
            doomed.forEach(EraEntities::despawn);
            removed += doomed.size();
        }
        return removed;
    }

    /** Checks an entity that just joined a level, on the next tick so it is never removed while its chunk loads. */
    public static void checkJoined(Entity entity)
    {
        MinecraftServer server = entity.getServer();
        if (server == null || typeOf(entity) == null || entity instanceof Seat)
            return;
        server.execute(() ->
        {
            if (entity.isAlive() && shouldDespawn(entity))
                despawn(entity);
        });
    }

    /**
     * Whether a restriction blocking the entity despawns it: a round or battle restriction always does, since it covers
     * only what the match spawned; a server or dimension one only with {@code despawn=on} and not for an operator's
     * bypass placement. Seats go with their driveable.
     */
    public static boolean shouldDespawn(Entity entity)
    {
        InfoType type = typeOf(entity);
        if (type == null || entity instanceof Seat || !entity.isAlive())
            return false;
        EraVerdicts verdicts = EraRestrictions.verdicts();
        for (ScopedRestriction scoped : EraRestrictions.restrictionsFor(entity))
        {
            if (verdicts.allows(scoped.restriction(), type))
                continue;
            if (!scoped.scope().kind().isDespawnAllowed())
                return true;
            if (scoped.restriction().policies().despawn() && !isBypassTagged(entity))
                return true;
        }
        return false;
    }

    /** Puts riders down, drops a driveable's cargo where it stands and removes the entity, which drops a gun's own items. */
    private static void despawn(Entity entity)
    {
        entity.ejectPassengers();
        if (entity instanceof Driveable driveable)
            driveable.dropContents();
        entity.discard();
    }
}
