package com.flansmodultimate.common.distant;

import com.flansmodultimate.api.parts.PartBodyPose;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.EntityDistancePolicy;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.driveable.PacketDistantContacts;
import com.flansmodultimate.network.client.effects.PacketDistantExplosion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Server side of the far-terrain integration. Players whose client draws far terrain with Distant Horizons
 * ask for it with a {@link com.flansmodultimate.network.server.driveable.PacketDistantSubscription}; they are then sent
 * the driveables beyond their entity tracking range, the entities standing in for one, and the large explosions beyond the range of explosion
 * particles, so their client can show them on the far terrain. Display data only: nothing here changes
 * gameplay. Runs on the server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantSync
{
    /**
     * How far inside the vanilla tracking radius contacts still start, in blocks. Tracking is decided by
     * chunk, so a driveable can leave it slightly before its horizontal distance says; the client skips any
     * contact it is already tracking.
     */
    private static final double TRACKING_OVERLAP = 32D;
    /** Movement faster than this between two ticks is a teleport, not a velocity worth extrapolating. */
    private static final double MAX_EXTRAPOLATED_SPEED = 20D;

    private record Subscription(boolean contacts, boolean explosions)
    {}

    private static final Map<UUID, Subscription> subscriptions = new HashMap<>();
    /** Players whose last update listed contacts, who must be told once when there are none left. */
    private static final Set<UUID> playersWithContacts = new HashSet<>();

    public static void subscribe(ServerPlayer player, boolean contacts, boolean explosions)
    {
        UUID id = player.getUUID();
        if (!contacts)
            playersWithContacts.remove(id);
        if (contacts || explosions)
            subscriptions.put(id, new Subscription(contacts, explosions));
        else
            subscriptions.remove(id);
    }

    public static void unsubscribe(UUID playerId)
    {
        subscriptions.remove(playerId);
        playersWithContacts.remove(playerId);
    }

    public static void clear()
    {
        subscriptions.clear();
        playersWithContacts.clear();
    }

    /** Sends the contact updates that are due; called at the end of every server tick. */
    public static void tick(MinecraftServer server, long tick)
    {
        if (subscriptions.isEmpty())
            return;

        int interval = Math.max(1, ModCommonConfig.distantContactUpdateInterval());
        if (tick % interval != 0)
            return;

        double range = ModCommonConfig.distantContactRange();
        int maxCount = ModCommonConfig.distantContactMaxCount();
        boolean enabled = ModCommonConfig.distantContactsEnabled() && range > 0D && maxCount > 0;
        // Vanilla tracks a driveable while it is within the view distance, less one chunk, horizontally
        double viewRadius = (server.getPlayerList().getViewDistance() - 1) * 16D;

        for (ServerLevel level : server.getAllLevels())
        {
            List<ServerPlayer> receivers = new ArrayList<>();
            for (ServerPlayer player : level.players())
            {
                Subscription subscription = subscriptions.get(player.getUUID());
                if (subscription != null && subscription.contacts())
                    receivers.add(player);
            }
            if (receivers.isEmpty())
                continue;

            Map<Entity, Double> shown = enabled ? shownEntities(level, viewRadius) : Map.of();
            Map<Entity, Optional<PacketDistantContacts.Contact>> contacts = new HashMap<>();
            for (ServerPlayer player : receivers)
            {
                List<PacketDistantContacts.Contact> selected = new ArrayList<>();
                for (Entity entity : nearest(player, shown, range, maxCount))
                    contacts.computeIfAbsent(entity, key -> Optional.ofNullable(toContact(key))).ifPresent(selected::add);
                send(player, interval, selected);
            }
        }
    }

    /** The driveables of a level and the entities standing in for one, each with the horizontal distance its contacts start from. */
    private static Map<Entity, Double> shownEntities(ServerLevel level, double viewRadius)
    {
        Map<Entity, Double> shown = new LinkedHashMap<>();
        double driveableRadius = Math.max(0D, viewRadius - TRACKING_OVERLAP);
        for (Driveable driveable : level.getEntities(EntityTypeTest.forClass(Driveable.class), driveable -> driveable.isAlive() && !driveable.isWreck() && driveable.getConfigType() != null))
            shown.put(driveable, driveableRadius);
        for (Entity wearer : PartBodies.wearers(level))
        {
            PartBody body = PartBodies.of(wearer);
            if (wearer instanceof Driveable || body == null || !(body.getType() instanceof DriveableType))
                continue;
            shown.put(wearer, standInTrackedRadius(viewRadius, EntityDistancePolicy.trackingRange(wearer), wearer.getType().clientTrackingRange() * 16));
        }
        return shown;
    }

    /**
     * A stand-in is tracked like any entity of its kind: within its own range and never past the view distance.
     *
     * @param configuredRange
     *            the tracking range the mod gives the entity in blocks, or negative when vanilla decides
     * @param vanillaRange
     *            the tracking range of the entity's type in blocks
     */
    static double standInTrackedRadius(double viewRadius, int configuredRange, int vanillaRange)
    {
        return Math.max(0D, Math.min(viewRadius, configuredRange < 0 ? vanillaRange : configuredRange) - TRACKING_OVERLAP);
    }

    private static List<Entity> nearest(ServerPlayer player, Map<Entity, Double> shown, double range, int maxCount)
    {
        Entity ownVehicle = player.getRootVehicle();
        double rangeSquared = range * range;
        List<Entity> candidates = new ArrayList<>();
        for (Map.Entry<Entity, Double> entry : shown.entrySet())
        {
            Entity entity = entry.getKey();
            double trackedRadius = entry.getValue();
            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            if (entity == ownVehicle || dx * dx + dz * dz <= trackedRadius * trackedRadius || entity.distanceToSqr(player) > rangeSquared)
                continue;
            candidates.add(entity);
        }
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        return candidates.size() > maxCount ? candidates.subList(0, maxCount) : candidates;
    }

    private static void send(ServerPlayer player, int interval, List<PacketDistantContacts.Contact> contacts)
    {
        UUID id = player.getUUID();
        if (contacts.isEmpty())
        {
            // An empty update only matters to a client that is still showing contacts
            if (!playersWithContacts.remove(id))
                return;
        }
        else
            playersWithContacts.add(id);
        PacketHandler.sendTo(new PacketDistantContacts(interval, contacts), player);
    }

    /** @return what a client is shown of the entity, or null for a stand-in that took its body off after it was listed */
    @Nullable
    private static PacketDistantContacts.Contact toContact(Entity entity)
    {
        Vec3 velocity = entity.position().subtract(entity.xo, entity.yo, entity.zo);
        if (velocity.lengthSqr() > MAX_EXTRAPOLATED_SPEED * MAX_EXTRAPOLATED_SPEED)
            velocity = Vec3.ZERO;
        if (entity instanceof Driveable driveable)
            return toContact(driveable, velocity);

        PartBody body = PartBodies.of(entity);
        if (body == null || !(body.getType() instanceof DriveableType type))
            return null;
        // The body may have been posed some ticks ago, so its origin is taken from where the wearer stands now.
        Vec3 origin = entity.position().add(body.originOffset());
        PartBodyPose pose = body.getPose();
        // Paintjob 0: the server does not know the skin a stand-in is drawn with.
        return new PacketDistantContacts.Contact(entity.getId(), type.getShortName(), 0, origin.x, origin.y, origin.z, (float) velocity.x, (float) velocity.y, (float) velocity.z, pose.yaw(),
            pose.pitch(), pose.roll(), body.missingParts().stream().mapToInt(Enum::ordinal).toArray(), true, pose.scale());
    }

    private static PacketDistantContacts.Contact toContact(Driveable driveable, Vec3 velocity)
    {
        Vec3 position = driveable.position();

        List<Integer> destroyed = new ArrayList<>();
        if (driveable.getDriveableData() != null)
        {
            for (Map.Entry<EnumDriveablePart, DriveablePart> part : driveable.getDriveableData().getParts().entrySet())
            {
                if (part.getValue().isDestroyed())
                    destroyed.add(part.getKey().ordinal());
            }
        }

        return new PacketDistantContacts.Contact(driveable.getId(), driveable.getShortName(), driveable.getPaintjobId(), position.x, position.y, position.z, (float) velocity.x, (float) velocity.y,
            (float) velocity.z, driveable.getYaw(), driveable.getPitch(), driveable.getRoll(), destroyed.stream().mapToInt(Integer::intValue).toArray(), false, 1F);
    }

    /**
     * Shows a large explosion to subscribed players who were too far away to be sent its particles.
     *
     * @param particleRange
     *            how far the explosion's particles were sent, in blocks
     */
    public static void onExplosion(ServerLevel level, Vec3 center, float explosionRadius, float blastRadius, boolean fiery, double particleRange)
    {
        onExplosion(level, center, explosionRadius, explosionRadius, blastRadius, fiery, particleRange);
    }

    // The negated comparison also rejects NaN.
    @SuppressWarnings("java:S1940")
    public static void onExplosion(ServerLevel level, Vec3 center, float explosionRadius, float visualRadius, float blastRadius, boolean fiery, double particleRange)
    {
        if (subscriptions.isEmpty())
            return;

        double range = ModCommonConfig.distantExplosionRange();
        if (range <= particleRange || !(explosionRadius >= ModCommonConfig.distantExplosionMinRadius()))
            return;

        double nearSquared = particleRange * particleRange;
        double rangeSquared = range * range;
        PacketDistantExplosion packet = null;
        for (ServerPlayer player : level.players())
        {
            Subscription subscription = subscriptions.get(player.getUUID());
            if (subscription == null || !subscription.explosions())
                continue;

            double distanceSquared = player.distanceToSqr(center);
            if (distanceSquared <= nearSquared || distanceSquared > rangeSquared)
                continue;

            if (packet == null)
                packet = new PacketDistantExplosion(center, visualRadius, blastRadius, fiery);
            PacketHandler.sendTo(packet, player);
        }
    }
}
