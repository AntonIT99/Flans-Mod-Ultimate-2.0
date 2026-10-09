package com.flansmodultimate.common.distant;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.driveable.PacketDistantContacts;
import com.flansmodultimate.network.client.effects.PacketDistantExplosion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
 * the driveables beyond their entity tracking range and the large explosions beyond the range of explosion
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
        double trackedRadius = Math.max(0D, (server.getPlayerList().getViewDistance() - 1) * 16D - TRACKING_OVERLAP);

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

            List<Driveable> driveables = enabled
                ? new ArrayList<>(level.getEntities(EntityTypeTest.forClass(Driveable.class), driveable -> driveable.isAlive() && driveable.getConfigType() != null))
                : List.of();
            Map<Driveable, PacketDistantContacts.Contact> contacts = new HashMap<>();
            for (ServerPlayer player : receivers)
            {
                List<PacketDistantContacts.Contact> selected = new ArrayList<>();
                for (Driveable driveable : nearest(player, driveables, trackedRadius, range, maxCount))
                    selected.add(contacts.computeIfAbsent(driveable, DistantSync::toContact));
                send(player, interval, selected);
            }
        }
    }

    private static List<Driveable> nearest(ServerPlayer player, List<Driveable> driveables, double trackedRadius, double range, int maxCount)
    {
        Entity ownVehicle = player.getRootVehicle();
        double trackedSquared = trackedRadius * trackedRadius;
        double rangeSquared = range * range;
        List<Driveable> candidates = new ArrayList<>();
        for (Driveable driveable : driveables)
        {
            double dx = driveable.getX() - player.getX();
            double dz = driveable.getZ() - player.getZ();
            if (driveable == ownVehicle || dx * dx + dz * dz <= trackedSquared || driveable.distanceToSqr(player) > rangeSquared)
                continue;
            candidates.add(driveable);
        }
        candidates.sort(Comparator.comparingDouble(driveable -> driveable.distanceToSqr(player)));
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

    private static PacketDistantContacts.Contact toContact(Driveable driveable)
    {
        Vec3 position = driveable.position();
        Vec3 velocity = position.subtract(driveable.xo, driveable.yo, driveable.zo);
        if (velocity.lengthSqr() > MAX_EXTRAPOLATED_SPEED * MAX_EXTRAPOLATED_SPEED)
            velocity = Vec3.ZERO;

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
            (float) velocity.z, driveable.getYaw(), driveable.getPitch(), driveable.getRoll(), destroyed.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Shows a large explosion to subscribed players who were too far away to be sent its particles.
     *
     * @param particleRange
     *            how far the explosion's particles were sent, in blocks
     */
    public static void onExplosion(ServerLevel level, Vec3 center, float explosionRadius, float blastRadius, boolean fiery, double particleRange)
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
                packet = new PacketDistantExplosion(center, explosionRadius, blastRadius, fiery);
            PacketHandler.sendTo(packet, player);
        }
    }
}
