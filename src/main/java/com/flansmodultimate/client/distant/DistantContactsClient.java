package com.flansmodultimate.client.distant;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.parts.PartBodyPose;
import com.flansmodultimate.client.render.thermal.ThermalVision;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.network.client.driveable.PacketDistantContacts;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Driveables shown as simplified shapes because they are too far away to be drawn as entities: those the
 * client tracks but no longer draws, and those only the server reports, beyond the tracking range. An entity
 * that wears the part boxes of a driveable type, as an NPC drawn as one does, is shown the same way. Each is
 * drawn by the far-terrain renderer beyond the vanilla chunks and by {@link DistantBoxRenderer} within them.
 * Render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantContactsClient
{
    /** Ticks a reported contact outlives the update that should have replaced it. */
    private static final int STALE_MARGIN_TICKS = 20;
    /** How driveables look in a thermal sight: their engines and hulls are the hottest things around. */
    private static final int THERMAL_COLOR = 0xFFF2F2F2;

    private record Reported(PacketDistantContacts.Contact contact, long receivedTick)
    {}

    private static final Map<Integer, Reported> reported = new HashMap<>();
    private static final Map<Integer, Proxy> proxies = new HashMap<>();
    private static int updateInterval = 5;
    private static long ticks;

    /** Replaces the reported contacts with a new update from the server. */
    public static void accept(PacketDistantContacts packet)
    {
        updateInterval = Math.max(1, packet.getUpdateInterval());
        reported.clear();
        if (!DistantHorizonsClient.contactsEnabled())
            return;
        for (PacketDistantContacts.Contact contact : packet.getContacts())
            reported.put(contact.entityId(), new Reported(contact, ticks));
    }

    static void tick(ClientLevel level, Vec3 camera, IDistantTerrain terrain, boolean enabled, double handoff)
    {
        ticks++;
        if (!enabled)
        {
            reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Entity ownVehicle = minecraft.player == null ? null : minecraft.player.getRootVehicle();
        Set<Integer> live = new HashSet<>();

        // Tracked, but too far away to be drawn as entities
        for (Entity entity : level.entitiesForRendering())
        {
            if (entity == ownVehicle || !entity.isAlive() || entity.shouldRender(camera.x, camera.y, camera.z))
                continue;
            if (entity instanceof Driveable driveable)
            {
                if (driveable.isWreck() || driveable.getConfigType() == null)
                    continue;
                proxies.computeIfAbsent(driveable.getId(), Proxy::new).follow(driveable);
                live.add(driveable.getId());
                continue;
            }
            // An entity standing in for a driveable, as an NPC drawn as one, is shown by the part boxes it wears
            PartBody body = PartBodies.of(entity);
            if (body != null && body.getType() instanceof DriveableType wornType)
            {
                proxies.computeIfAbsent(entity.getId(), Proxy::new).follow(entity, body, wornType);
                live.add(entity.getId());
            }
        }

        // Only reported by the server; one the client tracks is drawn by the loop above or as itself
        long staleAfter = updateInterval * 2L + STALE_MARGIN_TICKS;
        for (Reported entry : reported.values())
        {
            int id = entry.contact().entityId();
            if (ticks - entry.receivedTick() > staleAfter || live.contains(id) || level.getEntity(id) != null)
                continue;
            if (!(InfoType.getInfoType(entry.contact().shortName()) instanceof DriveableType type))
                continue;
            proxies.computeIfAbsent(id, Proxy::new).follow(entry, type);
            live.add(id);
        }

        proxies.values().removeIf(proxy ->
        {
            if (live.contains(proxy.id))
                return false;
            proxy.close();
            return true;
        });

        boolean thermal = ThermalVision.active();
        for (Proxy proxy : proxies.values())
            proxy.update(terrain, camera, handoff, thermal);
    }

    /** Calls {@code action} for every shape that {@link DistantBoxRenderer} draws this tick. */
    static void forEachNear(DistantBoxRenderer.ShapeConsumer action)
    {
        for (Proxy proxy : proxies.values())
        {
            if (proxy.near)
                action.accept(proxy, proxy.boxes, DistantBoxStyle.SOLID);
        }
    }

    public static void reset()
    {
        for (Proxy proxy : proxies.values())
            proxy.close();
        proxies.clear();
        reported.clear();
    }

    private static final class Proxy implements IDistantBoxGroup.Origin
    {
        private final int id;
        /** The tracked entity: a driveable, or the wearer of {@link #worn}. */
        @Nullable
        private Entity entity;
        /** The part body of a tracked entity that stands in for a driveable. */
        @Nullable
        private PartBody worn;
        /** The skin a stand-in was last seen with, kept for when only the server reports it. */
        @Nullable
        private ResourceLocation skin;
        /** The proxy's own body, posed from the reports of a stand-in the client does not track. */
        @Nullable
        private PartBody reportedBody;
        @Nullable
        private Reported report;
        @Nullable
        private DriveableType type;
        private List<DistantBox> boxes = List.of();
        @Nullable
        private IDistantBoxGroup group;
        /** Whether the mod draws this shape itself this tick, rather than the far-terrain renderer. */
        private boolean near;

        private Proxy(int id)
        {
            this.id = id;
        }

        private void follow(Driveable driveable)
        {
            entity = driveable;
            worn = null;
            skin = null;
            report = null;
            type = driveable.getConfigType();
        }

        private void follow(Entity wearer, PartBody body, DriveableType wornType)
        {
            entity = wearer;
            worn = body;
            skin = skinOf(wearer);
            report = null;
            type = wornType;
        }

        private void follow(Reported reported, DriveableType reportedType)
        {
            entity = null;
            worn = null;
            report = reported;
            type = reportedType;
        }

        /** The texture the entity's renderer draws it with, which for a stand-in is the skin of its driveable model. */
        @Nullable
        @SuppressWarnings({"unchecked", "rawtypes"}) // The dispatcher hands out the renderer registered for the entity's own type.
        private static ResourceLocation skinOf(Entity wearer)
        {
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(wearer);
            return renderer.getTextureLocation(wearer);
        }

        @Override
        public Vec3 at(float partialTick)
        {
            if (entity != null)
                return entity.getPosition(partialTick);
            if (report == null)
                return Vec3.ZERO;

            // Carry on along the last reported velocity until the next update, but not indefinitely
            PacketDistantContacts.Contact contact = report.contact();
            double age = Math.min(ticks - report.receivedTick() + partialTick, updateInterval * 2D);
            return new Vec3(contact.x() + contact.velocityX() * age, contact.y() + contact.velocityY() * age, contact.z() + contact.velocityZ() * age);
        }

        private void update(IDistantTerrain terrain, Vec3 camera, double handoff, boolean thermal)
        {
            if (type == null)
                return;

            if (entity instanceof Driveable driveable)
            {
                int color = thermal ? THERMAL_COLOR : DistantTextureColors.of(type, driveable.getPaintjobId());
                boxes = DistantProxyShapes.forEntity(driveable, color);
            }
            else if (entity != null && worn != null)
            {
                int color = thermal ? THERMAL_COLOR : DistantTextureColors.of(type, skin);
                boxes = DistantProxyShapes.forBody(worn, worn.originOffset(), Set.of(), color);
            }
            else if (report != null && report.contact().standIn())
            {
                PacketDistantContacts.Contact contact = report.contact();
                int color = thermal ? THERMAL_COLOR : DistantTextureColors.of(type, skin);
                if (reportedBody == null || reportedBody.getType() != type)
                    reportedBody = PartBody.of(type);
                reportedBody.setPose(new PartBodyPose(Vec3.ZERO, contact.yaw(), contact.pitch(), contact.roll(), contact.scale(), MountAim.REST));
                boxes = DistantProxyShapes.forBody(reportedBody, Vec3.ZERO, destroyedParts(contact), color);
            }
            else if (report != null)
            {
                PacketDistantContacts.Contact contact = report.contact();
                int color = thermal ? THERMAL_COLOR : DistantTextureColors.of(type, contact.paintjobId());
                boxes = DistantProxyShapes.forType(type, contact.yaw(), contact.pitch(), contact.roll(), destroyedParts(contact), color);
            }

            near = at(1F).distanceTo(camera) < handoff;
            if (near || !terrain.drawsBoxes())
            {
                if (group != null)
                    group.setActive(false);
                return;
            }

            if (group != null && !group.isValid())
            {
                group.close();
                group = null;
            }
            if (group == null)
            {
                group = terrain.createGroup("driveable", DistantBoxStyle.SOLID);
                if (group == null)
                    return;
                group.setOrigin(this);
            }
            group.setBoxes(boxes);
            group.setGlowing(thermal);
            group.setActive(true);
        }

        private static Set<EnumDriveablePart> destroyedParts(PacketDistantContacts.Contact contact)
        {
            if (contact.destroyedParts().length == 0)
                return Set.of();
            EnumDriveablePart[] parts = EnumDriveablePart.values();
            Set<EnumDriveablePart> destroyed = EnumSet.noneOf(EnumDriveablePart.class);
            for (int ordinal : contact.destroyedParts())
            {
                if (ordinal >= 0 && ordinal < parts.length)
                    destroyed.add(parts[ordinal]);
            }
            return destroyed;
        }

        private void close()
        {
            if (group != null)
                group.close();
            group = null;
        }
    }
}
